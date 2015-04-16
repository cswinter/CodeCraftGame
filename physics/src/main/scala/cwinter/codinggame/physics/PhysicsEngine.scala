package cwinter.codinggame.physics

import cwinter.codinggame.maths.Rectangle


class PhysicsEngine[T <: DynamicObject[T]](val boundingRectangle: Rectangle) {
  private[this] val objects = collection.mutable.ArrayBuffer.empty[T]
  private[this] var time: Double = 0
  private[this] var nextTime: Double = 0
  private[this] var discreteTime: Int = 0
  private[this] var collisionTable: Map[T, Collision] = null
  val events = collection.mutable.PriorityQueue[Collision]()


  def addObject(obj: T): Unit = {
    objects += obj
  }

  /**
   * Advance simulation by one timestep.
   */
  def update(): Unit = {
    discreteTime += 1
    nextTime = discreteTime / 30.0

    collisionTable = Map.empty[T, Collision]
    objects.foreach(updateNextCollision)

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
      if (collisionTable.contains(collision.obj) && collisionTable(collision.obj) == collision) {
        if (collision.time > time) {
          collision.involvedObjects.foreach(_.updatePosition(collision.time))
          time = collision.time
        }
        collision match {
          case ObjectWallCollision(obj, _) => obj.handleWallCollision(boundingRectangle)
          case ObjectObjectCollision(obj1, obj2, _) =>
            if (collisionTable.contains(obj1) &&
              collisionTable.contains(obj2) &&
              collisionTable(obj1) == collision && // redundant
              collisionTable(obj2) == collision) {
              obj1.handleObjectCollision(obj2)
            }
        }
        for (obj <- collision.involvedObjects) collisionTable -= obj

        collision.involvedObjects.foreach(updateNextCollision)
      }
    }

    objects.foreach(_.updatePosition(nextTime))
    time = nextTime
  }


  private def updateNextCollision(obj: T): Unit = {
    val collisions = computeCollisions(obj)
    if (collisions.nonEmpty) {
      val nextCol = collisions.minBy(_.time)
      collisionTable += obj -> nextCol
      events.enqueue(nextCol)
      nextCol match {
        case ObjectObjectCollision(_, obj2, t) =>
          if (!collisionTable.contains(obj2) ||
            (collisionTable(obj2).isInstanceOf[ObjectObjectCollision] &&
              collisionTable(obj2).asInstanceOf[ObjectObjectCollision].involvedObjects.contains(obj)) ||
            collisionTable(obj2).time >= t) {
            collisionTable += obj2 -> nextCol
          }
        case _ =>
      }
    }
  }


  private def computeCollisions(obj: T): Iterable[Collision] = {
    val nearbyObjects = objects
    nearbyObjects.foreach(_.updatePosition(time))
    val objectObjectCollisions =
      for {
        obji <- nearbyObjects
        dt <- obj.collisionTime(obji, nextTime)
      } yield ObjectObjectCollision(obj, obji, time + dt)

    val objectWallCollisions =
      for {
        dt <- obj.wallCollisionTime(boundingRectangle, nextTime)
      } yield ObjectWallCollision(obj, time + dt)

    objectObjectCollisions ++ objectWallCollisions
  }


  private sealed trait Collision extends Ordered[Collision] {
    val time: Double
    def involves(obj: T): Boolean
    def involvedObjects: Seq[T]
    def obj: T
    override def compare(that: Collision): Int = that.time compare time
  }

  private final case class ObjectObjectCollision(obj1: T, obj2: T, time: Double) extends Collision {
    def involves(obj: T): Boolean = obj == obj1 || obj == obj2
    def obj: T = obj1
    def involvedObjects: Seq[T] = Seq(obj1, obj2)
  }

  private final case class ObjectWallCollision(obj: T, time: Double) extends Collision {
    def involves(obj: T): Boolean = obj == this.obj
    def involvedObjects: Seq[T] = Seq(obj)
  }
}
