package cwinter.codinggame.core

import cwinter.codinggame.maths.Rectangle
import cwinter.collisions.VisionTracker
import cwinter.worldstate.{WorldObjectDescriptor, GameWorld}


class GameSimulator(
  val worldSize: Rectangle,
  initialObjects: Seq[WorldObject]
  // initial world state: resources, drones
) extends GameWorld {
  final val SightRadius = 250

  private val objects = collection.mutable.Set[WorldObject](initialObjects: _*)
  private val visionTracker = new VisionTracker[WorldObject](
    worldSize.xMin.toInt, worldSize.xMax.toInt,
    worldSize.yMin.toInt, worldSize.yMin.toInt,
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

