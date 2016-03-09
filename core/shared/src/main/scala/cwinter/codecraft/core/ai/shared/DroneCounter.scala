package cwinter.codecraft.core.ai.shared

private[codecraft] class DroneCounter {
  private[this] var counts = Map.empty[Class[_], Int]

  def apply[T](clazz: Class[T]): Int = counts.getOrElse(clazz, 0)

  def increment[T](clazz: Class[T]): Unit =
    counts = counts.updated(clazz, this(clazz) + 1)

  def decrement[T](clazz: Class[T]): Unit =
    counts = counts.updated(clazz, this(clazz) - 1)
}

