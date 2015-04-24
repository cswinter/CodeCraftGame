package cwinter.collisions

import Positionable.PositionableOps
import cwinter.codinggame.maths.Rectangle

class SquareGrid[T: Positionable](
  val xMin: Int,
  val xMax: Int,
  val yMin: Int,
  val yMax: Int,
  val cellWidth: Int
) {
  final val Padding = 1
  assert((xMax - xMin) % cellWidth == 0, s"(xMax - xMin) % cellWidth = ${(xMax - xMin) % cellWidth}")
  assert((yMax - yMin) % cellWidth == 0)

  val width = (xMax - xMin) / cellWidth
  val height = (yMax - yMin) / cellWidth

  //private[this]
  val cells = Array.fill(width + 2 * Padding, height + 2 * Padding)(Set.empty[T])


  def insert(obj: T): Unit = insert(obj, computeCell(obj))

  def insert(obj: T, cell: (Int, Int)): Unit = {
    val (x, y) = cell
    cells(x)(y) += obj
  }

  def insert(obj: T, x: Int, y: Int): Unit = {
    cells(x)(y) += obj
  }

  def remove(obj: T): Unit = remove(obj, computeCell(obj))

  def remove(obj: T, cell: (Int, Int)): Unit = {
    val (x, y) = cell
    assert(cells(x)(y).contains(obj))
    cells(x)(y) -= obj
  }

  def xTransfer(obj: T, cell: (Int, Int), direction: Int): Iterator[T] = {
    assert(direction == 1 || direction == -1)
    remove(obj, cell)

    val x = cell._1 + direction
    val y = cell._2
    insert(obj, (x, y))

    cells(x + direction)(y - 1).iterator ++
      cells(x + direction)(y).iterator ++
      cells(x + direction)(y + 1).iterator
  }


  def yTransfer(obj: T, cell: (Int, Int), direction: Int): Iterator[T] = {
    assert(direction == 1 || direction == -1)
    remove(obj, cell)

    val x = cell._1
    val y = cell._2 + direction
    insert(obj, (x, y))

    cells(x - 1)(y + direction).iterator ++
      cells(x)(y + direction).iterator ++
      cells(x + 1)(y + direction).iterator
  }


  def computeCell[T2: Positionable](elem: T2): (Int, Int) = {
    val cellX = Padding + (elem.position.x.toInt - xMin) / cellWidth
    val cellY = Padding + (elem.position.y.toInt - yMin) / cellWidth
    (cellX, cellY)
  }


  def cellBounds(x: Int, y: Int): Rectangle = {
    Rectangle(
      cellWidth * (x - Padding) + xMin, cellWidth * (x - Padding + 1) + xMin,
      cellWidth * (y - Padding) + yMin, cellWidth * (y - Padding + 1) + yMin)
  }


  def nearbyObjects(x: Int, y: Int): Iterator[T] =
    cells(x - 1)(y + 1).iterator ++
      cells(x + 0)(y + 1).iterator ++
      cells(x + 1)(y + 1).iterator ++
      cells(x - 1)(y + 0).iterator ++
      cells(x + 0)(y + 0).iterator ++
      cells(x + 1)(y + 0).iterator ++
      cells(x - 1)(y - 1).iterator ++
      cells(x + 0)(y - 1).iterator ++
      cells(x + 1)(y - 1).iterator

  def reducedNearbyObjects(x: Int, y: Int): Iterator[T] =
    cells(x - 1)(y + 1).iterator ++
      cells(x + 0)(y + 1).iterator ++
      cells(x + 1)(y + 1).iterator ++
      cells(x - 1)(y + 0).iterator ++
      cells(x)(y).iterator
}
