package cwinter.collisions

import CircleLike.CircleLikeOps
import cwinter.codinggame.maths.Vector2


abstract class QuadTree[T] extends SpatialIndex[T] {
  val midpoint: Vector2
  val width: Float

  def size: Int
}

object QuadTree {
  def apply[T: CircleLike](width: Float): QuadTree[T] =
    new QuadTreeNode[T](Vector2(0, 0), width)


  private case class QuadTreeLeaf[T](
    parent: QuadTreeNode[T],
    midpoint: Vector2,
    width: Float
  )(implicit val evidence: CircleLike[T]) extends QuadTree[T] {
    private[this] var elems = Set.empty[T]

    def allInRange(circle: T): Iterable[T] =
      elems.filter(elem => (elem.position dot circle.position) <= circle.radius)

    def remove(elem: T): Unit =
      elems -= elem

    def updatePositions() = Unit

    def updatePosition(elem: T) = Unit

    def add(elem: T): Unit =
      elems += elem

    def size: Int = elems.size
  }


  private class QuadTreeNode[T](
    val midpoint: Vector2,
    val width: Float
  )(implicit val evidence: CircleLike[T]) extends QuadTree[T] {

    private[this] var _size = 0
    private[this] var w2 = width / 2
    private[this] var topRight: QuadTree[T] = QuadTreeLeaf(this, midpoint + Vector2(w2, w2), w2)
    private[this] var topLeft: QuadTree[T] = QuadTreeLeaf(this, midpoint + Vector2(-w2, w2), w2)
    private[this] var bottomLeft: QuadTree[T] = QuadTreeLeaf(this, midpoint + Vector2(-w2, -w2), w2)
    private[this] var bottomRight: QuadTree[T] = QuadTreeLeaf(this, midpoint + Vector2(w2, -w2), w2)


    def allInRange(circle: T): Iterable[T] = ???

    def remove(elem: T): Unit = {
      _size -= 1
      getQuadrant(elem.position).remove(elem)
    }

    def add(elem: T): Unit = {
      _size += 1
      getQuadrant(elem.position).add(elem)
    }

    def updatePosition(obj: T): Unit = ???

    def updatePositions(): Unit =
      for (c <- children) c.updatePositions()

    def size = _size

    private def children = Seq(topRight, topLeft, bottomLeft, bottomRight)

    @inline private def getQuadrant(pos: Vector2): QuadTree[T] = {
      (pos.x > midpoint.x, pos.y > midpoint.y) match {
        case (true, true) => topRight
        case (true, false) => bottomRight
        case (false, true) => topLeft
        case (false, false) => bottomLeft
      }
    }
  }
}
