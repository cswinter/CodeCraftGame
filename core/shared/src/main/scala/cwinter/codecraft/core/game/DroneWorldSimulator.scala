package cwinter.codecraft.core.game

import cwinter.codecraft.collisions.{VisionTracker, VisionTracking}
import cwinter.codecraft.core.{PerformanceMonitor, PerformanceMonitorFactory}
import cwinter.codecraft.core.api._
import cwinter.codecraft.core.errors.Errors
import cwinter.codecraft.core.objects._
import cwinter.codecraft.core.objects.drone._
import cwinter.codecraft.core.replay.{NullReplayRecorder, ReplayRecorder, Replayer, ReplayFactory}
import cwinter.codecraft.graphics.engine.{ModelDescriptor, NullPositionDescriptor, PositionDescriptor, Simulator}
import cwinter.codecraft.graphics.models.{CircleOutlineModelBuilder, RectangleModelBuilder}
import cwinter.codecraft.physics.PhysicsEngine
import cwinter.codecraft.util.maths._
import cwinter.codecraft.util.modules.ModulePosition

import scala.async.Async._
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.scalajs.js.annotation.JSExport

/** Aggregates all datastructures required to run a game and implements the game loop.
  *
  * @param map Describes the initial state of the game world.
  * @param controllers The controllers for the initial drones.
  * @param eventGenerator Allows for triggering custom events.
  * @param replayer If set to `Some(r)`, the Simulator will replay the events recorded by `r`.
  */
