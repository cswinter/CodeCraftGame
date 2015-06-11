package cwinter.codecraft.collisions

import cwinter.codecraft.util.maths.Vector2
import org.scalatest.FlatSpec


class SquareGridTest extends FlatSpec {
  "A square grid" should "assign objects to the correct cell" in {

    val squareGrid = new SquareGrid[Vector2](-2000, 2000, -1000, 1000, 1)

    def checkCell(point: Vector2): Unit = {
      val cell = squareGrid.computeCell(point)
      val bounds = squareGrid.cellBounds(cell._1, cell._2)
      assert(bounds.contains(point))
    }

    checkCell(new Vector2(0, 0))
    checkCell(new Vector2(0, -123))
    checkCell(new Vector2(-2000, 1000))
    checkCell(new Vector2(0.9999999, -1))
    checkCell(new Vector2(1.0000001, 999.999999))
  }
}
