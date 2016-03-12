package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.api.Player
import cwinter.codecraft.core.{DroneWorldSimulator, WorldConfig, CommandRecorder}
import cwinter.codecraft.core.objects.IDGenerator
import cwinter.codecraft.core.replay.{NullReplayRecorder, ReplayRecorder}

import scala.util.Random


private[core] case class DroneContext(
  player: Player,
  worldConfig: WorldConfig,
  commandRecorder: Option[CommandRecorder],
  idGenerator: IDGenerator,
  rng: Random,
  isLocallyComputed: Boolean,
  simulator: DroneWorldSimulator,
  replayRecorder: ReplayRecorder = NullReplayRecorder
)

