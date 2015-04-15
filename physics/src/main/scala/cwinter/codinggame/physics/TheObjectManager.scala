package cwinter.codinggame.physics

import cwinter.codinggame.maths.Vector2
import robowars.graphics.application.DrawingCanvas
import robowars.worldstate.{WorldObject, GameWorld}


object TheObjectManager extends GameWorld {
  type TObject = MovingObject[ConstantVelocityObject]


  val N = 10
  val objects = List.tabulate[TObject](N * N)(i => {
    val x = i / N
    val y = i % N
    MovingObject(Vector2(-500 + x * 1250 / N, -500 + y * 1250 / N))
  })


  def worldState: Iterable[WorldObject] =
    objects.map(_.state)

  var time: Double = 0
  var nextTime: Double = 0
  var discreteTime: Int = 0
  var collisionTable: Map[TObject, Collision[TObject]] = null
  val events = collection.mutable.PriorityQueue[Collision[TObject]]()

  def update(): Unit = {
    discreteTime += 1
    nextTime = discreteTime / 30.0

    collisionTable = Map.empty[TObject, Collision[TObject]]
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
      // TODO: only update objects affected by collision + subsequent collision checks
      if (collisionTable.contains(collision.obj) && collisionTable(collision.obj) == collision) {
        if (collision.time > time) {
          objects.foreach(_.update(collision.time))
          time = collision.time
        }
        collision match {
          case ObjectWallCollision(obj, _) => obj.wallCollision()
          case ObjectObjectCollision(obj1, obj2, _) =>
            if (collisionTable.contains(obj1) &&
              collisionTable.contains(obj2) &&
              collisionTable(obj1) == collision && // redundant
              collisionTable(obj2) == collision) {
              obj1.collision(obj2)
            }
        }
        for (obj <- collision.involvedObjects) collisionTable -= obj

        collision.involvedObjects.foreach(updateNextCollision)
      }
    }

    objects.foreach(_.update(nextTime))
    time = nextTime
  }


  def updateNextCollision(obj: TObject): Unit = {
    val collisions = computeCollisions(obj)
    if (collisions.nonEmpty) {
      val nextCol = collisions.minBy(_.time)
      collisionTable += obj -> nextCol
      events.enqueue(nextCol)
      nextCol match {
        case ObjectObjectCollision(_, obj2, t) =>
          if (!collisionTable.contains(obj2) || (collisionTable(obj2).isInstanceOf[ObjectObjectCollision[TObject]] && collisionTable(obj2).asInstanceOf[ObjectObjectCollision[TObject]].involvedObjects.contains(obj)) || collisionTable(obj2).time >= t) {
            collisionTable += obj2 -> nextCol
          }
        case _ =>
      }
    }
  }

  def computeCollisions(obj: TObject): Iterable[Collision[TObject]] = {
    val objectObjectCollisions =
      for {
        obji <- objects
        dt <- obj.collisionTime(obji, nextTime)
      } yield ObjectObjectCollision(obj, obji, time + dt)

    val objectWallCollisions =
      for {
        dt <- obj.wallCollisionTime(nextTime)
      } yield ObjectWallCollision(obj, time + dt)

    objectObjectCollisions ++ objectWallCollisions
  }


  def main(args: Array[String]): Unit = {
    DrawingCanvas.run(this)
  }
}

sealed trait Collision[TObj] extends Ordered[Collision[TObj]] {
  val time: Double

  def involves(obj: TObj): Boolean

  def involvedObjects: Seq[TObj]

  def obj: TObj

  override def compare(that: Collision[TObj]): Int = that.time compare time
}

final case class ObjectObjectCollision[TObj](obj1: TObj, obj2: TObj, time: Double) extends Collision[TObj] {
  def involves(obj: TObj): Boolean = obj == obj1 || obj == obj2

  def obj: TObj = obj1

  def involvedObjects: Seq[TObj] = Seq(obj1, obj2)
}

final case class ObjectWallCollision[TObj](obj: TObj, time: Double) extends Collision[TObj] {
  def involves(obj: TObj): Boolean = obj == this.obj

  def involvedObjects: Seq[TObj] = Seq(obj)
}
