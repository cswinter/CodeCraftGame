package cwinter.codinggame.core

import cwinter.codinggame.collisions.VisionTracker
import cwinter.codinggame.core.drone._
import cwinter.codinggame.physics.PhysicsEngine
import cwinter.codinggame.util.maths.{Rng, Vector2}
import cwinter.codinggame.util.modules.ModulePosition
import cwinter.codinggame.worldstate._


class GameSimulator(
  val map: WorldMap,
  mothershipController1: DroneController,
  mothershipController2: DroneController,
  eventGenerator: Int => Seq[SimulatorEvent]
) extends GameWorld {
  final val SightRadius = 500
  final val MaxDroneRadius = 60

  private val visibleObjects = collection.mutable.Set.empty[WorldObject]
  private val dynamicObjects = collection.mutable.Set.empty[WorldObject]
  private val drones = collection.mutable.Set.empty[Drone]
  private var deadDrones = List.empty[Drone]

  private val visionTracker = new VisionTracker[WorldObject](
    map.size.xMin.toInt, map.size.xMax.toInt,
    map.size.yMin.toInt, map.size.yMax.toInt,
    SightRadius
  )

  private val physicsEngine = new PhysicsEngine[ConstantVelocityDynamics](
    map.size, MaxDroneRadius
  )

  map.minerals.foreach(spawnMineral)
  spawnDrone(mothership(BluePlayer, mothershipController1, Vector2(1000, 200)))
  spawnDrone(mothership(OrangePlayer, mothershipController2, Vector2(-1000, -200)))



  override def worldState: Iterable[WorldObjectDescriptor] = visibleObjects.flatMap(_.descriptor)

  private def spawnMineral(mineralCrystal: MineralCrystal): Unit = {
    visibleObjects.add(mineralCrystal)
    visionTracker.insert(mineralCrystal)
  }


  private def mothership(player: Player, controller: DroneController, pos: Vector2): Drone =
    new Drone(Seq.fill(2)(drone.Manipulator) ++ Seq.fill(3)(NanobotFactory) ++
      Seq.fill(3)(drone.StorageModule) ++ Seq.fill(2)(drone.Lasers), 7, controller, player, pos, 0, 21, true)

  private def spawnDrone(drone: Drone): Unit = {
    visibleObjects.add(drone)
    dynamicObjects.add(drone)
    drones.add(drone)
    visionTracker.insert(drone, generateEvents=true)
    physicsEngine.addObject(drone.dynamics)
    drone.initialise(physicsEngine.time)
  }

  private def droneKilled(drone: Drone): Unit = {
    visibleObjects.remove(drone)
    dynamicObjects.remove(drone)
    drones.remove(drone)
    visionTracker.remove(drone)
    physicsEngine.remove(drone.dynamics)

    deadDrones ::= drone
    drone.enqueueEvent(Destroyed)

    for {
      i <- 0 until ModulePosition.moduleCount(drone.size)
      pos = drone.position + Rng.double(0, drone.radius) * Rng.vector2()
    } spawnLightflash(pos)
  }

  private def spawnMissile(missile: LaserMissile): Unit = {
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
    // handle all drone events (execute user code)
    for (drone <- drones) {
      drone.processEvents()
    }

    for (drone <- deadDrones) {
      drone.processEvents()
    }
    deadDrones = List.empty[Drone]

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
      case DroneConstructionStarted(drone) =>
        visibleObjects.add(drone)
      case DroneConstructionCancelled(drone) =>
        visibleObjects.remove(drone)
      case SpawnLaserMissile(player, position, target) =>
        // TODO: remove this check once boundary collisions are done properly
        if (map.size.contains(position)) {
        val newMissile = new LaserMissile(player, position, physicsEngine.time, target)
          spawnMissile(newMissile)
        }
      case LaserMissileFaded(laserMissile) =>
        visibleObjects.remove(laserMissile)
        dynamicObjects.remove(laserMissile)
        physicsEngine.remove(laserMissile.dynamics)
      case MissileExplodes(laserMissile) =>
        spawnLightflash(laserMissile.position)
      case LightFlashDestroyed(lightFlash) =>
        visibleObjects.remove(lightFlash)
        dynamicObjects.remove(lightFlash)
      case DroneKilled(drone) =>
        droneKilled(drone)
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
case class SpawnLaserMissile(player: Player, position: Vector2, target: Drone) extends SimulatorEvent
case class LaserMissileFaded(laserMissile: LaserMissile) extends SimulatorEvent
case class MissileExplodes(homingMissile: LaserMissile) extends SimulatorEvent
case class LightFlashDestroyed(lightFlash: LightFlash) extends SimulatorEvent
case class DroneKilled(drone: Drone) extends SimulatorEvent
case class DroneConstructionCancelled(drone: Drone) extends SimulatorEvent


