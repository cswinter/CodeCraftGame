package robowars.worldstate


trait GameWorld {
  def worldState: Iterable[WorldObject]
}
