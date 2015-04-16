package cwinter.codinggame.physics

import cwinter.codinggame.maths.Rectangle


class PhysicsEngine[T <: DynamicObject[T]](val boundingRectangle: Rectangle) {
  private[this] val objects = collection.mutable.ArrayBuffer.empty[T]
  private[this] var time: Double = 0
  private[this] var nextTime: Double = 0
  private[this] var discreteTime: Int = 0
  private[this] var collisionTable: Map[T, Collision[T]] = null
  val events = collection.mutable.PriorityQueue[Collision[T]]()


  def addObject(obj: T): Unit = {
    objects += obj
  }

  /**
   * Advance simulation by one timestep.
   */
  def update(): Unit = {
    discreteTime += 1
    nextTime = discreteTime / 30.0

    collisionTable = Map.empty[T, Collision[T]]
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


  def updateNextCollision(obj: T): Unit = {
    val collisions = computeCollisions(obj)
    if (collisions.nonEmpty) {
      val nextCol = collisions.minBy(_.time)
      collisionTable += obj -> nextCol
      events.enqueue(nextCol)
      nextCol match {
        case ObjectObjectCollision(_, obj2, t) =>
          if (!collisionTable.contains(obj2) ||
            (collisionTable(obj2).isInstanceOf[ObjectObjectCollision[T]] &&
              collisionTable(obj2).asInstanceOf[ObjectObjectCollision[T]].involvedObjects.contains(obj)) ||
            collisionTable(obj2).time >= t) {
            collisionTable += obj2 -> nextCol
          }
        case _ =>
      }
    }
  }


  def computeCollisions(obj: T): Iterable[Collision[T]] = {
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


  private[PhysicsEngine] sealed trait Collision[TObj] extends Ordered[Collision[TObj]] {
    val time: Double
    def involves(obj: TObj): Boolean
    def involvedObjects: Seq[TObj]
    def obj: TObj
    override def compare(that: Collision[TObj]): Int = that.time compare time
  }

  private[PhysicsEngine] final case class ObjectObjectCollision[TObj](obj1: TObj, obj2: TObj, time: Double) extends Collision[TObj] {
    def involves(obj: TObj): Boolean = obj == obj1 || obj == obj2
    def obj: TObj = obj1
    def involvedObjects: Seq[TObj] = Seq(obj1, obj2)
  }

  private[PhysicsEngine] final case class ObjectWallCollision[TObj](obj: TObj, time: Double) extends Collision[TObj] {
    def involves(obj: TObj): Boolean = obj == this.obj
    def involvedObjects: Seq[TObj] = Seq(obj)
  }
}
