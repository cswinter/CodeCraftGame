package cwinter.codecraft.core.replay

private[core] object ReplayFactory {
  def replayRecorder: ReplayRecorder =
    new FileReplayRecorder(System.getProperty("user.home") + "/.codecraft/replays")
}

