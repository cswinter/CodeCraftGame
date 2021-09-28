package cwinter.codecraft.collisions

import cwinter.codecraft.util.maths.Vector2

import scala.collection.mutable

private[codecraft] trait VisionTracking {
  def position: Vector2
  def maxSpeed: Double

  private[collisions] var removed: Boolean = false
  private[collisions] var cell = (0, 0)

  private[collisions] def objectRemoved(obj: VisionTracking)
  private[collisions] def objectIsNearby(obj: VisionTracking)
  private[collisions] def x = cell._1
  private[collisions] def y = cell._2
  private[collisions] def x_=(value: Int): Unit = cell = cell.copy(_1 = value)
  private[collisions] def y_=(value: Int): Unit = cell = cell.copy(_2 = value)
}

private object VisionTracking {
  implicit object VisionTrackingIsPositionable extends Positionable[VisionTracking] {
    @inline
    override def position(e: VisionTracking): Vector2 = e.position
  }
}

private[collisions] case class NearbyObject(
  obj: VisionTracking,
  var isVisible: Boolean,
  var timeNextCheck: Int
)

private[collisions] object NearbyObject {
  implicit object NextCheckOrdering extends Ordering[NearbyObject] {
    override def compare(x: NearbyObject, y: NearbyObject): Int = y.timeNextCheck - x.timeNextCheck
  }
}

private[codecraft] trait ActiveVisionTracking extends VisionTracking {
  private[collisions] var nearbyObjects = Set.empty[VisionTracking]
  private val collisionQueue = new mutable.PriorityQueue[NearbyObject]()

  def objectEnteredVision(obj: VisionTracking): Unit
  def objectLeftVision(obj: VisionTracking): Unit
  def objectRemoved(obj: VisionTracking): Unit

  private[collisions] def objectIsNearby(obj: VisionTracking): Unit = {
    require(obj != this)
    if (!nearbyObjects.contains(obj)) {
      collisionQueue.enqueue(NearbyObject(obj, isVisible = false, 0))
      nearbyObjects += obj
    }
  }

  private[collisions] def recomputeVisible(time: Int, radius: Double): Unit = {
    while (collisionQueue.nonEmpty && collisionQueue.head.timeNextCheck <= time) {
      val no @ NearbyObject(obj, wasVisible, _) = collisionQueue.dequeue()
      if (!obj.removed) {
        val displacement = obj.position - position
        val distance = displacement.length
        val isVisible = distance <= radius
        if (wasVisible && !isVisible) objectLeftVision(obj)
        else if (!wasVisible && isVisible) objectEnteredVision(obj)

        if (math.abs(obj.x - x) < 2 && math.abs(obj.y - y) < 2) {
          val combinedSpeed = maxSpeed + obj.maxSpeed
          val visionDistance = math.abs(distance - radius)
          val timeNextCheck = time + (visionDistance / combinedSpeed).toInt + 1
          no.isVisible = isVisible
          no.timeNextCheck = timeNextCheck
          collisionQueue.enqueue(no)
        } else nearbyObjects -= obj
      }
    }
  }
}

private[codecraft] trait PassiveVisionTracking extends VisionTracking {
  private[collisions] def objectIsNearby(other: VisionTracking): Unit = ()
  private[collisions] def objectRemoved(other: VisionTracking): Unit = ()
}

private[codecraft] final class VisionTracker[T <: VisionTracking](
  val xMin: Int,
  val xMax: Int,
  val yMin: Int,
  val yMax: Int,
  val radius: Int
) {
  require((xMax - xMin) % radius == 0, f"xMax - xMin = $xMax - $xMin, but radius = $radius")
  require((yMax - yMin) % radius == 0, f"yMax - yMin = $yMax - $yMin, but radius = $radius")
  require(yMax > yMin, f"yMax = $yMax, but yMin = $yMin")
  require(xMax > xMin, f"xMax = $xMax, but xMin = $xMin")

  val width = (xMax - xMin) / radius
  val height = (yMax - yMin) / radius
  val radius2 = radius * radius

  private val movingObjects = mutable.Set.empty[T]
  private val trackingObjects = mutable.Set.empty[T with ActiveVisionTracking]
  private val allObjects = mutable.Set.empty[T]
  private val grid = new SquareGrid[T](xMin, xMax, yMin, yMax, radius)
  private var time = 0

  def insertActive[S <: T with ActiveVisionTracking](obj: S): Unit = {
    trackingObjects += obj
    insert(obj)
  }

  def insertPassive[S <: T with PassiveVisionTracking](obj: S): Unit = insert(obj)

  private[this] def insert(obj: T): Unit = {
    allObjects += obj
    if (obj.maxSpeed != 0) movingObjects += obj
    val cell @ (x, y) = grid.computeCell(obj)
    obj.cell = cell

    updateNearby(obj, grid.nearbyObjects(x, y))

    grid.insert(obj, x, y)
    obj.removed = false
  }

  def removePassive[S <: T with PassiveVisionTracking](obj: S): Unit = {
    remove(obj)
    for (nearby <- grid.nearbyObjects(obj.x, obj.y)) nearby.objectRemoved(obj)
  }

  def removeActive[S <: T with ActiveVisionTracking](obj: S): Unit = {
    trackingObjects -= obj
    remove(obj)
    for (nearby <- obj.nearbyObjects) nearby.objectRemoved(obj)
  }

  private[this] def remove(obj: T): Unit = {
    movingObjects -= obj
    allObjects -= obj
    grid.remove(obj, obj.x, obj.y)
    obj.removed = true
  }

  def updateAll(currTime: Int) = {
    this.time = currTime
    checkForCellTransfers()
    checkForCollisions()
  }

  def checkForCellTransfers(): Unit = {
    for (obj <- movingObjects) {
      val (newX, newY) = grid.computeCell(obj)
      val dx = newX - obj.x
      val dy = newY - obj.y
      if (dx < -1 || dx > 1 || dy < -1 || dy > 1) {
        grid.remove(obj, obj.cell)
        obj.cell = (newX, newY)
        updateNearby(obj, grid.nearbyObjects(newX, newY))
        grid.insert(obj, newX, newY)
      } else {
        if (obj.x != newX) {
          updateNearby(obj, grid.xTransfer(obj, obj.cell, dx))
          obj.x = newX
        }
        if (obj.y != newY) {
          updateNearby(obj, grid.yTransfer(obj, obj.cell, dy))
          obj.y = newY
        }
      }
    }
  }

  private def checkForCollisions(): Unit = {
    trackingObjects.foreach(_.recomputeVisible(time, radius))
    time += 1
  }

  private def updateNearby(obj: T, nearby: Iterator[T]): Unit = {
    for (obj2 <- nearby) {
      obj.objectIsNearby(obj2)
      obj2.objectIsNearby(obj)
    }
  }
}
