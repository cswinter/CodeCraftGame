package cwinter.codinggame.core

import cwinter.codinggame.maths.Vector2
import cwinter.codinggame.physics.PhysicsEngine
import cwinter.collisions.VisionTracker
import cwinter.worldstate.{WorldObjectDescriptor, GameWorld}


class GameSimulator(
  val map: Map,
  mothership: DroneController
) extends GameWorld {
  final val SightRadius = 250
  final val MaxDroneRadius = 60

  private val visibleObjects = collection.mutable.Set.empty[WorldObject]
  private val drones = collection.mutable.Set.empty[Drone]

  private val visionTracker = new VisionTracker[WorldObject](
    map.size.xMin.toInt, map.size.xMax.toInt,
    map.size.yMin.toInt, map.size.yMax.toInt,
    SightRadius
  )

  private val physicsEngine = new PhysicsEngine[DroneDynamics](
    map.size, MaxDroneRadius
  )

  map.minerals.foreach(spawnMineral)
  spawnDrone(Seq.fill(4)(StorageModule) ++ Seq.fill(6)(NanobotFactory),
    7, mothership, Vector2(0, -500), 28)



  override def worldState: Iterable[WorldObjectDescriptor] = visibleObjects.map(_.descriptor)

  private def spawnMineral(mineralCrystal: MineralCrystal): Unit = {
    visibleObjects.add(mineralCrystal)
    visionTracker.insert(mineralCrystal)
  }

  private def spawnDrone(modules: Seq[Module], size: Int, controller: DroneController, initialPos: Vector2, startingResources: Int = 0): Unit = {
    val drone = new Drone(modules, size, controller, initialPos, physicsEngine.time, startingResources)
    visibleObjects.add(drone)
    drones.add(drone)
    visionTracker.insert(drone, generateEvents=true)
    physicsEngine.addObject(drone.dynamics.unwrap)
    controller.initialise(drone)
  }

  override def update(): Unit = {
    // INVOKE ALL EVENTS FROM LAST TIMESTEP, COLLECT DRONE COMMANDS
    drones.foreach { drone =>
      drone.processEvents()
    }

    val simulatorEvents =
      for (drone <- drones; event <- drone.processCommands())
        yield event

    simulatorEvents.foreach {
      case MineralCrystalHarvested(mineralCrystal) => ???
      case DroneConstructionStarted(drone) =>
        visibleObjects.add(drone)
    }

    physicsEngine.update()

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
      case e => throw new Exception(s"AHHHH, AN UFO!!! RUN FOR YOUR LIFE!!! $e")
    }
    // COLLECT ALL EVENTS FROM VISION
  }

  override def timestep = physicsEngine.timestep
}


sealed trait SimulatorEvent
case class MineralCrystalHarvested(mineralCrystal: MineralCrystal) extends SimulatorEvent
case class DroneConstructionStarted(drone: Drone) extends SimulatorEvent