class DroneWorldSimulator(
  val map: WorldMap,
  controllers: Seq[DroneControllerBase],
  eventGenerator: Int => Seq[SimulatorEvent],
  replayer: Option[Replayer] = None,
  multiplayerConfig: MultiplayerConfig = SingleplayerConfig,
  forceReplayRecorder: Option[ReplayRecorder] = None,
  val settings: Settings = Settings.default,
  private var rngSeed: Int = GlobalRNG.seed
) extends Simulator {
  outer =>
  private final val MaxDroneRadius = 60
  final val TickPeriod = 10

  private val replayRecorder: ReplayRecorder =
    if (forceReplayRecorder.nonEmpty) forceReplayRecorder.get
    else if (replayer.isEmpty && settings.recordReplays) ReplayFactory.replayRecorder
    else NullReplayRecorder

  replayer.foreach { r =>
    GlobalRNG.seed = r.seed
    rngSeed = r.seed
  }

  val monitor: PerformanceMonitor = PerformanceMonitorFactory.performanceMonitor
  private val metaControllers =
    for (c <- controllers; mc <- c.metaController) yield mc
  private val worldConfig = WorldConfig(map.size)
  private val visibleObjects = collection.mutable.Set.empty[WorldObject]
  private val dynamicObjects = collection.mutable.Set.empty[WorldObject]
  private val dronesSorted = collection.mutable.TreeSet.empty[DroneImpl](DroneOrdering)
  private var _drones = Map.empty[Int, DroneImpl]
  private val missiles = collection.mutable.Map.empty[Int, HomingMissile]

  private def drones = _drones.values

  private var deadDrones = List.empty[DroneImpl]
  private var newlySpawnedDrones = List.empty[DroneImpl]
  private var _winner = Option.empty[Player]
  private val rng = new RNG(rngSeed)
  private final val debugMode = false
  private val errors = new Errors(debug)
  private[codecraft] val debugLog =
    if (DroneWorldSimulator.detailedLogging) Some(new DroneDebugLog) else None

  /** Returns the winning player. */
  def winner = _winner

  private val visionTracker = new VisionTracker[WorldObject with VisionTracking](
    map.size.xMin.toInt, map.size.xMax.toInt,
    map.size.yMin.toInt, map.size.yMax.toInt,
    GameConstants.DroneVisionRange
  )

  private val physicsEngine = new PhysicsEngine[ConstantVelocityDynamics](
    map.size, MaxDroneRadius
  )

  private val contextForPlayer: Map[Player, DroneContext] = {
    for (player <- players)
      yield player -> DroneContext(
        player,
        worldConfig,
        TickPeriod,
        if (shouldRecordCommands(player)) Some(multiplayerConfig.commandRecorder) else None,
        debugLog,
        new IDGenerator(player.id),
        rng,
        !multiplayerConfig.isInstanceOf[MultiplayerClientConfig],
        !multiplayerConfig.isInstanceOf[SingleplayerConfig.type],
        this,
        replayRecorder,
        debug,
        errors
      )
  }.toMap

  val worldBoundaries = ModelDescriptor(NullPositionDescriptor, RectangleModelBuilder(map.size))


  replayRecorder.recordInitialWorldState(map, rngSeed)

  private[codecraft] val minerals = map.instantiateMinerals()
  implicit private val mineralRegistry = minerals.map(m => (m.id, m)).toMap
  minerals.foreach(spawnMineral)
  for {
    (Spawn(spec, position, player, resources, name), controller) <- map.initialDrones zip controllers
    drone = createDrone(spec, controller, player, position, resources)
  } spawnDrone(drone)

  for (mc <- metaControllers) {
    mc._tickPeriod = TickPeriod
    mc._worldSize = map.size
    mc.init()
  }

  override def update(): Unit = gameLoop.run()

  override protected def asyncUpdate(): Future[Unit] = gameLoop.runAsync()

  private val gameLoop: SimulationPhase =
    IfDebug ? printDebugInfo <*>
    OnTick ? runDroneControllers <*>
    (IfServer & OnTick) ? serverSyncDroneCommands <*>
    (IfClient & OnTick) ? clientSyncDroneCommands <*>
    recomputeWorldState <*>
    (IfServer & BeforeTick) ? distributeWorldState <*>
    (IfClient & BeforeTick) ? (awaitWorldState <*> applyWorldState) <*>
    processDeathEvents <*>
    BeforeTick ? updatePositionDependentState <*>
    checkWinConditions <*>
    updateTextModels

  private def printDebugInfo = Local('PrintDebugInfo) {
    if (timestep > 0 && (timestep == 1 || timestep % 1000 == 0)) {
      println(monitor.compileReport + "\n")
    }
  }

  private def runDroneControllers = Local('RunDroneControllers) {
    replayRecorder.newTimestep(timestep)
    for (r <- replayer) r.run(timestep)
    for (mc <- metaControllers) mc.onTick()
    for (drone <- deadDrones) drone.processEvents()
    for (drone <- newlySpawnedDrones) drone.controller.onSpawn()
    for (drone <- drones) drone.processEvents()
    deadDrones = List.empty
    newlySpawnedDrones = List.empty
  }

  private def recomputeWorldState = Local('RecomputeWorldState) {
    val events = ListBuffer.empty[SimulatorEvent]
    for (obj <- dynamicObjects) events.appendAll(obj.update())
    for (drone <- dronesSorted) events.appendAll(drone.update())
    physicsEngine.update()
    processSimulatorEvents(events.toList)
  }

  private def processDeathEvents = Local('ProcessDeathEvents) {
    val deathEvents =
      for (drone <- drones; d <- drone.deathEvents)
        yield d
    processSimulatorEvents(deathEvents ++ debugEvents)
  }

  private def updatePositionDependentState = Local('CompleteStateUpdate) {
    for (drone <- drones) drone.updatePositionDependentState()
    visionTracker.updateAll(timestep)
  }

  private def checkWinConditions = Local('CheckWinConditions) {
    if (winner.isEmpty) {
      for (
        wc <- map.winCondition;
        player <- players
        if playerHasWon(wc, player)
      ) _winner = Some(player)
    }
  }

  //noinspection MutatorLikeMethodIsParameterless
  private def updateTextModels = Local('UpdateTextModels) {
    errors.updateMessages()
    for (winner <- winner)
      debug.drawText(
        s"${winner.name} has won!",
        0, 0,
        ColorRGBA(winner.color, 0.6f),
        absolutePosition = true, centered = true, largeFont = true)
  }

  private def clientSyncDroneCommands =
    sendCommandsToServer <*> awaitRemoteCommands <*> executeRemoteCommands

  private def serverSyncDroneCommands =
    awaitClientCommands <*> distributeCommandsToClients <*> executeRemoteCommands


  private def sendCommandsToServer = Local('SendCommandsToServer) {
    val MultiplayerClientConfig(_, _, server) = multiplayerConfig.asInstanceOf[MultiplayerClientConfig]
    val localCommands = multiplayerConfig.commandRecorder.popAll()
    server.sendCommands(localCommands)
  }

  private var remoteCommands = Seq.empty[(Int, DroneCommand)]

  private def awaitRemoteCommands = Async('AwaitRemoteCommands) {
    val MultiplayerClientConfig(_, _, server) = multiplayerConfig.asInstanceOf[MultiplayerClientConfig]
    server.receiveCommands().map(remoteCommands = _)
  }

  private def awaitClientCommands = Async('AwaitClientCommands) {
    val AuthoritativeServerConfig(_, _, clients) = multiplayerConfig.asInstanceOf[AuthoritativeServerConfig]
    val commandFutures = for (
      client <- clients.toSeq
    ) yield client.waitForCommands()

    val futureCommandSequences = Future.sequence(commandFutures)
    async {
      val commandSequences = await { futureCommandSequences }
      for {
        commands <- commandSequences
        command <- commands
      } yield command
    }.map(remoteCommands = _)
  }

  private def distributeCommandsToClients = Local('DistributeCommandsToClients) {
    val AuthoritativeServerConfig(_, _, clients) = multiplayerConfig.asInstanceOf[AuthoritativeServerConfig]
    val allCommands = multiplayerConfig.commandRecorder.popAll() ++ remoteCommands
    for (
      client <- clients;
      commands = allCommands.filter(d => !client.players.contains(simulationContext.drone(d._1).player))
    ) client.sendCommands(commands)
  }

  private def executeRemoteCommands = Local('ExecuteRemoteCommands) {
    for (
      (id, command) <- remoteCommands;
      drone = simulationContext.drone(id)
    ) drone ! command
  }

  private def distributeWorldState = Local('DistributeWorldState) {
    val AuthoritativeServerConfig(_, _, clients) = multiplayerConfig.asInstanceOf[AuthoritativeServerConfig]
    val worldState = collectWorldState(drones)
    for (client <- clients) client.sendWorldState(worldState)
  }

  private def collectWorldState(drones: Iterable[DroneImpl]): WorldStateMessage = {
    val stateChanges = ArrayBuffer.empty[DroneStateChangeMsg]
    val missileHits = ArrayBuffer.empty[MissileHit]
    for (
      drone <- drones;
      dynamics = drone.dynamics.asInstanceOf[ComputedDroneDynamics]
    ) {
      stateChanges.appendAll(dynamics.syncMsg())
      stateChanges.appendAll(dynamics.arrivalMsg)
    }

    for (context <- contextForPlayer.values) {
      missileHits.appendAll(context.missileHits)
      context.missileHits = List.empty
    }

    WorldStateMessage(missileHits, stateChanges)
  }

  private var remoteWorldState: WorldStateMessage = null
  private def awaitWorldState = Async('AwaitWorldState) {
    val MultiplayerClientConfig(_, _, server) = multiplayerConfig.asInstanceOf[MultiplayerClientConfig]
    server.receiveWorldState().map(remoteWorldState = _)
  }

  private def applyWorldState = Local('ApplyWorldState) {
    val WorldStateMessage(missileHits, stateChanges) = remoteWorldState
    for (MissileHit(droneID, position, missileID) <- missileHits) {
      val missile = missiles(missileID)
      simulationContext.drone(droneID).missileHit(missile)
      missile.dynamics.remove()
    }
    for (state <- stateChanges) simulationContext.drone(state.droneID).applyState(state)
  }


  @JSExport
  val namedDrones = (
    for ((Spawn(_, _, player, _, Some(name)), controller) <- map.initialDrones zip controllers)
      yield (
        name,
        if (player == BluePlayer) controller
        else new EnemyDrone(controller.drone, controller.drone.player)
        )
    ).toMap

  private def spawnMineral(mineralCrystal: MineralCrystalImpl): Unit = {
    visibleObjects.add(mineralCrystal)
    visionTracker.insertPassive(mineralCrystal)
  }

  private def createDrone(
    spec: DroneSpec,
    controller: DroneControllerBase,
    player: Player,
    position: Vector2,
    resources: Int
  ): DroneImpl = new DroneImpl(spec, controller, contextForPlayer(player), position, 0, resources)

  private def shouldRecordCommands(player: Player): Boolean =
    multiplayerConfig.isMultiplayerGame && multiplayerConfig.isLocalPlayer(player)

  private def spawnDrone(drone: DroneImpl): Unit = {
    visibleObjects.add(drone)
    dronesSorted.add(drone)
    _drones += drone.id -> drone
    visionTracker.insertActive(drone)
    if (drone.dynamics.isInstanceOf[ComputedDroneDynamics]) {
      physicsEngine.addObject(drone.dynamics.asInstanceOf[ComputedDroneDynamics])
    }
    drone.initialise(physicsEngine.time)
    newlySpawnedDrones ::= drone
  }

  private def droneKilled(drone: DroneImpl): Unit = {
    visibleObjects.remove(drone)
    dronesSorted.remove(drone)
    _drones -= drone.id
    visionTracker.removeActive(drone)
    if (drone.dynamics.isInstanceOf[ComputedDroneDynamics]) {
      physicsEngine.remove(drone.dynamics.asInstanceOf[ComputedDroneDynamics])
    }

    deadDrones ::= drone
    drone.enqueueEvent(Destroyed)

    for {
      i <- 0 until ModulePosition.moduleCount(drone.sides)
      pos = drone.position + GlobalRNG.double(0, drone.radius) * GlobalRNG.vector2()
    } spawnLightflash(pos)
  }

  private def spawnMissile(missile: HomingMissile): Unit = {
    visibleObjects.add(missile)
    dynamicObjects.add(missile)
    physicsEngine.addObject(missile.dynamics)
    missiles.put(missile.id, missile)
  }

  private def spawnLightflash(position: Vector2): Unit = {
    val lightFlash = new LightFlash(position)
    visibleObjects.add(lightFlash)
    dynamicObjects.add(lightFlash)
  }

  private def debugEvents: Seq[SimulatorEvent] = eventGenerator(timestep)

  private def processSimulatorEvents(events: Iterable[SimulatorEvent]): Unit = events.foreach(processEvent)

  private def processEvent(event: SimulatorEvent): Unit = event match {
    case MineralCrystalHarvested(mineralCrystal) =>
      visibleObjects.remove(mineralCrystal)
      visionTracker.removePassive(mineralCrystal)
    case DroneConstructionStarted(drone) =>
      visibleObjects.add(drone)
    case DroneConstructionCancelled(drone) =>
      visibleObjects.remove(drone)
      drone.controller.onConstructionCancelled()
    case SpawnHomingMissile(player, position, missileID, target) =>
      // TODO: remove this check once boundary collisions are done properly
      if (map.size.contains(position)) {
        val newMissile = new HomingMissile(player, position, missileID, physicsEngine.time, target)
        spawnMissile(newMissile)
      }
    case HomingMissileFaded(missile) =>
      visibleObjects.remove(missile)
      dynamicObjects.remove(missile)
      physicsEngine.remove(missile.dynamics)
    case MissileExplodes(missile) =>
      spawnLightflash(missile.position)
    case LightFlashDestroyed(lightFlash) =>
      visibleObjects.remove(lightFlash)
      dynamicObjects.remove(lightFlash)
    case DroneKilled(drone) =>
      droneKilled(drone)
    case SpawnDrone(drone) =>
      spawnDrone(drone)
    case SpawnEnergyGlobeAnimation(energyGlobeObject) =>
      visibleObjects.add(energyGlobeObject)
      dynamicObjects.add(energyGlobeObject)
    case RemoveEnergyGlobeAnimation(energyGlobeObject) =>
      visibleObjects.remove(energyGlobeObject)
      dynamicObjects.remove(energyGlobeObject)
  }

  private def players = map.initialDrones.map(_.player)


  private[codecraft] override def computeWorldState: Iterable[ModelDescriptor[_]] = {
    val result = ListBuffer.empty[ModelDescriptor[_]]
    result.append(worldBoundaries)
    for (obj <- visibleObjects) result.appendAll(obj.descriptor)
    if (settings.showMissileRadius) appendMissileRadii(result)
    if (settings.showSightRadius) appendSightRadii(result)
    result.toList
  }

  private def appendMissileRadii(buffer: ListBuffer[ModelDescriptor[_]]): Unit = {
    for {
      d <- drones
      if d.spec.missileBatteries > 0
    } buffer.append(
      ModelDescriptor(
        PositionDescriptor(d.position.x, d.position.y, 0),
        CircleOutlineModelBuilder(GameConstants.MissileLockOnRange, ColorRGB(1, 0, 0))))
  }

  private def appendSightRadii(buffer: ListBuffer[ModelDescriptor[_]]): Unit = {
    for (d <- drones) buffer.append(
      ModelDescriptor(
        PositionDescriptor(d.position.x, d.position.y, 0),
        CircleOutlineModelBuilder(GameConstants.DroneVisionRange, ColorRGB(0, 1, 0))))
  }

  private implicit def droneRegistry: Map[Int, DroneImpl] = _drones

  private implicit def simulationContext: SimulationContext =
    SimulationContext(droneRegistry, mineralRegistry, timestep)

  def replayString: Option[String] = replayRecorder.replayString

  override def initialCameraPos: Vector2 = map.initialDrones.head.position

  private[codecraft] override def handleKeypress(keyChar: Char): Unit = {
    keyChar match {
      case '1' => settings.showSightRadius = !settings.showSightRadius
      case '2' => settings.showMissileRadius = !settings.showMissileRadius
      case _ =>
    }
  }

  private[codecraft] override def additionalInfoText: String =
    s"""${if (settings.showSightRadius) "Hide" else "Show"} sight radius: 1
       |${if (settings.showMissileRadius) "Hide" else "Show"} missile range: 2
       |${replayRecorder.replayFilepath match { case Some(path) => "Replay path: " + path case _ => "" }}
       """.stripMargin


  private trait SimulationPhase {
    def run(): Unit
    def runAsync(): Future[Unit]
    def <*>[Out2](next: SimulationPhase): SimulationPhase = sequence(next)
    final def sequence(next: SimulationPhase): SimulationPhase = SimulationPhase.sequence(this, next)
    def isFullyLocal: Boolean
  }

  private object SimulationPhase {
    def sequence(phase1: SimulationPhase, phase2: SimulationPhase): SimulationPhase = (phase1, phase2) match {
      case (NoopSimulationPhase, _) => phase2
      case (_, NoopSimulationPhase) => phase1
      case (SimulationPhaseSeq(seq1), SimulationPhaseSeq(seq2)) => SimulationPhaseSeq(seq1 ++ seq2)
      case (SimulationPhaseSeq(seq), _) => SimulationPhaseSeq(seq :+ phase2)
      case (_, SimulationPhaseSeq(seq)) => SimulationPhaseSeq(phase1 +: seq)
      case _ => SimulationPhaseSeq(Seq(phase1, phase2))
    }
  }

  private object Local {
    def apply(label: Symbol)(code: => Unit) = new SimulationPhase {
      override def run(): Unit = monitor.measure(label)(code)
      override def runAsync(): Future[Unit] = {
        run()
        Future.successful(Unit)
      }
      override def isFullyLocal = true
    }
  }

  private object Async {
    def apply(label: Symbol)(code: => Future[Unit]) = {
      def instrumentedCode = {
        monitor.beginMeasurement(label)
        code.map(_ => monitor.endMeasurement(label))
      }
      new SimulationPhase {
        override def runAsync(): Future[Unit] = instrumentedCode
        override def run(): Unit = Await.result(runAsync(), 30 seconds)
        override def isFullyLocal = false
      }
    }
  }

  private object NoopSimulationPhase extends SimulationPhase {
    override def run(): Unit = ()
    override def runAsync(): Future[Unit] = Future.successful(Unit)
    override def isFullyLocal = true
  }

  private case class SimulationPhaseSeq(subphases: Seq[SimulationPhase]) extends SimulationPhase {
    override def run(): Unit = subphases.foreach(_.run())

    override def runAsync(): Future[Unit] = runAsync(subphases)

    private def runAsync(remaining: Seq[SimulationPhase]): Future[Unit] = remaining match {
      case Seq(last) => last.runAsync()
      case Seq(head, tail@_*) =>
        if (head.isFullyLocal) {
          head.run()
          runAsync(tail)
        } else {
          head.runAsync().flatMap(_ => runAsync(tail))
        }
    }

    override def isFullyLocal = subphases.forall(_.isFullyLocal)
  }

  private trait ConditionalSimulationPhase extends SimulationPhase {
    def shouldRun: Boolean
    def phase: SimulationPhase

    override def run(): Unit = if (shouldRun) phase.run()
    override def isFullyLocal: Boolean = phase.isFullyLocal || !shouldRun
    override def runAsync(): Future[Unit] =
      if (shouldRun) phase.runAsync() else Future.successful(Unit)
  }

  private trait RunOnCondition {
    self =>
    def shouldRun: Boolean
    def ?(conditionalPhase: SimulationPhase): SimulationPhase = new ConditionalSimulationPhase {
      override def shouldRun: Boolean = self.shouldRun
      override def phase: SimulationPhase = conditionalPhase
    }
  }

  private object OnTick extends RunOnCondition {
    override def shouldRun: Boolean = timestep % TickPeriod == 0
  }

  private object BeforeTick extends RunOnCondition {
    override def shouldRun: Boolean = timestep % TickPeriod == TickPeriod - 1
  }


  private trait SimulationPhaseGuard {
    self =>

    def shouldExecute: Boolean
    def ?(phase: SimulationPhase): SimulationPhase =
      if (shouldExecute) phase
      else NoopSimulationPhase

    def &(dynamicGuard: RunOnCondition): SimulationPhaseGuard = new SimulationPhaseGuard {
      def shouldExecute = self.shouldExecute
      override def ?(phase: SimulationPhase): SimulationPhase =
        if (shouldExecute) dynamicGuard ? phase
        else NoopSimulationPhase
    }
  }

  private object IfServer extends SimulationPhaseGuard {
    override def shouldExecute: Boolean = multiplayerConfig.isInstanceOf[AuthoritativeServerConfig]
  }

  private object IfClient extends SimulationPhaseGuard {
    override def shouldExecute: Boolean = multiplayerConfig.isInstanceOf[MultiplayerClientConfig]
  }

  private object IfDebug extends SimulationPhaseGuard {
    override def shouldExecute: Boolean = debugMode
  }

  private def playerHasWon(winCondition: WinCondition, player: Player): Boolean = {
    winCondition match {
      case DestroyEnemyMotherships => !drones.exists(isLivingEnemyMothership(player))
    }
  }

  private def isLivingEnemyMothership(player: Player)(drone: DroneImpl): Boolean =
    drone.player != player && !drone.isDead && drone.spec.constructors > 0
}

