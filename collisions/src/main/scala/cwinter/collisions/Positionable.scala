package cwinter.collisions

import cwinter.codinggame.maths.Vector2

trait Positionable[T] {
  def position(t: T): Vector2
}

object Positionable {
  final implicit class PositionableOps[T](t: T)(implicit ev: Positionable[T]) {
    @inline def position = ev.position(t)
  }
}
