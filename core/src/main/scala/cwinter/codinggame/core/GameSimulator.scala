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
  // TODO: only one set for objects that need to be updated
  private val drones = collection.mutable.Set.empty[Drone]
  private val missiles = collection.mutable.Set.empty[LaserMissile]
  private val lightFlashes = collection.mutable.Set.empty[LightFlash]

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
    drones.add(drone)
    visionTracker.insert(drone, generateEvents=true)
    physicsEngine.addObject(drone.dynamics.unwrap)
    drone.initialise(physicsEngine.time)
  }

  private def spawnMissile(missile: LaserMissile): Unit = {
    visibleObjects.add(missile)
    missiles.add(missile)
    //physicsEngine.addObject(missile.dynamics)
  }

  override def update(): Unit = {
    // INVOKE ALL EVENTS FROM LAST TIMESTEP, COLLECT DRONE COMMANDS
    drones.foreach { drone =>
      drone.processEvents()
    }

    missiles.foreach { missile =>
      missile.update()
    }

    val simulatorEvents =
      for (drone <- drones; event <- drone.processCommands())
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
        val newMissile = new LaserMissile(position, physicsEngine.time, target)
        spawnMissile(newMissile)
      case SpawnDrone(drone) =>
        spawnDrone(drone)
    }

    physicsEngine.update()
    lightFlashes.foreach(_.update())
    missiles.foreach { missile =>
      missile.dynamics.updatePosition(physicsEngine.time)
      if (missile.hasDied) {
        val lf = new LightFlash(missile.position.x.toFloat, missile.position.y.toFloat)
        visibleObjects.add(lf)
        lightFlashes.add(lf)
      }
    }
    missiles.retain(!_.hasDied)
    lightFlashes.retain(!_.hasDied)
    visibleObjects.retain(!_.hasDied)


    // COLLECT ALL EVENTS FROM PHYSICS SIMULATION


    visionTracker.updateAll()
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
    // COLLECT ALL EVENTS FROM VISION
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
