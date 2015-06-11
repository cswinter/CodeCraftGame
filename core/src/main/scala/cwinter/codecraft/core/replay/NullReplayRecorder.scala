package cwinter.codecraft.core.replay

object NullReplayRecorder extends ReplayRecorder {
  override def writeLine(string: String): Unit = ()
}
