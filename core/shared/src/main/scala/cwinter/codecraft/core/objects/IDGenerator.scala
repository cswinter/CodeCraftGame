package cwinter.codecraft.core.objects


class IDGenerator(group: Int) {
  private[this] var count: Int = -1
  private[this] val prefix = Integer.reverse(group)

  def getAndIncrement(): Int = {
    count += 1
    count + prefix
  }
}
