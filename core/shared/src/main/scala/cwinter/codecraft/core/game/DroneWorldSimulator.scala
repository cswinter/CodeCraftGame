package cwinter.codecraft.core.game

import java.util.concurrent.TimeoutException

import cwinter.codecraft.collisions.{VisionTracker, VisionTracking}
import cwinter.codecraft.core.{PerformanceMonitor, PerformanceMonitorFactory}
import cwinter.codecraft.core.api._
import cwinter.codecraft.core.errors.Errors
import cwinter.codecraft.core.objects._
import cwinter.codecraft.core.objects.drone._
import cwinter.codecraft.core.replay.{NullReplayRecorder, ReplayRecorder, Replayer, ReplayFactory}
import cwinter.codecraft.graphics.engine._
import cwinter.codecraft.graphics.models.{CircleOutlineModelBuilder, RectangleModelBuilder}
import cwinter.codecraft.physics.PhysicsEngine
import cwinter.codecraft.util.maths._
import cwinter.codecraft.util.modules.ModulePosition

import scala.async.Async._
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.{implicitConversions, postfixOps}
import scala.scalajs.js.annotation.JSExport

/** Aggregates all datastructures required to run a game and implements the game loop.
  *
  * @param config Describes the initial state of the game world.
  * @param eventGenerator Allows for triggering custom events.
  * @param multiplayerConfig Additional configuration for multiplayer games.
  * @param forceReplayRecorder If set to `Some(r)`, the Simulator will replay the events recorded by `r`.
  * @param settings Additional settings that don't affect gameplay.
  */
