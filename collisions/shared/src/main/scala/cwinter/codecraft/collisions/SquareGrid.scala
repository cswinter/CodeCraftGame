package cwinter.codecraft.collisions

import Positionable.PositionableOps
import cwinter.codecraft.util.maths.{Vector2, Rectangle}

private[codecraft] class SquareGrid[T: Positionable](
  val xMin: Int,
  val xMaxA: Int,
  val yMin: Int,
  val yMaxA: Int,
  val cellWidth: Int
) {
  // need padding of 2 since missiles can spawn outside the area
  // TODO: set back to 1 if object radius is used in wall collisions
  final val Padding = 2

  val xMax = math.ceil((xMaxA - xMin).toDouble / cellWidth).toInt * cellWidth + xMin
  val yMax = math.ceil((yMaxA - yMin).toDouble / cellWidth).toInt * cellWidth + yMin

  val width = (xMax - xMin) / cellWidth
  val height = (yMax - yMin) / cellWidth

  //private[this]
  val cells = Array.fill(width + 2 * Padding, height + 2 * Padding)(Set.empty[T])


  def insert(obj: T): Unit = insert(obj, computeCell(obj))

  def insert(obj: T, cell: (Int, Int)): Unit = {
    assert(
      Rectangle(xMin, xMax, yMin, yMax).contains(obj.position),
      s"Cell does not match: ${obj.position} ([$xMin, $xMax], [$yMin, $yMax])"
    )
    val (x, y) = cell
    cells(x)(y) += obj
  }

  def insert(obj: T, x: Int, y: Int): Unit = {
    cells(x)(y) += obj
  }

  def remove(obj: T): Unit = remove(obj, computeCell(obj))

  def remove(obj: T, cell: (Int, Int)): Unit = {
    val (x, y) = cell
    remove(obj, x, y)
  }

  def remove(obj: T, x: Int, y: Int): Unit = {
    assert(cells(x)(y).contains(obj))
    cells(x)(y) -= obj
  }

  /**
   * Updates the x grid position of an object.
   * @param obj The object to be updated.
   * @param cell The old cell value.
   * @param direction The change in x value. The new cell will be `cell + (direction, 0)`.
   * @return Returns all objects which are now adjacent.
   */
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
    assert(direction == 1 || direction == -1, s"Parameter direction must be either 1 or -1. Actual value: $direction")
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
    // ALSO: we must floor the x/y components of the position BEFORE subtracting xMin/yMin, otherwise there might
    // be precision loss (e.g. adding denormalized to xMin will do nothing)
    val cellX = Padding + ((math.floor(elem.position.x) - xMin) / cellWidth).toInt
    val cellY = Padding + ((math.floor(elem.position.y) - yMin) / cellWidth).toInt
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