private[codecraft] object DroneWorldSimulator {
  private var detailedLogging: Boolean = false
  def enableDetailedLogging(): Unit = detailedLogging = true
}


private[codecraft] sealed trait SimulatorEvent
private[codecraft] case class RemoveEnergyGlobeAnimation(energyGlobeObject: EnergyGlobeObject) extends SimulatorEvent
private[codecraft] case class DroneConstructionCancelled(drone: DroneImpl) extends SimulatorEvent
private[codecraft] case class DroneConstructionStarted(drone: DroneImpl) extends SimulatorEvent
private[codecraft] case class DroneKilled(drone: DroneImpl) extends SimulatorEvent
private[codecraft] case class HomingMissileFaded(missile: HomingMissile) extends SimulatorEvent
private[codecraft] case class LightFlashDestroyed(lightFlash: LightFlash) extends SimulatorEvent
private[codecraft] case class MineralCrystalHarvested(mineralCrystal: MineralCrystalImpl) extends SimulatorEvent
private[codecraft] case class MissileExplodes(homingMissile: HomingMissile) extends SimulatorEvent
private[codecraft] case class SpawnDrone(drone: DroneImpl) extends SimulatorEvent
private[codecraft] case class SpawnHomingMissile(player: Player, position: Vector2, missileID: Int, target: DroneImpl) extends SimulatorEvent
private[codecraft] case class SpawnEnergyGlobeAnimation(energyGlobeObject: EnergyGlobeObject) extends SimulatorEvent

