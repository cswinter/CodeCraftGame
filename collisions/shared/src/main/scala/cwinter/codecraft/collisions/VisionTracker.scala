package cwinter.codecraft.collisions

import cwinter.codecraft.util.maths.Vector2

import scala.collection.mutable


trait VisionTracking {
  def position: Vector2
  def maxSpeed: Double

  private[collisions] def entersSight(obj: VisionTracking)
  private[collisions] def notInSight(obj: VisionTracking)
  private[collisions] var cell = (0, 0)
  private[collisions] def x = cell._1
  private[collisions] def y = cell._2
  private[collisions] def x_=(value: Int): Unit = cell = cell.copy(_1 = value)
  private[collisions] def y_=(value: Int): Unit = cell = cell.copy(_2 = value)
}

trait ActiveVisionTracking extends VisionTracking {
  private[this] var _inSight = Set.empty[VisionTracking]
  private[this] var events = Seq.empty[Event]


  private[collisions] def entersSight(other: VisionTracking): Unit = {
    if (other != this) {
      _inSight += other
      events :+= EnteredSightRadius(other)
    }
  }

  private[collisions] def notInSight(obj: VisionTracking): Unit = inSight -= obj

  private[collisions] def inSight_=(value: Set[VisionTracking]): Unit = {
    for (
      newObj <- value -- _inSight
      if newObj != this
    ) events :+= EnteredSightRadius(newObj)
    for (oldObj <- _inSight -- value)
      events :+= LeftSightRadius(oldObj)

    _inSight = value
  }
  private[collisions] def inSight: Set[VisionTracking] = _inSight

  private[collisions] def collectEvents(): Seq[Event] = {
    val tmp = events
    events = Seq.empty[Event]
    tmp
  }
}

trait PassiveVisionTracking extends VisionTracking {
  private[collisions] def entersSight(other: VisionTracking): Unit = ()
  private[collisions] def notInSight(other: VisionTracking): Unit = ()
}

sealed trait Event

case class EnteredSightRadius[T](obj: T) extends Event
case class LeftSightRadius[T](obj: T) extends Event

private[codecraft] final class VisionTracker[T <: VisionTracking](
  val xMin: Int,
  val xMax: Int,
  val yMin: Int,
  val yMax: Int,
  val radius: Int
) {
  require((xMax - xMin) % radius == 0)
  require((yMax - yMin) % radius == 0)
  require(yMax > yMin)
  require(xMax > xMin)

  val width = (xMax - xMin) / radius
  val height = (yMax - yMin) / radius
  val radius2 = radius * radius

  private[this] val trackedObjects = mutable.Set.empty[T with ActiveVisionTracking]
  private[this] val allObjects = mutable.Set.empty[T]
  private[this] val grid = new SquareGrid[T](xMin, xMax, yMin, yMax, radius)(VisionTrackingIsPositionable)


  def insertActive[S <: T with ActiveVisionTracking](obj: S): Unit = {
    trackedObjects += obj
    insert(obj)
  }

  def insertPassive[S <: T with PassiveVisionTracking](obj: S): Unit = insert(obj)

  private[this] def insert(obj: T): Unit = {
    allObjects += obj
    val cell@(x, y) = grid.computeCell(obj)
    obj.cell = cell

    for (other <- grid.nearbyObjects(x, y)) {
      if (contains(obj, other)) {
        obj.entersSight(other)
        other.entersSight(obj)
      }
    }

    grid.insert(obj, x, y)
  }

  def removePassive[S <: T with PassiveVisionTracking](obj: S): Unit = {
    for (e <- grid.nearbyObjects(obj.x, obj.y)) e.notInSight(obj)
    remove(obj)
  }

  def removeActive[S <: T with ActiveVisionTracking](obj: S): Unit = {
    for (e <- obj.inSight) e.notInSight(obj)
    trackedObjects -= obj
    remove(obj)
  }

  private[this] def remove(obj: T): Unit = {
    val (x, y) = obj.cell
    grid.remove(obj, x, y)
    allObjects -= obj
  }


  def updateAll() = {
    checkForCellTransfers()
    recomputeInSight()
  }

  def checkForCellTransfers(): Unit = {
    for (obj <- allObjects) {
      val (newX, newY) = grid.computeCell(obj)(VisionTrackingIsPositionable)
      val dx = newX - obj.x
      val dy = newY - obj.y
      if (dx < -1 || dx > 1 || dy < -1 || dy > 1) {
        grid.remove(obj, obj.cell)
        grid.insert(obj, newX, newY)
        obj.cell = (newX, newY)
      } else {
        if (obj.x != newX) {
          grid.xTransfer(obj, obj.cell, dx)
          obj.x = newX
        }
        if (obj.y != newY) {
          grid.yTransfer(obj, obj.cell, dy)
          obj.y = newY
        }
      }
    }
  }

  def recomputeInSight(): Unit = {
    for (elem <- trackedObjects) {
      val (x, y) = elem.cell

      elem.inSight = {
        for {
          other <- grid.nearbyObjects(x, y)
          if elem != other && contains(elem, other)
        } yield other
      }.toSet
    }
  }

  def getVisible[S <: T with ActiveVisionTracking](obj: S): Set[VisionTracking] = obj.inSight

  def collectEvents(): Iterable[(T, Seq[Event])] =
    for (obj <- trackedObjects)
      yield (obj, obj.collectEvents())

  private def contains(elem1: T, elem2: T): Boolean = {
    val diff = elem1.position - elem2.position
    (diff dot diff) <= radius2
  }

  implicit private object VisionTrackingIsPositionable extends Positionable[VisionTracking] {
    @inline
    override def position(e: VisionTracking): Vector2 = e.position
  }

}

