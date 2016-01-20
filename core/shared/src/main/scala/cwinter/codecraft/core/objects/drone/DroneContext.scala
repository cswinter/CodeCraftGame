package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.api.Player
import cwinter.codecraft.core.{WorldConfig, CommandRecorder}
import cwinter.codecraft.core.objects.IDGenerator
import cwinter.codecraft.core.replay.{NullReplayRecorder, ReplayRecorder}


case class DroneContext(
  player: Player,
  worldConfig: WorldConfig,
  commandRecorder: Option[CommandRecorder],
  idGenerator: IDGenerator,
  isLocallyComputed: Boolean,
  replayRecorder: ReplayRecorder = NullReplayRecorder
)

