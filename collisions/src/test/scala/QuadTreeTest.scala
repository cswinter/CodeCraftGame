import org.scalatest.FlatSpec
import cwinter.collisions._


class QuadTreeTest extends FlatSpec {
  "An empty quadtree" should "return an empty list" in {
    val emptyQuadtree = QuadTree[Circle](100)
    assert(emptyQuadtree.allInRange(Circle(Vector2(0, 0), 10)).isEmpty)
  }
}
