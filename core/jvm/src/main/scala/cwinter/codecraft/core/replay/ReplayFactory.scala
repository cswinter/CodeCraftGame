package cwinter.codecraft.core.replay

object ReplayFactory {
  def replayRecorder: ReplayRecorder =
    new FileReplayRecorder(System.getProperty("user.home") + "/.codecraft/replays")
}

