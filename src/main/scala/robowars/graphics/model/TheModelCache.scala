package robowars.graphics.model


object TheModelCache {
  private[this] var nModels = 0
  private[this] val cache = new collection.mutable.HashMap[Any, Model[_]]()


  def get[T](key: Any): Option[Model[T]] = {
    cache.get(key).asInstanceOf[Option[Model[T]]]
  }

  def getOrElseUpdate[T](key: Any)(generator: => Model[T]): Model[T] = {
    cache.getOrElseUpdate(key, {nModels += 1; generator}).asInstanceOf[Model[T]]
  }

  def CachedModelCount = nModels
}
