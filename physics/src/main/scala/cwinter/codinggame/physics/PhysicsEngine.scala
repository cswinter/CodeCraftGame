package cwinter.codinggame.physics

import cwinter.codinggame.maths.{Vector2, Rectangle}
import cwinter.collisions.{Positionable, SquareGrid}
import robowars.graphics.engine.Debug
import robowars.worldstate.{WorldObject, Rectangle => DrawRectangle}


class PhysicsEngine[T <: DynamicObject[T]](val worldBoundaries: Rectangle, val maxRadius: Int) {
  private[this] val objects = collection.mutable.ArrayBuffer.empty[ObjectRecord]
  private[this] var time: Double = 0
  private[this] var nextTime: Double = 0
  private[this] var discreteTime: Int = 0
  private[this] val events = collection.mutable.PriorityQueue[Collision]()
  // TODO: convert to int better, or allow floating point boundaries
  private[this] val grid = new SquareGrid[ObjectRecord](
    worldBoundaries.xMin.toInt,
    worldBoundaries.xMax.toInt,
    worldBoundaries.yMin.toInt,
    worldBoundaries.yMax.toInt,
    2 * maxRadius
  )(ObjectRecordHasPosition)

  Debug.drawAlways(DrawRectangle(-1, worldBoundaries))

  def addObject(obj: T): Unit = {
    val (x, y) = grid.computeCell(obj.pos)
    val record = new ObjectRecord(obj, x, y)
    objects += record
    grid.insert(record, x, y)
  }

  /**
   * Advance simulation by one timestep.
   */
  def update(): Unit = {

    discreteTime += 1
    nextTime = discreteTime / 30.0

    objects.foreach(obj => {
      updateNextCollision(obj, grid.reducedNearbyObjects(obj.cellX, obj.cellY), erase = false)
      updateTransfer(obj)
    })

    var lastEvent: Collision = null
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
      // 3. numerical problems (after collision with wall, object, there is slight overlap)
      // 4. various erasure problems (e.g. A, B, on collision course, C collides with D collides with A, making B an orphan)
      // 5. duplicate wall collision after transfer


      println(f"collision at ts=$discreteTime: $collision")

      if (collision == lastEvent) {
        println(">>> END HERE")
        throw new Exception("Oh noes!")
      }
      lastEvent = collision


      collision match {
        case ObjectWallCollision(obj, t) =>
          println("match object wall")
          if (obj.nextCollision == Some(collision)) {
            println("check success")
            println(s"object state: ${obj.obj}")
            time = t
            obj.updatePosition(t)
            println(s"after position update: ${obj.obj}")

            obj.handleWallCollision(worldBoundaries)
            updateNextCollision(obj, grid.nearbyObjects(obj.cellX, obj.cellY))
            println(s"after handling wall collision: ${obj.obj}")

            updateTransfer(obj)
          } else {
            println(s"check failure: ${obj.nextCollision} != ${Some(collision)}")
          }
        case ObjectObjectCollision(obj1, obj2, t) =>
          if (obj1.nextCollision == Some(collision)) {
            println("Check 1 success")
            println(f"obj2.nextCollision=${obj2.nextCollision}")
            if (obj2.nextCollision == Some(collision)) {
              println("Check 2 success")
              time = t
              obj1.updatePosition(t)
              obj2.updatePosition(t)

              obj1.handleObjectCollision(obj2)
              updateNextCollision(obj2, grid.nearbyObjects(obj2.cellX, obj2.cellY))

              updateTransfer(obj1)
              updateTransfer(obj2)
            }
            updateNextCollision(obj1, grid.nearbyObjects(obj1.cellX, obj1.cellY))
          } else if (obj2.nextCollision == Some(collision)) {
            updateNextCollision(obj2, grid.nearbyObjects(obj2.cellX, obj2.cellY))
          }
        case Transfer(obj, t, x, y, d) =>
          if (obj.nextTransfer == Some(collision)) {
            time = t
            obj.updatePosition(t)

            // TODO: rework Transfer to eliminate superfluous cell computations, parameters etc.
            val newlyNearbyObjects =
              if (d.xAxisAligned) grid.xTransfer(obj, (obj.cellX, obj.cellY), d.x)
              else grid.yTransfer(obj, (obj.cellX, obj.cellY), d.y)
            obj.cellX = x
            obj.cellY = y
            updateNextCollision(obj, newlyNearbyObjects, erase = false, pathUnchanged = true)

            updateTransfer(obj)
          }
      }
    }

