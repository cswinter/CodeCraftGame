package cwinter.codecraft.graphics.model


import scala.language.higherKinds


private[graphics] trait TypedCache {
  type V[a]

  private[this] var nModels = 0
  private[this] val cache = new java.util.HashMap[Any, Any]()
  private[this] var _lastCachedModel = ""


  def get[T](key: Any): Option[V[T]] = {
    cache.get(key).asInstanceOf[Option[V[T]]]
  }

  def getOrElseUpdate[T](key: Any)(generator: => V[T]): V[T] = {
    val result = cache.get(key)
    if (result == null) {
      nModels += 1
      _lastCachedModel = key.toString
      val value = generator
      cache.put(key, value)
      value
    } else result
  }.asInstanceOf[V[T]]

  def CachedModelCount = nModels

  def lastCachedModel = _lastCachedModel

  def clear(): Unit = {
    cache.clear()
  }
}

