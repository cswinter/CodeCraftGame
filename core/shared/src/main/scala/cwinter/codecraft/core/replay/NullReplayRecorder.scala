package cwinter.codecraft.core.replay

private[codecraft] object NullReplayRecorder extends ReplayRecorder {
  override def writeLine(string: String): Unit = ()
}
