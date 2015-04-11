package cwinter.collisions


trait SpatialIndex[T] {
  implicit val evidence: CircleLike[T]

  def add(obj: T)
  def remove(obj: T)
  def updatePosition(obj: T)
  def updatePositions()
  def allInRange(circle: T): Iterable[T]
}
