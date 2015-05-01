package cwinter.collisions

import Positionable.PositionableOps
import cwinter.codinggame.util.maths.{Vector2, Rectangle}

class SquareGrid[T: Positionable](
  val xMin: Int,
  val xMaxA: Int,
  val yMin: Int,
  val yMaxA: Int,
  val cellWidth: Int
) {
  final val Padding = 2 // need padding of 2 since missiles can spawn outside the area TODO: set back to 1 if object radius is used in wall collisions

  val xMax = math.ceil((xMaxA - xMin).toDouble / cellWidth).toInt * cellWidth + xMin
  val yMax = math.ceil((yMaxA - yMin).toDouble / cellWidth).toInt * cellWidth + yMin
  println(s"$xMaxA, $xMax, $yMaxA, $yMax")

  val width = (xMax - xMin) / cellWidth
  val height = (yMax - yMin) / cellWidth

  //private[this]
  val cells = Array.fill(width + 2 * Padding, height + 2 * Padding)(Set.empty[T])


  def insert(obj: T): Unit = insert(obj, computeCell(obj))

  def insert(obj: T, cell: (Int, Int)): Unit = {
    assert(Rectangle(xMin, xMax, yMin, yMax).contains(obj.position))
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
    // if an object spawns outside of the bounds, expression can be negative, so we need floor
    // toInt will round UP for negative values
    val cellX = Padding + math.floor((elem.position.x - xMin) / cellWidth).toInt
    val cellY = Padding + math.floor((elem.position.y - yMin) / cellWidth).toInt
    assert({
      val bounds = cellBounds(cellX, cellY)
      val Vector2(x, y) = elem.position
      x <= bounds.xMax && x >= bounds.xMin && y <= bounds.yMax && y >= bounds.yMin
    }, s"invalid cell: ${(cellX, cellY)} with bounds ${cellBounds(cellX, cellY)} for ${elem.position}")
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


  def minX: Int = Padding - 1
  def minY: Int = Padding - 1
  def maxX: Int = width + Padding
  def maxY: Int = height + Padding
}
