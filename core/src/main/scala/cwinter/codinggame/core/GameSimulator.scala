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

  private val objects = collection.mutable.Set[WorldObject](map.minerals: _*)
  private val drones = collection.mutable.Set.empty[Drone]

  private val visionTracker = new VisionTracker[WorldObject](
    map.size.xMin.toInt, map.size.xMax.toInt,
    map.size.yMin.toInt, map.size.yMax.toInt,
    SightRadius
  )

  private val physicsEngine = new PhysicsEngine[DroneDynamics](
    map.size, MaxDroneRadius
  )

  spawnDrone(Seq(), 3, mothership, Vector2(0, -500))


  override def worldState: Iterable[WorldObjectDescriptor] = objects.map(_.descriptor)

  private def spawnDrone(modules: Seq[Module], size: Int, controller: Any, initialPos: Vector2): Unit = {
    val drone = new Drone(modules, size, controller, initialPos, physicsEngine.time)
    objects.add(drone)
    drones.add(drone)
    visionTracker.insert(drone)
    physicsEngine.addObject(drone.dynamics.unwrap)
  }

  override def update(): Unit = {
    // ADVANCE PHYSICS SIMULATION (n times)
    // COLLECT ALL EVENTS FROM PHYSICS SIMULATION
    // COLLECT ALL EVENTS FROM VISION
    // INVOKE ALL EVENTS ON ROBOTS
  }
}

