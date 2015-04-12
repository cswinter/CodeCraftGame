package cwinter.collisions

import cwinter.codinggame.maths.Vector2


trait CircleLike[T] {
  def position(t: T): Vector2
  def radius(t: T): Float
}


object CircleLike {
  final implicit class CircleLikeOps[T](obj: T)(implicit ev: CircleLike[T]) {
    @inline def position = ev.position(obj)
    @inline def radius = ev.radius(obj)
  }
}


case class Circle(position: Vector2, radius: Float)

object Circle {
  implicit object CircleIsCircleLike extends CircleLike[Circle] {
    def position(c: Circle) = c.position
    def radius(c: Circle) = c.radius
  }
}
