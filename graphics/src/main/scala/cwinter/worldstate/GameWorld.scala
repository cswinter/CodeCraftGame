package cwinter.worldstate


trait GameWorld {
  def worldState: Iterable[WorldObjectDescriptor]
  def update(): Unit
}
