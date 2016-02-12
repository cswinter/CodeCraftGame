package cwinter.codecraft.core.ai.shared

class DroneCounter {
  private[this] var counts = Map.empty[Class[_], Int]

  def apply[T](clazz: Class[T]): Int = 0

  def increment[T](clazz: Class[T]): Unit =
    counts = counts.updated(clazz, counts.getOrElse(clazz, 0) + 1)

  def decrement[T](clazz: Class[T]): Unit =
    counts = counts.updated(clazz, counts.getOrElse(clazz, 0) - 1)
}

