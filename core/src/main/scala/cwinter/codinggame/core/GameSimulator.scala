package cwinter.codinggame.core

import cwinter.collisions.VisionTracker
import cwinter.worldstate.{WorldObjectDescriptor, GameWorld}


class GameSimulator(
  val map: Map
  // initial world state: resources, drones
) extends GameWorld {
  final val SightRadius = 250

  private val objects = collection.mutable.Set[WorldObject](map.minerals: _*)
  private val visionTracker = new VisionTracker[WorldObject](
    map.size.xMin.toInt, map.size.xMax.toInt,
    map.size.yMin.toInt, map.size.yMin.toInt,
    SightRadius
  )


  override def worldState: Iterable[WorldObjectDescriptor] = objects.map(_.descriptor)
  override def update(): Unit = {
    // ADVANCE PHYSICS SIMULATION (n times)
    // COLLECT ALL EVENTS FROM PHYSICS SIMULATION
    // COLLECT ALL EVENTS FROM VISION
    // INVOKE ALL EVENTS ON ROBOTS
  }
}

