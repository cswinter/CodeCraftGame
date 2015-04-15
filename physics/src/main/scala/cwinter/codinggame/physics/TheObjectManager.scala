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

  def update(): Unit = {
    discreteTime += 1
    nextTime = discreteTime / 30.0

    var events = collection.mutable.PriorityQueue[Collision[TObject]]()
    for {
      obji <- objects
      collision <- computeCollisions(obji)
    } events.enqueue(collision)

    while (events.size > 0) {
      val collision = events.dequeue()

      println(f"collision at ts=$discreteTime: $collision")
      // TODO: only update objects affected by collision + subsequent collision checks
      objects.foreach(_.update(collision.time))
      time = collision.time

      collision match {
        case ObjectWallCollision(obj, _) => obj.wallCollision()
        case ObjectObjectCollision(obj1, obj2, _) =>
          obj1.collision(obj2)
          // TODO: do this efficiently
          events = events.filterNot(c =>
            if (c.involves(obj1) || c.involves(obj2)) {
              println(s"removed $c")
              true
            } else false)
      }

      for {
        obj <- collision.involvedObjects
        c <- computeCollisions(obj, idCheck = false)
      } events.enqueue(c)
    }

    objects.foreach(_.update(nextTime))
    time = nextTime
  }

  def computeCollisions(obj: TObject, idCheck: Boolean = true): Iterable[Collision[TObject]] = {
    val objectObjectCollisions =
      for {
        obji <- objects
        if !idCheck || obj.id < obji.id
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
  override def compare(that: Collision[TObj]): Int = that.time compare time
}

final case class ObjectObjectCollision[TObj](obj1: TObj, obj2: TObj, time: Double) extends Collision[TObj] {
  def involves(obj: TObj): Boolean = obj == obj1 || obj == obj2
  def involvedObjects: Seq[TObj] = Seq(obj1, obj2)
}

final case class ObjectWallCollision[TObj](obj: TObj, time: Double) extends Collision[TObj] {
  def involves(obj: TObj): Boolean = obj == this.obj
  def involvedObjects: Seq[TObj] = Seq(obj)
}
