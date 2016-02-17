package cwinter.codecraft.graphics.model

private[codecraft] object TheCompositeModelBuilderCache {
  private[this] var nModels = 0
  private[this] val cache = new java.util.HashMap[Any, (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, _]])]()
  private[this] var _lastCachedModel = ""


  def getOrElseUpdate[T](key: Any)(
    generator: => (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, T]])
  ): (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, T]]) = {
    val result = cache.get(key)
    if (result == null) {
      nModels += 1
      _lastCachedModel = key.toString
      val value = generator
      cache.put(key, value)
      value
    } else result
  }.asInstanceOf[(Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, T]])]

  def CachedModelCount = nModels

  def lastCachedModel = _lastCachedModel

  def clear(): Unit = {
    cache.clear()
  }
}
