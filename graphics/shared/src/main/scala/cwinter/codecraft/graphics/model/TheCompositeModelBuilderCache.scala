package cwinter.codecraft.graphics.model

private[codecraft] object TheCompositeModelBuilderCache {
  private[this] var nModels = 0
  private[this] val cache = new collection.mutable.HashMap[Any, (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, _]])]()
  private[this] var _lastCachedModel = ""


  def get[T](key: Any): Option[(Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, T]])] = {
    cache.get(key).asInstanceOf[Option[(Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, T]])]]
  }

  def getOrElseUpdate[T](key: Any)(
    generator: => (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, T]])
  ): (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, T]]) = {
    cache.getOrElseUpdate(key, {
      nModels += 1
      _lastCachedModel = key.toString
      generator
    }).asInstanceOf[(Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, T]])]
  }

  def CachedModelCount = nModels

  def lastCachedModel = _lastCachedModel

  def clear(): Unit = {
    cache.clear()
  }
}
