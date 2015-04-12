package cwinter.codinggame.physics

import cwinter.codinggame.maths.Vector2
import robowars.graphics.application.DrawingCanvas
import robowars.worldstate.{WorldObject, GameWorld}


object P3Ordering extends Ordering[(ConstantVelocityObject, ConstantVelocityObject, Float)] {
  override def compare(x: (ConstantVelocityObject, ConstantVelocityObject, Float), y: (ConstantVelocityObject, ConstantVelocityObject, Float)): Int = y._3 compare x._3
}

object TheObjectManager extends GameWorld {
  val objects = List.tabulate(100)(i => {
    val x = i / 10
    val y = i % 10
    MovingObject(Vector2(-500 + x * 125, -500 + y * 125))
  })
  var timesteps = 0


  def worldState: Iterable[WorldObject] =
    objects.map(_.state)

  def update(): Unit = {
    var currTime = timesteps / 30f
    timesteps += 1
    val time = timesteps / 30f

    // determine next collision
    val collisionTimes = for {
      obj1 <- objects
      obj2 <- objects
      if obj1.id < obj2.id
      t <- obj1.collisionTime(obj2, time)
    } yield (obj1, obj2, currTime + t)

    val wallCollisionTimes =
      for {
        obj <- objects
        t <- obj.wallCollisionTime(time)
      } yield (obj, obj, currTime + t)


    val ignore = collection.mutable.Set.empty[(ConstantVelocityObject, ConstantVelocityObject, Float)]
    var events = collection.mutable.PriorityQueue[(ConstantVelocityObject, ConstantVelocityObject, Float)]()(P3Ordering)
    events ++= collisionTimes
    events ++= wallCollisionTimes

    while (events.size > 0) {
      val col@(obj1, obj2, t) = events.dequeue()

      if (!ignore.contains(col)) {
        println(f"collision at ts=$timesteps t=$t%.3f between ${obj1.id} and ${obj2.id}")
        // TODO: only update objects affected by collision + subsequent collision checks
        objects.foreach(_.update(t))
        currTime = t
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
          /*for (x <- events.clone()) {
            if (x._1 == obj1 || x._2 == obj1 || x._1 == obj2 || x._2 == obj2) {
              println(s"removed ${x._1.id}, ${x._2.id}")
              ignore += x
            }
          }*/
        }
        for {
          obji <- objects
          if obji != obj1 && obji != obj2
          dt <- obj1.collisionTime(obji, time)
        } {
          println(s"add ${obj1.id}, ${obji.id}"); events.enqueue((obj1, obji, currTime + dt))
        }
        if (obj1 != obj2) {
          for {
            obji <- objects
            if obji != obj1 && obji != obj2
            dt <- obj2.collisionTime(obji, time)
          } {
            println(s"add ${obj2.id}, ${obji.id}"); events.enqueue((obj2, obji, currTime + dt))
          }
        }
        for {
          obj <- if (obj1 == obj2) Seq(obj1) else Seq(obj1, obj2)
          dt <- obj.wallCollisionTime(time)
        } {
          println(s"add wall collision ${obj1.id}, dt=$dt, currTime=$currTime")
          if (dt < 0) {
            ;
          }
          events.enqueue((obj, obj, currTime + dt))
        }
      }
    }

    objects.foreach(_.update(time))
  }

  def main(args: Array[String]): Unit = {
    DrawingCanvas.run(this)
  }
}
