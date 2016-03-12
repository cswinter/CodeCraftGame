package cwinter.codecraft.core

import cwinter.codecraft.collisions.VisionTracker
import cwinter.codecraft.core.api._
import cwinter.codecraft.core.errors.Errors
import cwinter.codecraft.core.multiplayer.RemoteClient
import cwinter.codecraft.core.objects._
import cwinter.codecraft.core.objects.drone._
import cwinter.codecraft.core.replay._
import cwinter.codecraft.graphics.engine.Debug
import cwinter.codecraft.graphics.worldstate._
import cwinter.codecraft.physics.PhysicsEngine
import cwinter.codecraft.util.maths.{ColorRGBA, ColorRGB, Rng, Vector2}
import cwinter.codecraft.util.modules.ModulePosition

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.scalajs.js.annotation.JSExport
import scala.util.Random


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
  forceReplayRecorder: Option[ReplayRecorder] = None
) extends Simulator {
  private final val MaxDroneRadius = 60

  private var showSightRadius = false
  private var showMissileRadius = false

  private val replayRecorder: ReplayRecorder =
    if (forceReplayRecorder.nonEmpty) forceReplayRecorder.get
    else if (replayer.isEmpty) ReplayFactory.replayRecorder
    else NullReplayRecorder

  replayer.foreach { r => Rng.seed = r.seed }

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
  private val rng = new Random(Rng.seed)
  private var _winner = Option.empty[Player]

  /** Returns the winning player. */
  def winner = _winner

  private val visionTracker = new VisionTracker[WorldObject](
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
        if (shouldRecordCommands(player)) Some(multiplayerConfig.commandRecorder)
        else None,
        new IDGenerator(player.id),
        rng,
        !multiplayerConfig.isInstanceOf[MultiplayerClientConfig],
        replayRecorder
      )
  }.toMap

  Debug.drawAlways(ModelDescriptor(NullPositionDescriptor, DrawRectangle(map.size)))



  replayRecorder.recordInitialWorldState(map)

  private[codecraft] val minerals = map.instantiateMinerals()
  implicit private val mineralRegistry = minerals.map(m => (m.id, m)).toMap
  minerals.foreach(spawnMineral)
  for {
    (Spawn(spec, position, player, resources, name), controller) <- map.initialDrones zip controllers
    drone = createDrone(spec, controller, player, position, resources)
  } spawnDrone(drone)


  @JSExport
  val namedDrones = (
    for ((Spawn(_, _,player, _, Some(name)), controller) <- map.initialDrones zip controllers)
    yield (
      name,
      if (player == BluePlayer) controller
      else new EnemyDrone(controller.drone, controller.drone.player)
    )
  ).toMap

  private def spawnMineral(mineralCrystal: MineralCrystalImpl): Unit = {
    visibleObjects.add(mineralCrystal)
    visionTracker.insert(mineralCrystal)
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
    visionTracker.insert(drone, generateEvents=true)
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
    visionTracker.remove(drone)
    if (drone.dynamics.isInstanceOf[ComputedDroneDynamics]) {
      physicsEngine.remove(drone.dynamics.asInstanceOf[ComputedDroneDynamics])
    }

    deadDrones ::= drone
    drone.enqueueEvent(Destroyed)

    for {
      i <- 0 until ModulePosition.moduleCount(drone.size)
      pos = drone.position + Rng.double(0, drone.radius) * Rng.vector2()
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

  override def update(): Unit = {
    if (multiplayerConfig.isMultiplayerGame) {
      Await.result(multiplayerUpdate(), 30 seconds)
    } else {
      singleplayerUpdate()
    }
  }

  override protected def asyncUpdate(): Future[Unit] = {
    if (multiplayerConfig.isMultiplayerGame) {
      multiplayerUpdate()
    } else {
      singleplayerUpdate()
      Future.successful(Unit)
    }
  }

  private def multiplayerUpdate(): Future[Unit] = async {
    prepareDroneCommands()
    await { syncDroneCommands() }
    updateWorldState()
    await { syncWorldState() }
    completeUpdate()
  }

  private def singleplayerUpdate(): Unit = {
    prepareDroneCommands()
    updateWorldState()
    completeUpdate()
  }

  private def prepareDroneCommands(): Unit = {
    replayRecorder.newTimestep(timestep)
    showSightAndMissileRadii()
    checkWinConditions()

    // TODO: expose a simulationHasFinished property that will stop the update method from being called
    if (replayer.exists(_.finished)) return
    for (r <- replayer) r.run(timestep)

    processDroneEvents()
  }

  private def updateWorldState(): Unit = {
    val events = executeGameMechanics()
    physicsEngine.update()
    processSimulatorEvents(events ++ debugEvents)
  }

  private def completeUpdate(): Unit = {
    val deathEvents =
      for (drone <- drones; d <- drone.deathEvents)
        yield d
    processSimulatorEvents(deathEvents)
    processVisionTrackerEvents()
    Errors.updateMessages()
  }


  private def showSightAndMissileRadii(): Unit = {
    if (showMissileRadius) {
      for (
        d <- drones
        if d.spec.missileBatteries > 0
      ) Debug.draw(
          ModelDescriptor(
            PositionDescriptor(d.position.x.toFloat, d.position.y.toFloat, 0),
            DrawCircleOutline(GameConstants.MissileLockOnRange, ColorRGB(1, 0, 0))
          )
        )
    }
    if (showSightRadius) {
      for (d <- drones) {
        Debug.draw(
          ModelDescriptor(
            PositionDescriptor(d.position.x.toFloat, d.position.y.toFloat, 0),
            DrawCircleOutline(GameConstants.DroneVisionRange, ColorRGB(0, 1, 0))
          )
        )
      }
    }
  }

  private def checkWinConditions(): Unit = {
    if (_winner.isEmpty) {
      for (
        wc <- map.winCondition;
        player <- players
        if playerHasWon(wc, player)
      ) _winner = Some(player)
    }
    for (player <- _winner) showVictoryMessage(player)
  }

  private def showVictoryMessage(winner: Player): Unit = {
    Debug.drawText(s"${winner.name} has won!", 0, 0, ColorRGBA(winner.color, 0.6f), true, true)
  }

  private def processDroneEvents(): Unit = {
    for (mc <- metaControllers) mc.onTick()

    for (drone <- deadDrones) {
      drone.processEvents()
    }
    for (drone <- newlySpawnedDrones) {
      drone.controller.onSpawn()
    }

    for (drone <- drones) {
      drone.processEvents()
    }
    deadDrones = List.empty[DroneImpl]
    newlySpawnedDrones = List.empty[DroneImpl]
  }

  private def executeGameMechanics(): Seq[SimulatorEvent] = {
    for (obj <- dynamicObjects.toSeq; event <- obj.update())
      yield event
  } ++ {
    for (drone <- dronesSorted.toSeq; event <- drone.update())
      yield event
  }

  private def debugEvents: Seq[SimulatorEvent] =
    eventGenerator(timestep)

  private def processSimulatorEvents(events: Iterable[SimulatorEvent]): Unit = {
    events.foreach(processEvent)
  }

  private def processEvent(event: SimulatorEvent): Unit = event match {
    case MineralCrystalHarvested(mineralCrystal) =>
      visibleObjects.remove(mineralCrystal)
      visionTracker.remove(mineralCrystal)
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

  private def processVisionTrackerEvents(): Unit = {
    visionTracker.updateAll()
    for (drone <- drones) drone.objectsInSight = visionTracker.getVisible(drone)
    for {
      (drone: DroneImpl, events) <- visionTracker.collectEvents()
      event <- events
    } event match {
      case visionTracker.EnteredSightRadius(mineral: MineralCrystalImpl) =>
        drone.enqueueEvent(MineralEntersSightRadius(mineral))
      case visionTracker.LeftSightRadius(obj) => // don't care (for now)
      case visionTracker.EnteredSightRadius(other: DroneImpl) =>
        drone.enqueueEvent(DroneEntersSightRadius(other))
      case e => throw new Exception(s"AHHHH, AN UFO!!! RUN FOR YOUR LIFE!!! $e")
    }
  }

  private def syncDroneCommands(): Future[Unit] = async {
    // [CLIENTS + SERVER] COLLECT COMMANDS FROM LOCAL DRONES
    // [CLIENTS] SEND COMMANDS FROM LOCAL DRONES
    // [CLIENTS} RECEIVE COMMANDS FROM SERVER
    // [SERVER] RECEIVE ALL COMMANDS
    // [SERVER] DISTRIBUTE COMMANDS TO ALL CLIENTS
    // [CLIENTS + SERVER] EXECUTE COMMANDS FROM REMOTE DRONES

    val localCommands = multiplayerConfig.commandRecorder.popAll()

    val remoteCommands: Seq[(Int, DroneCommand)] = multiplayerConfig match {
      case AuthoritativeServerConfig(local, remote, clients) =>
        await { syncWithClients(clients, localCommands) }
      case MultiplayerClientConfig(local, remote, server) =>
        server.sendCommands(localCommands)
        await { server.receiveCommands()(simulationContext) }
      case SingleplayerConfig =>
        throw new Exception("Matched SingleplayerConfig in syncDroneCommands().")
    }

    executeCommands(remoteCommands)
  }

  private def syncWithClients(
    clients: Set[RemoteClient],
    localCommands: Seq[(Int, DroneCommand)]
  ): Future[Seq[(Int, DroneCommand)]] = async {
    val remoteCommands = await { receiveCommandsFromClients(clients) }
    distributeCommandsToClients(clients, remoteCommands ++ localCommands)
    remoteCommands
  }

  private def receiveCommandsFromClients(clients: Set[RemoteClient]): Future[Seq[(Int, DroneCommand)]] = {
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
    }
  }

  private def distributeCommandsToClients(
    clients: Set[RemoteClient],
    allCommands: Seq[(Int, DroneCommand)]
  ): Unit = {
    for (
      client <- clients;
      commands = allCommands.filter(d => !client.players.contains(droneRegistry(d._1).player))
    ) client.sendCommands(commands)
  }

  private def executeCommands(commands: Seq[(Int, DroneCommand)]): Unit = {
    for (
      (id, command) <- commands;
      drone = droneRegistry(id)
    ) drone ! command
  }

  private def syncWorldState(): Future[Unit] = async {
    multiplayerConfig match {
      case AuthoritativeServerConfig(_, _, clients) =>
        val syncMessages = collectWorldState()
        for (client <- clients)
          client.sendWorldState(syncMessages)
      case MultiplayerClientConfig(_, _, server) =>
        val worldState = await { server.receiveWorldState() }
        for (state <- worldState) state match {
          case MissileHit(droneID, position, missileID) =>
            val missile = missiles(missileID)
            simulationContext.drone(droneID).missileHit(missile)
            missile.dynamics.remove()
          case d: DroneDynamicsState =>
            simulationContext.drone(d.droneId).applyState(d)
        }
      case SingleplayerConfig =>
        throw new Exception("Matched SingleplayerConfig in syncWorldState().")
    }
  }

  private def collectWorldState(): Iterable[DroneStateMessage] = {
    val positions = for (drone <- drones)
      yield drone.dynamics.asInstanceOf[ComputedDroneDynamics].state
    val missileHits = for (
      drone <- drones;
      missileHit <- drone.popMissileHits()
    ) yield missileHit

    positions ++ missileHits
  }

  private def players = map.initialDrones.map(_.player)
  
  private def playerHasWon(winCondition: WinCondition, player: Player): Boolean =
    winCondition match {
      case DestroyEnemyMotherships =>
        !drones.exists(isLivingEnemyMothership(_, player))
    }

  private def isLivingEnemyMothership(drone: DroneImpl, player: Player): Boolean =
    drone.player != player && !drone.isDead && drone.spec.constructors > 0


  private[codecraft] override def computeWorldState: Iterable[ModelDescriptor[_]] = {
    visibleObjects.flatMap(_.descriptor)
  }

  private implicit def droneRegistry: Map[Int, DroneImpl] = _drones

  private implicit def simulationContext: SimulationContext =
    SimulationContext(droneRegistry, mineralRegistry, timestep)


  def replayString: Option[String] = replayRecorder.replayString


  override def initialCameraPos: Vector2 = map.initialDrones.head.position


  private[codecraft] override def handleKeypress(keyChar: Char): Unit = {
    keyChar match {
      case '1' => showSightRadius = !showSightRadius
      case '2' => showMissileRadius = !showMissileRadius
      case _ =>
    }
  }


  private[codecraft] override def additionalInfoText: String =
    s"""${if (showSightRadius) "Hide" else "Show"} sight radius: 1
       |${if (showMissileRadius) "Hide" else "Show"} missile range: 2
       |${replayRecorder.replayFilepath match{ case Some(path) => "Replay path: " + path case _ => ""}}
     """.stripMargin
}



private[codecraft] sealed trait SimulatorEvent
private[codecraft] case class MineralCrystalHarvested(mineralCrystal: MineralCrystalImpl) extends SimulatorEvent
private[codecraft] case class DroneConstructionStarted(drone: DroneImpl) extends SimulatorEvent
private[codecraft] case class SpawnDrone(drone: DroneImpl) extends SimulatorEvent
private[codecraft] case class SpawnHomingMissile(player: Player, position: Vector2, missileID: Int, target: DroneImpl) extends SimulatorEvent
private[codecraft] case class HomingMissileFaded(missile: HomingMissile) extends SimulatorEvent
private[codecraft] case class MissileExplodes(homingMissile: HomingMissile) extends SimulatorEvent
private[codecraft] case class LightFlashDestroyed(lightFlash: LightFlash) extends SimulatorEvent
private[codecraft] case class DroneKilled(drone: DroneImpl) extends SimulatorEvent
private[codecraft] case class DroneConstructionCancelled(drone: DroneImpl) extends SimulatorEvent
private[codecraft] case class SpawnEnergyGlobeAnimation(energyGlobeObject: EnergyGlobeObject) extends SimulatorEvent
private[codecraft] case class RemoveEnergyGlobeAnimation(energyGlobeObject: EnergyGlobeObject) extends SimulatorEvent

