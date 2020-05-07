package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.api.Player
import cwinter.codecraft.core.errors.Errors
import cwinter.codecraft.core.game.{CommandRecorder, DroneWorldSimulator, GameConfig, SpecialRules}
import cwinter.codecraft.core.objects.IDGenerator
import cwinter.codecraft.core.replay.{NullReplayRecorder, ReplayRecorder}
import cwinter.codecraft.graphics.engine.Debug
import cwinter.codecraft.util.maths.{RNG, Rectangle}

private[core] case class DroneContext(
  player: Player,
  worldSize: Rectangle,
  tickPeriod: Int,
  commandRecorder: Option[CommandRecorder],
  debugLog: Option[DroneDebugLog],
  idGenerator: IDGenerator,
  rng: RNG,
  isLocallyComputed: Boolean,
  isMultiplayer: Boolean,
  simulator: DroneWorldSimulator,
  replayRecorder: ReplayRecorder = NullReplayRecorder,
  debug: Debug,
  errors: Errors,
  specialRules: SpecialRules
) {
  def settings = simulator.settings
  def isAuthoritativeServer: Boolean = isMultiplayer && isLocallyComputed
  def isMultiplayerClient: Boolean = isMultiplayer && !isLocallyComputed

  var missileHits = List.empty[MissileHit]
  var mineralHarvests = List.empty[MineralHarvest]
}
