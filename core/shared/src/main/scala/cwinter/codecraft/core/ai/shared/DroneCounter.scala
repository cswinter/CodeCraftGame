package cwinter.codecraft.core.ai.shared

class DroneCounter {
  private[this] var counts = Map.empty[Symbol, Int]

  def apply(name: Symbol): Int = {
    counts.getOrElse(name, 0)
  }

  def increment(name: Symbol): Unit = {
    counts = counts.updated(name, this(name) + 1)
  }

  def decrement(name: Symbol): Unit = {
    counts = counts.updated(name, this(name) - 1)
  }
}
