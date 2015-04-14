package cwinter.codinggame.physics

import cwinter.codinggame.maths.Vector2
import robowars.graphics.application.DrawingCanvas
import robowars.worldstate.{WorldObject, GameWorld}


class P3Ordering[T1,T2] extends Ordering[(T1, T2, Double)] {
  override def compare(x: (T1, T2, Double), y: (T1, T2, Double)): Int = y._3 compare x._3
}

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

    var events = collection.mutable.PriorityQueue[(TObject, TObject, Double)]()(new P3Ordering)
    for {
      obji <- objects
      collision <- computeCollisions(obji)
    } events.enqueue(collision)

    while (events.size > 0) {
      val (obj1, obj2, t) = events.dequeue()

      println(f"collision at ts=$discreteTime t=$t%.3f between ${obj1.id} and ${obj2.id}")
      // TODO: only update objects affected by collision + subsequent collision checks
      objects.foreach(_.update(t))
      time = t
      if (obj1 == obj2) {
        // object <-> wall collision
        obj1.wallCollision()
      } else {
        // object <-> object collision
        obj1.collision(obj2)
        // TODO: do this efficiently
        events = events.filterNot(x =>
          if (x._1 == obj1 || x._2 == obj1 || x._1 == obj2 || x._2 == obj2) {
            println(s"removed ${x._1.id}, ${x._2.id}")
            true
          } else false)
      }

      for {
        c <- computeCollisions(obj1, idCheck=false)
      } events.enqueue(c)
      if (obj1 != obj2) {
        for {
          c <- computeCollisions(obj2, idCheck=false)
        } events.enqueue(c)
      }
    }

    objects.foreach(_.update(nextTime))
    time = nextTime
  }

  def computeCollisions(obj: TObject, idCheck: Boolean = true): Iterable[(TObject, TObject, Double)] = {
    val objectObjectCollisions =
      for {
        obji <- objects
        if !idCheck || obj.id < obji.id
        dt <- obj.collisionTime(obji, nextTime)
      } yield (obj, obji, time + dt)

    val objectWallCollisions =
      for {
        dt <- obj.wallCollisionTime(nextTime)
      } yield (obj, obj, time + dt)

    objectObjectCollisions ++ objectWallCollisions
  }


  def main(args: Array[String]): Unit = {
    DrawingCanvas.run(this)
  }
}
