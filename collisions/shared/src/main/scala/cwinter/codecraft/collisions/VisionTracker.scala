package cwinter.codecraft.collisions

import Positionable.PositionableOps
import cwinter.codecraft.util.maths.Vector2

private[codecraft] class VisionTracker[T: Positionable](
  val xMin: Int,
  val xMax: Int,
  val yMin: Int,
  val yMax: Int,
  val radius: Int
) {
  assert((xMax - xMin) % radius == 0)
  assert((yMax - yMin) % radius == 0)
  assert(yMax > yMin)
  assert(xMax > xMin)

  val width = (xMax - xMin) / radius
  val height = (yMax - yMin) / radius

  private[this] var tracked = Set.empty[Element]
  private[this] val elementMap = collection.mutable.Map.empty[T, Element]
  private[this] val grid = new SquareGrid[Element](xMin, xMax, yMin, yMax, radius)(ElementIsPositionable)


  def insert(obj: T, generateEvents: Boolean = false): Unit = {
    val elem = new Element(obj, generateEvents)
    elementMap.put(obj, elem)
    if (generateEvents) tracked += elem
    val (x, y) = elem.cell

    for (other <- grid.nearbyObjects(x, y)) {
      if (contains(elem, other)) {
        elem.inSight += other
        other.inSight += elem
      }
    }

    grid.insert(elem, x, y)
  }


  def remove(obj: T): Unit = {
    val elem = elementMap(obj)
    for (e <- elem.inSight) e.inSight -= elem
    val (x, y) = elem.cell
    grid.remove(elem, x, y)
    elementMap -= obj
    if (elem.generateEvents) tracked -= elem
  }


  def updateAll() = {
    for (elem <- elementMap.values) {
      val (newX, newY) = grid.computeCell(elem)
      val dx = newX - elem.x
      val dy = newY - elem.y
      if (dx < -1 || dx > 1 || dy < -1 || dy > 1) {
        grid.remove(elem, elem.cell)
        grid.insert(elem, newX, newY)
        elem.cell = (newX, newY)
      } else {
        if (elem.x != newX) {
          grid.xTransfer(elem, elem.cell, dx)
          elem.x = newX
        }
        if (elem.y != newY) {
          grid.yTransfer(elem, elem.cell, dy)
          elem.y = newY
        }
      }
    }

    for (elem <- tracked) {
      val (x, y) = elem.cell

      elem.inSight = {
        for {
          other <- grid.nearbyObjects(x, y)
          if elem != other && contains(elem, other)
        } yield other
      }.toSet
    }
  }

  def getVisible(obj: T): Set[T] =
    elementMap(obj).inSight.map(_.elem)

  def collectEvents(): Iterable[(T, Seq[Event])] = {
    for (elem <- elementMap.values)
      yield (elem.elem, elem.collectEvents())
  }

  private def contains(elem1: Element, elem2: Element): Boolean = {
    val diff = elem1.position - elem2.position
    (diff dot diff) <= radius * radius
  }

  private final class Element(
    val elem: T,
    val generateEvents: Boolean
  ) {
    private[this] var _inSight = Set.empty[Element]
    var cell = grid.computeCell(this)
    private[this] var events = Seq.empty[Event]


    def entersSight(other: Element): Unit = {
      if (other != this) {
        _inSight += other
        if (generateEvents)
          events :+= EnteredSightRadius(other.elem)
      }
    }

    def inSight_=(value: Set[Element]): Unit = {
      if (generateEvents) {
        for (
          newObj <- value -- _inSight
          if newObj != this
        ) events :+= EnteredSightRadius(newObj.elem)
        for (oldObj <- _inSight -- value)
          events :+= LeftSightRadius(oldObj.elem)
      }

      _inSight = value
    }

    def collectEvents(): Seq[Event] = {
      val tmp = events
      events = Seq.empty[Event]
      tmp
    }

    def inSight: Set[Element] = _inSight

    def position = elem.position

    def x = cell._1
    def y = cell._2
    def x_=(value: Int): Unit =
      cell = cell.copy(_1 = value)
    def y_=(value: Int): Unit =
      cell = cell.copy(_2 = value)
  }

  implicit private object ElementIsPositionable extends Positionable[Element] {
    override def position(e: Element): Vector2 = e.elem.position
  }

  sealed trait Event

  case class EnteredSightRadius(obj: T) extends Event
  case class LeftSightRadius(obj: T) extends Event
}
