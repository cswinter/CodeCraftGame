package cwinter.codecraft.core

import cwinter.codecraft.collisions.VisionTracker
import cwinter.codecraft.core.api.{DroneControllerBase, BluePlayer, Player, DroneSpec}
import cwinter.codecraft.core.errors.Errors
import cwinter.codecraft.core.objects._
import cwinter.codecraft.core.objects.drone._
import cwinter.codecraft.core.replay._
import cwinter.codecraft.graphics.engine.Debug
import cwinter.codecraft.graphics.worldstate._
import cwinter.codecraft.physics.PhysicsEngine
import cwinter.codecraft.util.maths.{ColorRGB, Rng, Vector2}
import cwinter.codecraft.util.modules.ModulePosition

import scala.scalajs.js.annotation.JSExport


/**
 * Aggregates all datastructures required to run a game and implements the game loop.
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
  multiplayerConfig: MultiplayerConfig = SingleplayerConfig
) extends Simulator {
  private final val MaxDroneRadius = 60

  private var showSightRadius = false
  private var showMissileRadius = false

  private var _syncMessages: Iterable[DroneDynamicsState] = Seq.empty

  private val replayRecorder: ReplayRecorder =
    if (replayer.isEmpty) ReplayFactory.replayRecorder
    else NullReplayRecorder

  private val worldConfig = WorldConfig(map.size)
  private val visibleObjects = collection.mutable.Set.empty[WorldObject]
  private val dynamicObjects = collection.mutable.Set.empty[WorldObject]
  private var _drones = Map.empty[Int, DroneImpl]
  private def drones = _drones.values
  private var deadDrones = List.empty[DroneImpl]


  private val visionTracker = new VisionTracker[WorldObject](
    map.size.xMin.toInt, map.size.xMax.toInt,
    map.size.yMin.toInt, map.size.yMax.toInt,
    DroneSpec.SightRadius
  )

  private val physicsEngine = new PhysicsEngine[ConstantVelocityDynamics](
    map.size, MaxDroneRadius
  )

  Debug.drawAlways(DrawRectangle(0, map.size))


  replayer.foreach { r => Rng.seed = r.seed }

  replayRecorder.recordInitialWorldState(map)

  map.minerals.foreach(spawnMineral)
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
  ): DroneImpl = {
    val commandRecorder =
      if (shouldRecordCommands(player))
        Some(multiplayerConfig.commandRecorder)
      else None

    new DroneImpl(
      spec, controller, player, position, 0, worldConfig,
      commandRecorder, replayRecorder, resources
    )
  }

  def shouldRecordCommands(player: Player): Boolean =
    multiplayerConfig.isMultiplayerGame && multiplayerConfig.isLocalPlayer(player)

  private def spawnDrone(drone: DroneImpl): Unit = {
    visibleObjects.add(drone)
    dynamicObjects.add(drone)
    _drones += drone.id -> drone
    visionTracker.insert(drone, generateEvents=true)
    if (drone.dynamics.isInstanceOf[ComputedDroneDynamics]) {
      physicsEngine.addObject(drone.dynamics.asInstanceOf[ComputedDroneDynamics])
    }
    drone.initialise(physicsEngine.time)
  }

  private def droneKilled(drone: DroneImpl): Unit = {
    visibleObjects.remove(drone)
    dynamicObjects.remove(drone)
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
  }

  private def spawnLightflash(position: Vector2): Unit = {
    val lightFlash = new LightFlash(position)
    visibleObjects.add(lightFlash)
    dynamicObjects.add(lightFlash)
  }

  override def update(): Unit = {
    replayRecorder.newTimestep(timestep)

    showSightAndMissileRadii()

    checkWinConditions()

    // TODO: expose a simulationHasFinished property that will stop the update method from being called
    if (replayer.exists(_.finished)) return

    for (r <- replayer) {
      implicit val mineralRegistry = map.minerals.map(m => (m.id, m)).toMap
      r.run(timestep)
    }

    processDroneEvents()


    if (multiplayerConfig.isMultiplayerGame) {
      syncDroneCommands()
    }

    val events = executeGameMechanics()

    processSimulatorEvents(events ++ debugEvents)

    physicsEngine.update()

    if (multiplayerConfig.isInstanceOf[AuthoritativeServerConfig]) {
      collectWorldState()
      syncWorldState()
    }

    processVisionTrackerEvents()

    Errors.updateMessages()
  }

  private def showSightAndMissileRadii(): Unit = {
    if (showMissileRadius) {
      for (
        d <- drones
        if d.spec.missileBatteries > 0
      ) Debug.draw(DrawCircleOutline(d.position.x.toFloat, d.position.y.toFloat, DroneConstants.MissileLockOnRadius, ColorRGB(1, 0, 0)))
    }
    if (showSightRadius) {
      for (d <- drones) {
        Debug.draw(DrawCircleOutline(d.position.x.toFloat, d.position.y.toFloat, DroneSpec.SightRadius, ColorRGB(0, 1, 0)))
      }
    }
  }

  private def checkWinConditions(): Unit = {
    if (timestep % 30 == 0) {
      for (
        wc <- map.winCondition;
        player <- players
        if playerHasWon(wc, player)
      ) showVictoryMessage(player)
    }
  }

  private def processDroneEvents(): Unit = {
    for (drone <- deadDrones) {
      drone.processEvents()
    }

    for (drone <- drones) {
      drone.processEvents()
    }
    deadDrones = List.empty[DroneImpl]
  }

  private def executeGameMechanics(): Seq[SimulatorEvent] = {
    for (obj <- dynamicObjects.toSeq; event <- obj.update())
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
    case MineralCrystalDestroyed(mineralCrystal) =>
      visibleObjects.remove(mineralCrystal)
    case MineralCrystalActivated(mineralCrystal) =>
      visibleObjects.add(mineralCrystal)
    case MineralCrystalInactivated(mineralCrystal) =>
      visibleObjects.remove(mineralCrystal)
    case DroneConstructionStarted(drone) =>
      visibleObjects.add(drone)
    case DroneConstructionCancelled(drone) =>
      visibleObjects.remove(drone)
    case SpawnHomingMissile(player, position, target) =>
      // TODO: remove this check once boundary collisions are done properly
      if (map.size.contains(position)) {
        val newMissile = new HomingMissile(player, position, physicsEngine.time, target)
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

  private def syncDroneCommands(): Unit = {
    // [CLIENTS + SERVER] COLLECT COMMANDS FROM LOCAL DRONES
    // [CLIENTS] SEND COMMANDS FROM LOCAL DRONES
    // [CLIENTS} RECEIVE COMMANDS FROM SERVER
    // [SERVER] RECEIVE ALL COMMANDS
    // [SERVER] DISTRIBUTE COMMANDS TO ALL CLIENTS
    // [CLIENTS + SERVER] EXECUTE COMMANDS FROM REMOTE DRONES

    val localCommands = multiplayerConfig.commandRecorder.popAll()

    val remoteCommands = multiplayerConfig match {
      case AuthoritativeServerConfig(local, remote, clients) =>
        syncWithClients(clients, localCommands)
      case MultiplayerClientConfig(local, remote, server) =>
        server.sendCommands(localCommands)
        server.receiveCommands()
      case SingleplayerConfig =>
        throw new Exception("Matched SingleplayerConfig in syncDroneCommands().")
    }

    executeCommands(remoteCommands, droneRegistry)
  }

  private def syncWithClients(
    clients: Set[RemoteClient],
    localCommands: Seq[(Int, DroneCommand)]
  ): Seq[(Int, DroneCommand)] = {
    val remoteCommands = for (
      client <- clients.toSeq;
      command <- client.waitForCommands()
    ) yield command
    val allCommands = remoteCommands ++ localCommands
    for (
      client <- clients;
      commands = allCommands.filter(d => !client.players.contains(droneRegistry(d._1).player))
    ) client.sendCommands(commands)
    remoteCommands
  }

  private def executeCommands(commands: Seq[(Int, DroneCommand)], droneRegistry: Map[Int, DroneImpl]): Unit = {
    for (
      (id, command) <- commands;
      drone = droneRegistry(id)
    ) drone ! command
  }

  private def collectWorldState(): Unit = {
    _syncMessages = for (drone <- drones)
      yield drone.dynamics.asInstanceOf[ComputedDroneDynamics].state
  }

  private def syncWorldState(): Unit = multiplayerConfig match {
    case AuthoritativeServerConfig(_, _, clients) =>
      for (client <- clients)
        client.sendWorldState(_syncMessages)
    case MultiplayerClientConfig(_, _, server) =>
      val worldState = server.receiveWorldState()
      for (state <- worldState)
        droneRegistry(state.droneId).applyState(state)
    case SingleplayerConfig =>
      throw new Exception("Matched SingleplayerConfig in syncWorldState().")
  }

  private def players = map.initialDrones.map(_.player)
  
  private def playerHasWon(winCondition: WinCondition, player: Player): Boolean =
    winCondition match {
      case DestroyEnemyMotherships =>
        !drones.exists(isLivingEnemyMothership(_, player))
    }

  private def isLivingEnemyMothership(drone: DroneImpl, player: Player): Boolean =
    drone.player != player && !drone.isDead && drone.spec.constructors > 0

  def showVictoryMessage(winner: Player): Unit = {
    for (
      drone <- drones
      if drone.player == winner
    ) Errors.inform("Victory!", drone.position)
  }


  private[codecraft] override def computeWorldState: Iterable[WorldObjectDescriptor] = {
    visibleObjects.flatMap(_.descriptor)
  }

  implicit def droneRegistry: Map[Int, DroneImpl] = _drones


  def replayString: Option[String] = replayRecorder.replayString

  def syncMessages: Iterable[DroneDynamicsState] = _syncMessages


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
private[codecraft] case class MineralCrystalActivated(mineralCrystal: MineralCrystalImpl) extends SimulatorEvent
private[codecraft] case class MineralCrystalInactivated(mineralCrystal: MineralCrystalImpl) extends SimulatorEvent
private[codecraft] case class MineralCrystalDestroyed(mineralCrystal: MineralCrystalImpl) extends SimulatorEvent
private[codecraft] case class SpawnHomingMissile(player: Player, position: Vector2, target: DroneImpl) extends SimulatorEvent
private[codecraft] case class HomingMissileFaded(missile: HomingMissile) extends SimulatorEvent
private[codecraft] case class MissileExplodes(homingMissile: HomingMissile) extends SimulatorEvent
private[codecraft] case class LightFlashDestroyed(lightFlash: LightFlash) extends SimulatorEvent
private[codecraft] case class DroneKilled(drone: DroneImpl) extends SimulatorEvent
private[codecraft] case class DroneConstructionCancelled(drone: DroneImpl) extends SimulatorEvent
private[codecraft] case class SpawnEnergyGlobeAnimation(energyGlobeObject: EnergyGlobeObject) extends SimulatorEvent
private[codecraft] case class RemoveEnergyGlobeAnimation(energyGlobeObject: EnergyGlobeObject) extends SimulatorEvent