    objects.foreach(_.obj.updatePosition(nextTime))
    time = nextTime
  }

  // TODO: return new collision, rather than assigning?
  // TODO: eradicate use of Iterator. how could you?
  private def updateNextCollision(obj: ObjectRecord, nearbyObjects: Iterator[ObjectRecord], erase: Boolean = true, pathUnchanged: Boolean = false): Unit = {
    // TODO: retain previous collision, unless erased
    if (erase) {
      println(f"erasing next collision $obj, ${obj.nextCollision}")
      obj.nextCollision = None
    }
    val collisions = computeCollisions(obj, nearbyObjects, pathUnchanged)
    if (collisions.nonEmpty) {
      val nextCol = collisions.minBy(_.time)
      if (obj.nextCollision.map(_.time <= nextCol.time) == Some(true)) return

      val nextColOpt = Some(nextCol)
      obj.nextCollision = nextColOpt
      events.enqueue(nextCol)
      println(f"enqueued collision: $nextCol")
      nextCol match {
        case ObjectObjectCollision(_, obj2, t) =>
          // TODO: make this not horrible
          if (obj2.nextCollision == None ||
            (obj2.nextCollision.get.isInstanceOf[ObjectObjectCollision] &&
              (obj2.nextCollision.get.asInstanceOf[ObjectObjectCollision].obj2 == obj ||
                obj2.nextCollision.get.asInstanceOf[ObjectObjectCollision].obj1 == obj)) ||
            obj2.nextCollision.get.time >= t) {
            obj2.nextCollision = nextColOpt
            println(f"augmented collision: $nextCol")
          }
        case owc: ObjectWallCollision =>
        case _ => throw new Exception("this shouldn't happen...")
      }
    }
  }


  private def computeCollisions(obj: ObjectRecord, nearbyObjects: Iterator[ObjectRecord], pathUnchanged: Boolean): Seq[Collision] = {
    val nearby = nearbyObjects.toSeq
    nearby.foreach(_.obj.updatePosition(time))
    val objectObjectCollisions =
      for {
        obji <- nearby
        if obj != obji
        dt <- obj.collisionTime(obji, nextTime)
      } yield ObjectObjectCollision(obj, obji, time + dt)

    val objectWallCollisions =
      if (pathUnchanged) None
      else for {
        (dt, direction) <- obj.wallCollisionTime(worldBoundaries, nextTime)
      } yield ObjectWallCollision(obj, time + dt)

    objectObjectCollisions ++ objectWallCollisions
  }


  private def updateTransfer(obj: ObjectRecord): Unit = {
    obj.nextTransfer = None
    val cellBounds = grid.cellBounds(obj.cellX, obj.cellY)
    for ((dt, direction) <- obj.wallCollisionTime(cellBounds, nextTime)) {
      val newX = obj.cellX + direction.x
      val newY = obj.cellY + direction.y
      val transferEvent = Transfer(obj, time + dt, newX, newY, direction)

      // transfers out of the world boundaries may be scheduled before the corresponding wall collision
      // therefore we check for this explicitly
      if (newX <= grid.width && newY <= grid.height && newX > 0 && newY > 0) {
        obj.nextTransfer = Some(transferEvent)
        events.enqueue(transferEvent)
        println(s"enqueued transfer $transferEvent, dt=$dt")
      }
    }
  }


  private final class ObjectRecord(val obj: T, var cellX: Int, var cellY: Int) {
    var nextCollision: Option[Collision] = None
    var nextTransfer: Option[Transfer] = None

    override def toString: String = f"${this.hashCode & 0xffff}%x"
  }

  import language.implicitConversions

  private implicit def objectRecordIsT(objRec: ObjectRecord): T = objRec.obj

  private implicit object ObjectRecordHasPosition extends Positionable[ObjectRecord] {
    override def position(t: ObjectRecord): Vector2 = t.obj.pos
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
    newCellY: Int,
    direction: Direction
  ) extends Collision

}
