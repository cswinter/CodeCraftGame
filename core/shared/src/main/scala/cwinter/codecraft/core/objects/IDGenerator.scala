package cwinter.codecraft.core.objects


private[codecraft] class IDGenerator(group: Int) {
  import IDGenerator._
  private[this] var count: Int = -1
  assert(group <= MaxFactions)

  def getAndIncrement(): Int = {
    count += 1
    count * MaxFactions + group
  }
}

private[codecraft] object IDGenerator {
  final val MaxFactions = 4
}

