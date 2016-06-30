package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.api.Player
import cwinter.codecraft.core.errors.Errors
import cwinter.codecraft.core.{DroneWorldSimulator, WorldConfig, CommandRecorder}
import cwinter.codecraft.core.objects.IDGenerator
import cwinter.codecraft.core.replay.{NullReplayRecorder, ReplayRecorder}
import cwinter.codecraft.graphics.engine.{Debug, TextModel}
import cwinter.codecraft.util.maths.RNG

import scala.util.Random


private[core] case class DroneContext(
  player: Player,
  worldConfig: WorldConfig,
  tickPeriod: Int,
  commandRecorder: Option[CommandRecorder],
  idGenerator: IDGenerator,
  rng: RNG,
  isLocallyComputed: Boolean,
  simulator: DroneWorldSimulator,
  replayRecorder: ReplayRecorder = NullReplayRecorder,
  debug: Debug,
  errors: Errors
) {
  def settings = simulator.settings
}

