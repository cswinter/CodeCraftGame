package cwinter.codecraft.core

import cwinter.codecraft.collisions.VisionTracker
import cwinter.codecraft.core.api.{BluePlayer, Player, DroneSpec}
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
 * @param eventGenerator Allows for triggering custom events.
 * @param replayer If set to `Some(r)`, the Simulator will replay the events recorded by `r`.
 */
class DroneWorldSimulator(
  val map: WorldMap,
  eventGenerator: Int => Seq[SimulatorEvent],
  replayer: Option[Replayer] = None
) extends Simulator {
  private final val MaxDroneRadius = 60

  private var showSightRadius = false
  private var showMissileRadius = false

  private val replayRecorder =
    if (replayer.isEmpty) ReplayFactory.replayRecorder
    else NullReplayRecorder

  private val worldConfig = WorldConfig(map.size)
  private val visibleObjects = collection.mutable.Set.empty[WorldObject]
  private val dynamicObjects = collection.mutable.Set.empty[WorldObject]
  private val _drones = collection.mutable.Set.empty[DroneImpl]
  private def drones = _drones
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

  replayRecorder.recordVersion()
  replayRecorder.recordRngSeed(Rng.seed)
  replayRecorder.recordWorldSize(map.size)
  map.minerals.foreach(replayRecorder.recordMineral)
  map.minerals.foreach(spawnMineral)
  for {
    Spawn(spec, controller, position, player, resources, _) <- map.initialDrones
    drone = new DroneImpl(spec, controller, player, position, 0, worldConfig, replayRecorder, resources)
  } {
    spawnDrone(drone)
    replayRecorder.recordSpawn(spec, position, player)
  }

  @JSExport
  val namedDrones = (for (Spawn(_, controller, _, _, _, Some(name)) <- map.initialDrones) yield (
    name,
    if (controller.playerID == BluePlayer.id) controller
    else new EnemyDrone(controller.drone, controller.drone.player)
  )).toMap


  private def spawnMineral(mineralCrystal: MineralCrystalImpl): Unit = {
    visibleObjects.add(mineralCrystal)
    visionTracker.insert(mineralCrystal)
  }

  private def spawnDrone(drone: DroneImpl): Unit = {
    visibleObjects.add(drone)
    dynamicObjects.add(drone)
    _drones.add(drone)
    visionTracker.insert(drone, generateEvents=true)
    physicsEngine.addObject(drone.dynamics)
    drone.initialise(physicsEngine.time)
  }

  private def droneKilled(drone: DroneImpl): Unit = {
    visibleObjects.remove(drone)
    dynamicObjects.remove(drone)
    _drones.remove(drone)
    visionTracker.remove(drone)
    physicsEngine.remove(drone.dynamics)

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

    if (showMissileRadius) {
      for (
        d <- _drones
        if d.spec.missileBatteries > 0
      ) {
        Debug.draw(DrawCircleOutline(d.position.x.toFloat, d.position.y.toFloat, DroneConstants.MissileLockOnRadius, ColorRGB(1, 0, 0)))
      }
    }
    if (showSightRadius) {
      for (d <- _drones) {
        Debug.draw(DrawCircleOutline(d.position.x.toFloat, d.position.y.toFloat, DroneSpec.SightRadius, ColorRGB(0, 1, 0)))
      }
    }

    if (replayer.exists(_.finished)) return

    // check win condition
    if (timestep % 30 == 0) {
      for ((player, winCondition) <- map.winConditions) {
        if (winCondition(this)) {
          for (
            drone <- drones
            if drone.player == player
          ) Errors.inform("Victory!", drone.position)
        }
      }
    }

    for (r <- replayer) {
      implicit val droneRegistry = _drones.map(d => (d.id, d)).toMap
      implicit val mineralRegistry = map.minerals.map(m => (m.id, m)).toMap
      r.run(timestep)
    }

    // handle all drone events (execute user code)
    for (drone <- _drones) {
      drone.processEvents()
    }

    for (drone <- deadDrones) {
      drone.processEvents()
    }
    deadDrones = List.empty[DroneImpl]

    // execute game mechanics for all objects + collect resulting events
    val simulatorEvents = {
      for (obj <- dynamicObjects; event <- obj.update())
        yield event
    } ++ eventGenerator(timestep)


    simulatorEvents.foreach {
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

    physicsEngine.update()

    // COLLECT ALL EVENTS FROM PHYSICS SIMULATION

    visionTracker.updateAll()
    for (drone <- _drones) drone.objectsInSight = visionTracker.getVisible(drone)
    // SPAWN NEW OBJECTS HERE???
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

    Errors.updateMessages()
  }


  private[codecraft] override def computeWorldState: Iterable[WorldObjectDescriptor] = {
    visibleObjects.flatMap(_.descriptor)
  }


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

