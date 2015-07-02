package cwinter.codecraft.collisions

import cwinter.codecraft.util.maths.Vector2

trait Positionable[-T] {
  def position(t: T): Vector2
}

object Positionable {
  final implicit class PositionableOps[T](t: T)(implicit ev: Positionable[T]) {
    @inline def position = ev.position(t)
  }

  implicit object Vector2IsPositionable extends Positionable[Vector2] {
    override def position(t: Vector2): Vector2 = t
  }
}
