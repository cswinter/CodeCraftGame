package cwinter.codecraft.core.objects


private[codecraft] class IDGenerator(group: Int) {
  private[this] var count: Int = -1
  // Integer.reverse would be better, this is an easy hack which is JavaScript compatible
  private[this] val prefix = group * 0x01000000

  def getAndIncrement(): Int = {
    count += 1
    count + prefix
  }
}
