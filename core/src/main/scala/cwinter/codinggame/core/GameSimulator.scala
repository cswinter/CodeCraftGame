package cwinter.codinggame.core

import cwinter.codinggame.physics.PhysicsEngine
import cwinter.codinggame.util.maths.Vector2
import cwinter.collisions.VisionTracker
import cwinter.worldstate.{WorldObjectDescriptor, GameWorld}


class GameSimulator(
  val map: WorldMap,
  mothershipController: DroneController
) extends GameWorld {
  final val SightRadius = 250
  final val MaxDroneRadius = 60

  private val visibleObjects = collection.mutable.Set.empty[WorldObject]
  private val dynamicObjects = collection.mutable.Set.empty[WorldObject]
  private val drones = collection.mutable.Set.empty[Drone]

  private val visionTracker = new VisionTracker[WorldObject](
    map.size.xMin.toInt, map.size.xMax.toInt,
    map.size.yMin.toInt, map.size.yMax.toInt,
    SightRadius
  )

  private val physicsEngine = new PhysicsEngine[ConstantVelocityDynamics](
    map.size, MaxDroneRadius
  )

  map.minerals.foreach(spawnMineral)
  spawnDrone(mothership(mothershipController, Vector2(-50, -400)))



  override def worldState: Iterable[WorldObjectDescriptor] = visibleObjects.map(_.descriptor)

  private def spawnMineral(mineralCrystal: MineralCrystal): Unit = {
    visibleObjects.add(mineralCrystal)
    visionTracker.insert(mineralCrystal)
  }


  private def mothership(controller: DroneController, pos: Vector2): Drone =
    new Drone(Seq.fill(4)(StorageModule) ++ Seq.fill(6)(NanobotFactory), 7, controller, pos, 0, 28)

  private def spawnDrone(drone: Drone): Unit = {
    visibleObjects.add(drone)
    dynamicObjects.add(drone)
    drones.add(drone)
    visionTracker.insert(drone, generateEvents=true)
    physicsEngine.addObject(drone.dynamics)
    drone.initialise(physicsEngine.time)
  }

  private def spawnMissile(missile: LaserMissile): Unit = {
    visibleObjects.add(missile)
    dynamicObjects.add(missile)
    physicsEngine.addObject(missile.dynamics)
  }

  override def update(): Unit = {
    // handle all drone events (execute user code)
    for (drone <- drones) {
      drone.processEvents()
    }

    // execute game mechanics for all objects + collect resulting events
    val simulatorEvents =
      for (obj <- dynamicObjects; event <- obj.update())
        yield event

    simulatorEvents.foreach(println)
    simulatorEvents.foreach {
      case MineralCrystalHarvested(mineralCrystal) =>
        visibleObjects.remove(mineralCrystal)
        visionTracker.remove(mineralCrystal)
      case MineralCrystalDestroyed(mineralCrystal) =>
        visibleObjects.remove(mineralCrystal)
      case MineralCrystalActivated(mineralCrystal) =>
        visibleObjects.add(mineralCrystal)
      case DroneConstructionStarted(drone) =>
        visibleObjects.add(drone)
      case SpawnLaserMissile(position, target) =>
        // TODO: remove this check once boundary collisions are done properly
        if (map.size.contains(position)) {
          val newMissile = new LaserMissile(position, physicsEngine.time, target)
          spawnMissile(newMissile)
        }
      case LaserMissileDestroyed(laserMissile) =>
        visibleObjects.remove(laserMissile)
        dynamicObjects.remove(laserMissile)
        physicsEngine.remove(laserMissile.dynamics)
        val lightFlash = new LightFlash(laserMissile.position)
        visibleObjects.add(lightFlash)
        dynamicObjects.add(lightFlash)
      case LightFlashDestroyed(lightFlash) =>
        visibleObjects.remove(lightFlash)
        dynamicObjects.remove(lightFlash)
      case SpawnDrone(drone) =>
        spawnDrone(drone)
    }

    physicsEngine.update()


    // COLLECT ALL EVENTS FROM PHYSICS SIMULATION

    visionTracker.updateAll()
    for (drone <- drones) drone.objectsInSight = visionTracker.getVisible(drone)
    // SPAWN NEW OBJECTS HERE???
    for {
      (drone: Drone, events) <- visionTracker.collectEvents()
      event <- events
    } event match {
      case visionTracker.EnteredSightRadius(mineral: MineralCrystal) =>
        drone.enqueueEvent(MineralEntersSightRadius(mineral))
      case visionTracker.LeftSightRadius(obj) => // don't care (for now)
      case visionTracker.EnteredSightRadius(other: Drone) =>
        drone.enqueueEvent(DroneEntersSightRadius(other))
      case e => throw new Exception(s"AHHHH, AN UFO!!! RUN FOR YOUR LIFE!!! $e")
    }
  }

  override def timestep = physicsEngine.timestep
}


sealed trait SimulatorEvent
case class MineralCrystalHarvested(mineralCrystal: MineralCrystal) extends SimulatorEvent
case class DroneConstructionStarted(drone: Drone) extends SimulatorEvent
case class SpawnDrone(drone: Drone) extends SimulatorEvent
case class MineralCrystalActivated(mineralCrystal: MineralCrystal) extends SimulatorEvent
case class MineralCrystalDestroyed(mineralCrystal: MineralCrystal) extends SimulatorEvent
case class SpawnLaserMissile(position: Vector2, target: Drone) extends SimulatorEvent
case class LaserMissileDestroyed(laserMissile: LaserMissile) extends SimulatorEvent
case class LightFlashDestroyed(lightFlash: LightFlash) extends SimulatorEvent



