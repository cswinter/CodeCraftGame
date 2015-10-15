package cwinter.codecraft.core.replay

private[codecraft] class ConsoleReplayRecorder extends ReplayRecorder {
  val replay = new StringBuilder

  protected override def writeLine(string: String): Unit = {
    println(string)
    replay append (string + "\n")
  }
}
