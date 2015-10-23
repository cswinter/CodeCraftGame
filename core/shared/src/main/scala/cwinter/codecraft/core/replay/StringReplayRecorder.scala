package cwinter.codecraft.core.replay

private[core] class StringReplayRecorder extends ReplayRecorder {
  final val replay = new StringBuilder()


  protected override def writeLine(string: String): Unit = {
    replay.append(string + "\n")
  }

  override def replayString: Option[String] = Some(replay.toString)
}
