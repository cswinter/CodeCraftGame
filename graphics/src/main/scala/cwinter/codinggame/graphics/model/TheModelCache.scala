package cwinter.codinggame.graphics.model

object TheModelCache {
  private[this] var nModels = 0
  private[this] val cache = new collection.mutable.HashMap[Any, Model[_]]()
  private[this] var _lastCachedModel = ""


  def get[T](key: Any): Option[Model[T]] = {
    cache.get(key).asInstanceOf[Option[Model[T]]]
  }

  def getOrElseUpdate[T](key: Any)(generator: => Model[T]): Model[T] = {
    cache.getOrElseUpdate(key, {
      nModels += 1
      _lastCachedModel = key.toString()
      generator
    }).asInstanceOf[Model[T]]
  }

  def CachedModelCount = nModels

  def lastCachedModel = _lastCachedModel
}
