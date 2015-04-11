package cwinter.collisions


case class Vector2(x: Float, y: Float) {
  def dot(rhs: Vector2): Float = x * rhs.x + y * rhs.y
  def +(rhs: Vector2): Vector2 = Vector2(x + rhs.x, y + rhs.y)
  def -(rhs: Vector2): Vector2 = Vector2(x - rhs.x, y - rhs.y)
}
