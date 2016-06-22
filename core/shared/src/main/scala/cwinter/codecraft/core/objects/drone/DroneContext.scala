package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.api.Player
import cwinter.codecraft.core.{DroneWorldSimulator, WorldConfig, CommandRecorder}
import cwinter.codecraft.core.objects.IDGenerator
import cwinter.codecraft.core.replay.{NullReplayRecorder, ReplayRecorder}
import cwinter.codecraft.util.maths.RNG

import scala.util.Random


private[core] case class DroneContext(
  player: Player,
  worldConfig: WorldConfig,
  commandRecorder: Option[CommandRecorder],
  idGenerator: IDGenerator,
  rng: RNG,
  isLocallyComputed: Boolean,
  simulator: DroneWorldSimulator,
  replayRecorder: ReplayRecorder = NullReplayRecorder
) {
  def settings = simulator.settings
}

