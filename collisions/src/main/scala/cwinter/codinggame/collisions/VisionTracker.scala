package cwinter.codinggame.collisions

import Positionable.PositionableOps

class VisionTracker[T: Positionable](
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

  private[this] val elementMap = collection.mutable.Map.empty[T, Element]
  private[this] val cells = Array.fill(width + 2, height + 2)(Set.empty[Element])


  def insert(obj: T, generateEvents: Boolean = false): Unit = {
    val elem = new Element(obj, generateEvents)
    val (x, y) = elem.cell

    for (other <- nearbyElements(x, y)) {
      if (contains(elem, other)) {
        elem.inSight += other
        other.inSight += elem
      }
    }

    cells(x)(y) += elem
    elementMap.put(obj, elem)
  }


  def remove(obj: T): Unit = {
    val elem = elementMap(obj)
    for (e <- elem.inSight) e.inSight -= elem
    val (x, y) = elem.cell
    cells(x)(y) -= elem
    elementMap -= obj
  }


  def updateAll() = {
    for (elem <- elementMap.values) {
      val actualCell = computeCell(elem)
      if (elem.cell != actualCell) {
        changeCell(elem, actualCell)
      }
    }

    for (elem <- elementMap.values) {
      val (x, y) = elem.cell

      elem.inSight = {
        for {
          other <- nearbyElements(x, y)
          if elem != other && contains(elem, other)
        } yield other
      }.toSet
    }
  }

  private def changeCell(elem: Element, newCell: (Int, Int)): Unit = {
    val (x1, y1) = elem.cell
    cells(x1)(y1) -= elem

    val (x2, y2) = newCell
    cells(x2)(y2) += elem

    elem.cell = newCell
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

  private def computeCell(elem: Element): (Int, Int) = {
    val cellX = 1 + (elem.position.x.toInt - xMin) / radius
    val cellY = 1 + (elem.position.y.toInt - yMin) / radius
    (cellX, cellY)
  }

  private def nearbyElements(x: Int, y: Int): Iterator[Element] =
    cells(x - 1)(y + 1).iterator ++
      cells(x + 0)(y + 1).iterator ++
      cells(x + 1)(y + 1).iterator ++
      cells(x - 1)(y + 0).iterator ++
      cells(x + 0)(y + 0).iterator ++
      cells(x + 1)(y + 0).iterator ++
      cells(x - 1)(y - 1).iterator ++
      cells(x + 0)(y - 1).iterator ++
      cells(x + 1)(y - 1).iterator

  private final class Element(
    val elem: T,
    val generateEvents: Boolean
  ) {
    private[this] var _inSight = Set.empty[Element]
    var cell = computeCell(this)
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
        )
          events :+= EnteredSightRadius(newObj.elem)
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
  }

  sealed trait Event

  case class EnteredSightRadius(obj: T) extends Event
  case class LeftSightRadius(obj: T) extends Event
}