class DroneWorldSimulator(
  val config: GameConfig,
  eventGenerator: Int => Seq[SimulatorEvent] = t => Seq.empty,
  multiplayerConfig: MultiplayerConfig = SingleplayerConfig,
  forceReplayRecorder: Option[ReplayRecorder] = None,
  val settings: Settings = Settings.default,
  val specialRules: SpecialRules = SpecialRules.default
) extends Simulator { outer =>
  private final val MaxDroneRadius = 60

  private val replayRecorder: ReplayRecorder =
    if (forceReplayRecorder.nonEmpty) forceReplayRecorder.get
    else if (settings.recordReplays) ReplayFactory.replayRecorder
    else NullReplayRecorder

  val monitor: PerformanceMonitor = PerformanceMonitorFactory.performanceMonitor
  private val metaControllers = for ((_, c) <- config.drones; mc <- c.metaController) yield mc
  private val visibleObjects = collection.mutable.Set.empty[WorldObject]
  private val dynamicObjects = collection.mutable.Set.empty[WorldObject]
  private val dronesSorted = collection.mutable.TreeSet.empty[DroneImpl](DroneOrdering)
  private var _drones = Map.empty[Int, DroneImpl]
  private val missiles = collection.mutable.Map.empty[Int, HomingMissile]

  private def drones = _drones.values

  def dronesFor(player: Player): Seq[Drone] =
    (for {
      drone <- _drones.values
      if drone.player == player
    } yield drone.wrapperFor(player)).toSeq

  private var deadDrones = List.empty[DroneImpl]
  private var unsyncedDroneSpawns = List.empty[DroneImpl]
  private var newlySpawnedDrones = List.empty[DroneImpl]
  private var _winner = Option.empty[Player]
  private val rng = new RNG(config.rngSeed)
  private final val debugMode = false
  private val errors = new Errors(debug)
  private[codecraft] val debugLog =
    if (DroneWorldSimulator.detailedLogging) Some(new DroneDebugLog) else None

  private[this] var _currentPhase: Symbol = 'NotStarted

  /** Returns the winning player. */
  def winner: Option[Player] = _winner

  private val visionTracker = new VisionTracker[WorldObject with VisionTracking](
    config.worldSize.xMin.toInt,
    config.worldSize.xMax.toInt,
    config.worldSize.yMin.toInt,
    config.worldSize.yMax.toInt,
    GameConstants.DroneVisionRange
  )

  private val physicsEngine = new PhysicsEngine[ConstantVelocityDynamics](
    config.worldSize,
    MaxDroneRadius
  )

  private val contextForPlayer: Map[Player, DroneContext] = {
    for (player <- players)
      yield
        player -> DroneContext(
          player,
          config.worldSize,
          config.tickPeriod,
          if (shouldRecordCommands(player)) Some(multiplayerConfig.commandRecorder) else None,
          debugLog,
          new IDGenerator(player.id),
          rng,
          !multiplayerConfig.isInstanceOf[MultiplayerClientConfig],
          !multiplayerConfig.isInstanceOf[SingleplayerConfig.type],
          this,
          replayRecorder,
          debug,
          errors,
          specialRules
        )
  }.toMap

  val worldBoundaries = ModelDescriptor(NullPositionDescriptor, RectangleModelBuilder(config.worldSize))

  replayRecorder.recordInitialWorldState(config)

  private[codecraft] val minerals = config.instantiateMinerals()
  implicit private val mineralRegistry = minerals.map(m => (m.id, m)).toMap
  minerals.foreach(spawnMineral)
  for {
    (Spawn(spec, position, player, resources, name), controller) <- config.drones
    drone = createDrone(spec, controller, player, position, resources)
  } spawnDrone(drone)

  for (mc <- metaControllers) {
    mc._tickPeriod = config.tickPeriod
    mc._worldSize = config.worldSize
    mc._simulationContext = simulationContext
    mc.init()
  }

  override def update(): Unit = gameLoop.run()

  override protected def asyncUpdate()(implicit ec: ExecutionContext): Future[Unit] = gameLoop.runAsync()

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
      updateTextModels <*>
      IfServer ? serverCallback

  private def printDebugInfo = Local('PrintDebugInfo) {
    if (timestep > 0 && (timestep == 1 || timestep % 1000 == 0)) {
      println(monitor.compileReport + "\n")
    }
  }

  private def runDroneControllers = Local('RunDroneControllers) {
    replayRecorder.newTimestep(timestep)
    for (mc <- metaControllers) mc.onTick(simulationContext)
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
      for (wc <- config.winConditions.reverse;
           player <- players
           if playerHasWon(wc, player)) {
        _winner = Some(player)
        gameStatus = Stopped(s"$player won")
        for (c <- metaControllers) c.gameOver(player)
      }
    }
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

  private def awaitRemoteCommands = Async('AwaitRemoteCommands) { implicit ec =>
    val MultiplayerClientConfig(_, _, server) = multiplayerConfig.asInstanceOf[MultiplayerClientConfig]
    server.receiveCommands().map {
      case Left(commands) => remoteCommands = commands
      case Right(gameClosedReason) => gameStatus = Stopped(gameClosedReason.toString)
    }
  }

  private def awaitClientCommands = Async('AwaitClientCommands) { implicit ec =>
    val clients = multiplayerConfig.asInstanceOf[AuthoritativeServerConfig].clients
    val commandFutures = for (client <- clients.toSeq) yield client.waitForCommands()

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
    val clients = multiplayerConfig.asInstanceOf[AuthoritativeServerConfig].clients
    val allCommands = multiplayerConfig.commandRecorder.popAll() ++ remoteCommands
    for {
      client <- clients
      commands = allCommands.filter(d => !client.players.contains(simulationContext.drone(d._1).player))
    } client.sendCommands(commands)
  }

  private def executeRemoteCommands = Local('ExecuteRemoteCommands) {
    for ((id, command) <- remoteCommands) {
      if (simulationContext.droneRegistry.contains(id)) {
        simulationContext.drone(id) ! command
      } else {
        println(s"WARNING: desync, drone with id $id not found.")
      }
    }
  }

  private def distributeWorldState = Local('DistributeWorldState) {
    val clients = multiplayerConfig.asInstanceOf[AuthoritativeServerConfig].clients
    val worldState = collectWorldState(drones)
    for (client <- clients) client.sendWorldState(worldState)
  }

  private def collectWorldState(drones: Iterable[DroneImpl]): WorldStateMessage = {
    val stateChanges = ArrayBuffer.empty[DroneMovementMsg]
    val missileHits = ArrayBuffer.empty[MissileHit]
    val mineralHarvests = ArrayBuffer.empty[MineralHarvest]
    for (drone <- drones;
         dynamics = drone.dynamics.asInstanceOf[ComputedDroneDynamics]) {
      stateChanges.appendAll(dynamics.syncMsg())
      stateChanges.appendAll(dynamics.arrivalMsg)
    }

    for (context <- contextForPlayer.values) {
      missileHits.appendAll(context.missileHits)
      mineralHarvests.appendAll(context.mineralHarvests)
      context.missileHits = List.empty
      context.mineralHarvests = List.empty
    }

    val droneSpawns = unsyncedDroneSpawns.map(d => DroneSpawned(d.id))
    unsyncedDroneSpawns = List.empty

    WorldStateMessage(missileHits, stateChanges, mineralHarvests, droneSpawns)
  }

  private var remoteWorldState: WorldStateMessage = _
  private def awaitWorldState = Async('AwaitWorldState) { implicit ec =>
    val MultiplayerClientConfig(_, _, server) = multiplayerConfig.asInstanceOf[MultiplayerClientConfig]
    server.receiveWorldState().map {
      case Left(worldState) => remoteWorldState = worldState
      case Right(gameClosedReason) => gameStatus = Stopped(gameClosedReason.toString)
    }
  }

  private def applyWorldState = Local('ApplyWorldState) {
    val WorldStateMessage(missileHits, stateChanges, mineralHarvests, droneSpawns) = remoteWorldState

    for {
      DroneSpawned(droneID) <- droneSpawns
      drone <- unsyncedDroneSpawns.find(_.id == droneID)
    } spawnDrone(drone)

    for {
      MineralHarvest(droneID, mineralID) <- mineralHarvests
      drone <- simulationContext.maybeDrone(droneID)
      mineral = simulationContext.mineral(mineralID)
      event <- drone.applyHarvest(mineral)
    } processEvent(event)

    for {
      MissileHit(droneID, position, missileID, shieldDamage, hullDamage) <- missileHits
      drone <- simulationContext.maybeDrone(droneID)
    } {
      val missile = missiles(missileID)
      drone.missileHit(missile.position, shieldDamage, hullDamage)
      missile.dynamics.remove()
    }

    for {
      state <- stateChanges
      drone <- simulationContext.maybeDrone(state.droneID)
    } drone.applyState(state)

    if (multiplayerConfig.isInstanceOf[MultiplayerClientConfig] && config.tickPeriod > 1)
      correctSpeculativePositions()
  }

  private def correctSpeculativePositions(): Unit = {
    for (drone <- drones
         if !drone.isDead) drone.dynamics match {
      case speculating: SpeculatingDroneDynamics =>
        if (speculating.syncSpeculator()) {
          physicsEngine.remove(speculating.speculative)
          physicsEngine.addObject(speculating.speculative)
        }
      case _ =>
    }
  }

  //noinspection MutatorLikeMethodIsParameterless
  private def updateTextModels = Local('UpdateTextModels) {
    errors.updateMessages()
    for (winner <- winner)
      debug.drawText(s"${winner.name} has won!",
                     0,
                     0,
                     ColorRGBA(winner.color, 0.6f),
                     absolutePosition = true,
                     centered = true,
                     largeFont = true)
  }

  private def serverCallback = Local('ServerCallback) {
    multiplayerConfig.asInstanceOf[AuthoritativeServerConfig].updateCompleted(this)
  }

  @JSExport
  val namedDrones = (
    for ((Spawn(_, _, player, _, Some(name)), controller) <- config.drones)
      yield
        (
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
    drone.dynamics match {
      case local: ComputedDroneDynamics => physicsEngine.addObject(local)
      case speculating: SpeculatingDroneDynamics => physicsEngine.addObject(speculating.speculative)
    }
    drone.initialise(physicsEngine.time)
    newlySpawnedDrones ::= drone
  }

  private def droneKilled(drone: DroneImpl): Unit = {
    visibleObjects.remove(drone)
    dronesSorted.remove(drone)
    _drones -= drone.id
    visionTracker.removeActive(drone)
    drone.dynamics match {
      case local: ComputedDroneDynamics => physicsEngine.remove(local)
      case speculating: SpeculatingDroneDynamics => physicsEngine.remove(speculating.speculative)
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
      if (config.worldSize.contains(position)) {
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
      multiplayerConfig match {
        case _: MultiplayerClientConfig =>
          unsyncedDroneSpawns ::= drone
        case _: AuthoritativeServerConfig =>
          unsyncedDroneSpawns ::= drone
          spawnDrone(drone)
        case _ => spawnDrone(drone)
      }
    case SpawnEnergyGlobeAnimation(energyGlobeObject) =>
      visibleObjects.add(energyGlobeObject)
      dynamicObjects.add(energyGlobeObject)
    case RemoveEnergyGlobeAnimation(energyGlobeObject) =>
      visibleObjects.remove(energyGlobeObject)
      dynamicObjects.remove(energyGlobeObject)
  }

  private def players = config.drones.map(_._1.player)

  private[codecraft] override def computeWorldState: Seq[ModelDescriptor[_]] = {
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
      ModelDescriptor(PositionDescriptor(d.position.x, d.position.y, 0),
                      CircleOutlineModelBuilder(GameConstants.MissileLockOnRange, ColorRGB(1, 0, 0))))
  }

  private def appendSightRadii(buffer: ListBuffer[ModelDescriptor[_]]): Unit = {
    for (d <- drones)
      buffer.append(
        ModelDescriptor(PositionDescriptor(d.position.x, d.position.y, 0),
                        CircleOutlineModelBuilder(GameConstants.DroneVisionRange, ColorRGB(0, 1, 0))))
  }

  private implicit def droneRegistry: Map[Int, DroneImpl] = _drones

  private implicit def simulationContext: SimulationContext =
    SimulationContext(droneRegistry, mineralRegistry, timestep)

  def replayString: Option[String] = replayRecorder.replayString

  override def initialCameraPos: Vector2 =
    config.drones
      .map(_._1)
      .find(d => multiplayerConfig.isLocalPlayer(d.player))
      .map(_.position)
      .getOrElse(Vector2.Null)

  private[codecraft] override def handleKeypress(keyChar: Char): Unit = {
    keyChar match {
      case '1' => settings.showSightRadius = !settings.showSightRadius
      case '2' => settings.showMissileRadius = !settings.showMissileRadius
      case _ =>
    }
  }

  private[codecraft] override def additionalInfoText: String = {
    s"""${if (settings.showSightRadius) "Hide" else "Show"} sight radius: 1
       |${if (settings.showMissileRadius) "Hide" else "Show"} missile range: 2
       |${replayRecorder.replayFilepath match {
         case Some(path) => "Replay path: " + path
         case _ => ""
       }}
       |""".stripMargin
  }

  protected override def textModels: Iterable[TextModel] = {
    val extraText = gameStatus match {
      case Crashed(exception) => Some(s"Game has crashed: ${exception.getMessage}")
      case Stopped(msg) => Some(s"Game has been stopped: $msg")
      case _ =>
        multiplayerConfig match {
          case MultiplayerClientConfig(_, _, connection) =>
            val elapsed = connection.msSinceLastResponse
            if (elapsed >= 1000)
              Some(
                s"Connection issue. Seconds since last reply from server: ${connection.msSinceLastResponse / 1000}s")
            else None
          case _ => None
        }
    }

    extraText match {
      case None => super.textModels
      case Some(msg) =>
        super.textModels ++
          List(TextModel(msg, 0f, 0.9f, ColorRGBA(0.5f, 1f, 0, 1), absolutePos = true, centered = true))
    }
  }

  private trait SimulationPhase {
    def run(): Unit
    def runAsync()(implicit ec: ExecutionContext): Future[Unit]
    def <*>[Out2](next: SimulationPhase): SimulationPhase = sequence(next)
    final def sequence(next: SimulationPhase): SimulationPhase = SimulationPhase.sequence(this, next)
    def isFullyLocal: Boolean
  }

  private object SimulationPhase {
    def sequence(phase1: SimulationPhase, phase2: SimulationPhase): SimulationPhase =
      (phase1, phase2) match {
        case (NoopSimulationPhase, _) => phase2
        case (_, NoopSimulationPhase) => phase1
        case (SimulationPhaseSeq(seq1), SimulationPhaseSeq(seq2)) => SimulationPhaseSeq(seq1 ++ seq2)
        case (SimulationPhaseSeq(seq), _) => SimulationPhaseSeq(seq :+ phase2)
        case (_, SimulationPhaseSeq(seq)) => SimulationPhaseSeq(phase1 +: seq)
        case _ => SimulationPhaseSeq(Seq(phase1, phase2))
      }

    sealed trait Result
    case object Success extends Result
    case object Abort extends Result
    case class Failure(exception: Throwable) extends Result
    implicit def unitIsSuccess(unit: Unit): Result = Success
  }

  private object Local {
    def apply(label: Symbol)(code: => Unit) = new SimulationPhase {
      override def run(): Unit = {
        _currentPhase = label
        monitor.measure(label)(code)
      }
      override def runAsync()(implicit ec: ExecutionContext): Future[Unit] = {
        run()
        Future.successful(Unit)
      }
      override def isFullyLocal = true
    }
  }

  private object Async {
    def apply(label: Symbol)(code: ExecutionContext => Future[Unit]) = {
      def instrumentedCode(implicit ec: ExecutionContext) = {
        _currentPhase = label
        monitor.beginMeasurement(label)
        code(ec).map(_ => monitor.endMeasurement(label))
      }
      new SimulationPhase {
        override def runAsync()(implicit ec: ExecutionContext): Future[Unit] = instrumentedCode
        override def run(): Unit = {
          try {
            CrossPlatformAwait.result(runAsync()(scala.concurrent.ExecutionContext.Implicits.global),
                                      multiplayerConfig.timeoutSecs)
          } catch {
            case _: TimeoutException =>
              gameStatus = Stopped("Connection timed out.")
              multiplayerConfig match {
                case a: AuthoritativeServerConfig => a.onTimeout(outer)
                case _ =>
              }
          }
        }

        override def isFullyLocal = false
      }
    }
  }

  private object NoopSimulationPhase extends SimulationPhase {
    override def run(): Unit = ()
    override def runAsync()(implicit ec: ExecutionContext): Future[Unit] = Future.successful(Unit)
    override def isFullyLocal = true
  }

  private case class SimulationPhaseSeq(subphases: Seq[SimulationPhase]) extends SimulationPhase {
    override def run(): Unit = subphases.foreach { phase =>
      phase.run()
    }

    override def runAsync()(implicit ec: ExecutionContext): Future[Unit] = runAsync(subphases)

    private def runAsync(remaining: Seq[SimulationPhase])(implicit ec: ExecutionContext): Future[Unit] =
      remaining match {
        case Seq(last) =>
          last.runAsync()
        case Seq(head, tail @ _ *) =>
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
    override def runAsync()(implicit ec: ExecutionContext): Future[Unit] =
      if (shouldRun) phase.runAsync() else Future.successful(Unit)
  }

  private trait RunOnCondition { self =>
    def shouldRun: Boolean
    def ?(conditionalPhase: SimulationPhase): SimulationPhase = new ConditionalSimulationPhase {
      override def shouldRun: Boolean = self.shouldRun
      override def phase: SimulationPhase = conditionalPhase
    }
  }

  private object OnTick extends RunOnCondition {
    override def shouldRun: Boolean = timestep % config.tickPeriod == 0
  }

  private object BeforeTick extends RunOnCondition {
    override def shouldRun: Boolean = (timestep + 1) % config.tickPeriod == 0
  }

  private trait SimulationPhaseGuard { self =>

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
    if (player.isObserver) return false
    winCondition match {
      case DestroyEnemyMotherships => !drones.exists(isLivingEnemyMothership(player))
      case LargestFleet(timeout: Int) =>
        if (timestep >= timeout) {
          if (drones.isEmpty) return true
          val winner = drones
            .groupBy(_.player)
            .maxBy {
              case (_, drones) =>
                drones.map(_.spec.resourceCost).sum
            }
            ._1
          winner == player
        } else false
      case DestroyAllEnemies => drones.forall(_.player == player)
      case DroneCount(c) => drones.count(_.player == player) >= c
    }
  }

  private def isLivingEnemyMothership(player: Player)(drone: DroneImpl): Boolean =
    drone.player != player && !drone.isDead && drone.spec.constructors > 0

  private[this] var _gameStatus: Status = Running
  protected def gameStatus_=(value: Status) = {
    value match {
      case Stopped(msg) => println(s"Game has ended: $msg")
      case _ =>
    }
    _gameStatus = value
  }
  override protected[codecraft] def gameStatus = _gameStatus

  override def togglePause(): Unit = multiplayerConfig match {
    case SingleplayerConfig => super.togglePause()
    case _ =>
  }

  def currentPhase = _currentPhase

  def tickPeriod = config.tickPeriod

  private[codecraft] override def frameQueueThreshold: Int = if (precomputeFrames) tickPeriod + 1 else 2
  private[codecraft] override def maxFrameQueueSize: Int = if (precomputeFrames) 2 * tickPeriod else 2
  private[codecraft] override def framelimitPeriod: Int = if (precomputeFrames) tickPeriod else 1
  private[codecraft] def precomputeFrames: Boolean =
    multiplayerConfig.isInstanceOf[MultiplayerClientConfig] && settings.allowFramePrecomputation
}

private[codecraft] object DroneWorldSimulator {
  private var detailedLogging: Boolean = false
  def enableDetailedLogging(): Unit = detailedLogging = true
}

private[codecraft] sealed trait SimulatorEvent
private[codecraft] case class RemoveEnergyGlobeAnimation(energyGlobeObject: EnergyGlobeObject)
    extends SimulatorEvent
private[codecraft] case class DroneConstructionCancelled(drone: DroneImpl) extends SimulatorEvent
private[codecraft] case class DroneConstructionStarted(drone: DroneImpl) extends SimulatorEvent
private[codecraft] case class DroneKilled(drone: DroneImpl) extends SimulatorEvent
private[codecraft] case class HomingMissileFaded(missile: HomingMissile) extends SimulatorEvent
private[codecraft] case class LightFlashDestroyed(lightFlash: LightFlash) extends SimulatorEvent
private[codecraft] case class MineralCrystalHarvested(mineralCrystal: MineralCrystalImpl)
    extends SimulatorEvent
private[codecraft] case class MissileExplodes(homingMissile: HomingMissile) extends SimulatorEvent
private[codecraft] case class SpawnDrone(drone: DroneImpl) extends SimulatorEvent
private[codecraft] case class SpawnHomingMissile(player: Player,
                                                 position: Vector2,
                                                 missileID: Int,
                                                 target: DroneImpl)
    extends SimulatorEvent
private[codecraft] case class SpawnEnergyGlobeAnimation(energyGlobeObject: EnergyGlobeObject)
    extends SimulatorEvent
