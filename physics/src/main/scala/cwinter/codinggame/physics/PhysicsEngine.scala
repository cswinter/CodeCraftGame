package cwinter.codinggame.physics

import cwinter.codinggame.maths.Rectangle


class PhysicsEngine[T <: DynamicObject[T]](val worldBoundaries: Rectangle) {
  private[this] val objects = collection.mutable.ArrayBuffer.empty[ObjectRecord]
  private[this] var time: Double = 0
  private[this] var nextTime: Double = 0
  private[this] var discreteTime: Int = 0
  private[this] val events = collection.mutable.PriorityQueue[Collision]()


  def addObject(obj: T): Unit = {
    objects += new ObjectRecord(obj)
  }

  /**
   * Advance simulation by one timestep.
   */
  def update(): Unit = {
    discreteTime += 1
    nextTime = discreteTime / 30.0

    objects.foreach(x => updateNextCollision(x))

    while (events.size > 0) {
      val collision = events.dequeue()

      // POTENTIAL PROBLEMS in A<->B COLLISION:
      // 1.
      // handle A<->B => T[A]=T[B]=NULL
      // handle B<->A => T[A] is NULL!
      // 2.
      // A<->B=@1 is enqueued (T[A]=T[B]=@1)
      // B<->A=@2 is enqueued !(T[B]=@2)
      // now, neither @1 nor @2 can be processed, leading to infinite loop.


      println(f"collision at ts=$discreteTime: $collision")
      
      collision match {
        case ObjectWallCollision(obj, t) =>
          if (obj.nextCollision == Some(collision)) {
            time = t
            obj.updatePosition(t)

            obj.handleWallCollision(worldBoundaries)
            updateNextCollision(obj)
          }
        case ObjectObjectCollision(obj1, obj2, t) =>
          if (obj1.nextCollision == Some(collision)) {
            if (obj2.nextCollision == Some(collision)) {
              time = t
              obj1.updatePosition(t)
              obj2.updatePosition(t)

              obj1.handleObjectCollision(obj2)
              updateNextCollision(obj2)
            }
            updateNextCollision(obj1)
          }
        case _ => ???
      }
    }

    objects.foreach(_.obj.updatePosition(nextTime))
    time = nextTime
  }


  private def updateNextCollision(obj: ObjectRecord): Unit = {
    obj.nextCollision = None
    val collisions = computeCollisions(obj)
    if (collisions.nonEmpty) {
      val nextCol = collisions.minBy(_.time)
      val nextColOpt = Some(nextCol)
      obj.nextCollision = nextColOpt
      events.enqueue(nextCol)
      nextCol match {
        case ObjectObjectCollision(_, obj2, t) =>
          // TODO: make this not horrible
          if (obj2.nextCollision == None ||
            (obj2.nextCollision.get.isInstanceOf[ObjectObjectCollision] &&
              obj2.nextCollision.get.asInstanceOf[ObjectObjectCollision].obj2 == obj) ||
            obj2.nextCollision.get.time >= t) {
            obj2.nextCollision = nextColOpt
          }
        case _ =>
      }
    }
  }


  private final class ObjectRecord(
    val obj: T,
    var cellX: Int = 0,
    var cellY: Int = 0,
    var nextCollision: Option[Collision] = None,
    var nextTransfer: Any = null
  )

  import language.implicitConversions
  private implicit def objectRecordIsT(objRec: ObjectRecord): T = objRec.obj


  private def computeCollisions(obj: ObjectRecord): Iterable[Collision] = {
    val nearbyObjects = objects
    nearbyObjects.foreach(_.obj.updatePosition(time))
    val objectObjectCollisions =
      for {
        obji <- nearbyObjects
        dt <- obj.obj.collisionTime(obji.obj, nextTime)
      } yield ObjectObjectCollision(obj, obji, time + dt)

    val objectWallCollisions =
      for {
        dt <- obj.obj.wallCollisionTime(worldBoundaries, nextTime)
      } yield ObjectWallCollision(obj, time + dt)

    objectObjectCollisions ++ objectWallCollisions
  }


  private sealed trait Collision extends Ordered[Collision] {
    val time: Double
    override def compare(that: Collision): Int = that.time compare time
  }

  private final case class ObjectObjectCollision(
    obj1: ObjectRecord,
    obj2: ObjectRecord,
    time: Double
  ) extends Collision

  private final case class ObjectWallCollision(
    obj: ObjectRecord,
    time: Double
  ) extends Collision

  private final case class Transfer(
    obj: ObjectRecord,
    time: Double,
    newCellX: Int,
    newCellY: Int
  ) extends Collision
}
