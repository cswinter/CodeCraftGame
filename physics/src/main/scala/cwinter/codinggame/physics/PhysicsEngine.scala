package cwinter.codinggame.physics

import cwinter.codinggame.maths.{Vector2, Rectangle}
import cwinter.collisions.{Positionable, SquareGrid}
import robowars.graphics.engine.Debug
import robowars.worldstate.{WorldObject, Rectangle => DrawRectangle}

/**
 * Simulates movement of colliding particles.
 *
 * @param worldBoundaries All particles are confined inside this rectangle.
 * @param maxRadius Bound on the radii of the particles.
 * @tparam T Type of the particles.
 *
 * A good summary of the type of algorithm used is found in the first section of
 * "Algorithms for Particle-Field Simulations with Collisions".
 * (link: http://homepages.warwick.ac.uk/~masdr/JOURNALPUBS/stuart48.pdf)
 *
 * ## Main datastructures
 * 1. `objects`: set of particles
 * 2. `grid`: partitions the world into square cells. each object is assigned to one cell.
 * 3. `events`: priority queue of collisions that (may) happen in the future
 *
 * ## A note on nomenclature
 * 1. Distinguish between potential collisions (which have been calculated, but may become
 * invalid because of other collisions), and true collisions (the "ground truth" values
 * of what collision will actually take place.
 * 2. A->B (A records a potential collision with B)
 * 3. Write A<->B for A->B & B->A
 *
 * ## Key invariances:
 * - if obj.nextCollision == Some(collision), then the time of the next true collision
 *    of obj is greater or equal to collision.time
 * - if `collision` is the next event in the queue, and for all objects `obji` that are involved
 *     `obji.nextCollision` == `Some(collision)`, then `collision` is a true collision
 *
 * ## Gotchas/edge cases/potential bugs:
 * - events in the queue might not be valid anymore
 * - it is possible that the next (potential) collision of `obj1` is with `obj2`, but the
 *    next collision of `obj2` is with `obj3`
 * - the same event might be in the queue twice in a row:
 *    - recomputed wall transfer (this type of duplicated can be avoided)
 *    - objects A and B have computed a different time for their collision, this is only
 *       reconciled after reentering the collision once
 *       (B->C at t=9. A->B at t=10. B->C becomes invalid, B->A at t=11.
 *        A->B is processed, computes A<->B at t=10. A->B is processed again, this time A and B agree.
 *        (TODO: WHAT IF THIS RECOMPUTATION NOW GIVES A<->B at t=9.9??? VERY UNLIKELY TO HAPPEN,
 *          SINCE TIME OF PREVIOUS EVENT GENERALLY << 10, BUT IF ALREADY time >9.9, WE GET ASSERTION ERROR,
 *          NEGATIVE MOVEMENTS)
 * - numerical issues:
 *    - after transfer, an object might still be (just) inside the previous cell
 *    - at and shortly after a collision, two objects may overlap
 *    - the time for a collision may change when computed at a later time, or by a different object
 *
 * ## Proof of progress
 */
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


    while (events.size > 0) {
      val collision = events.dequeue()

      collision match {
        case ObjectWallCollision(obj, t) =>
          if (obj.nextCollision == Some(collision)) {
            time = t
            obj.updatePosition(t)

            obj.handleWallCollision(worldBoundaries)
            updateNextCollision(obj, grid.nearbyObjects(obj.cellX, obj.cellY))

            updateTransfer(obj)
          }
        case ObjectObjectCollision(obj1, obj2, t) =>
          if (obj1.nextCollision == Some(collision)) {
            if (obj2.nextCollision == Some(collision)) {
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
      obj.nextCollision = None
    }
    val collisions = computeCollisions(obj, nearbyObjects, pathUnchanged)
    if (collisions.nonEmpty) {
      val nextCol = collisions.minBy(_.time)
      if (obj.nextCollision.map(_.time <= nextCol.time) == Some(true)) return

      val nextColOpt = Some(nextCol)
      obj.nextCollision = nextColOpt
      events.enqueue(nextCol)
      nextCol match {
        case ObjectObjectCollision(_, obj2, t) =>
          if (obj2.nextCollision.map(_.time >= t) != Some(false)) {
            obj2.nextCollision = nextColOpt
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
