package cwinter.codecraft.core.replay

private[core] object ReplayFactory {
  def replayRecorder: ReplayRecorder = NullReplayRecorder
}
