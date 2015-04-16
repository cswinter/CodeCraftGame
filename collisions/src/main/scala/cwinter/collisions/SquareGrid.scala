package cwinter.collisions

import Positionable.PositionableOps

class SquareGrid[T: Positionable](
  val xMin: Int,
  val xMax: Int,
  val yMin: Int,
  val yMax: Int,
  val cellWidth: Int
) {
  assert((xMax - xMin) % cellWidth == 0)
  assert((yMax - yMin) % cellWidth == 0)

  val width = (xMax - xMin) / cellWidth
  val height = (yMax - yMin) / cellWidth

  private[this] val cells = Array.fill(width + 2, height + 2)(Set.empty[T])


  def insert(obj: T): Unit = insert(obj, computeCell(obj))

  def insert(obj: T, cell: (Int, Int)): Unit = {
    val (x, y) = cell
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
      cells(x + direction)(y + 2).iterator
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


  def computeCell(elem: T): (Int, Int) = {
    val cellX = 1 + (elem.position.x.toInt - xMin) / cellWidth
    val cellY = 1 + (elem.position.y.toInt - yMin) / cellWidth
    (cellX, cellY)
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
