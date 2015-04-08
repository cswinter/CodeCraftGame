package robowars.worldstate


trait GameWorld {
  def worldState: Iterable[WorldObject]
  def update(): Unit
}
