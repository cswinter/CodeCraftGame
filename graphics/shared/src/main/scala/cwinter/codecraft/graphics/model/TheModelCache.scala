package cwinter.codecraft.graphics.model


private[codecraft] object TheModelCache {
  private[this] var nModels = 0
  private[this] val cache = new java.util.HashMap[Any, Model[_]]()
  private[this] var _lastCachedModel = ""


  def get[T](key: Any): Option[Model[T]] = {
    cache.get(key).asInstanceOf[Option[Model[T]]]
  }

  def getOrElseUpdate[T](key: Any)(generator: => Model[T]): Model[T] = {
    val result = cache.get(key)
    if (result == null) {
      nModels += 1
      _lastCachedModel = key.toString
      val value = generator
      cache.put(key, value)
      value
    } else result
  }.asInstanceOf[Model[T]]

  def CachedModelCount = nModels

  def lastCachedModel = _lastCachedModel

  def clear(): Unit = {
    cache.clear()
    TheCompositeModelBuilderCache.clear()
  }
}
