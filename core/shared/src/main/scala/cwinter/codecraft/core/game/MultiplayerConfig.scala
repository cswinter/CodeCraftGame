package cwinter.codecraft.core.game

import cwinter.codecraft.core.api.Player
import cwinter.codecraft.core.multiplayer.{RemoteClient, RemoteServer}

import scala.concurrent.duration._
import scala.concurrent.duration.Duration

sealed trait MultiplayerConfig {
  def isMultiplayerGame: Boolean
  def commandRecorder: CommandRecorder
  def isLocalPlayer(player: Player): Boolean
  def timeoutSecs: Duration = 30.seconds
}

private[core] object SingleplayerConfig extends MultiplayerConfig {
  def isMultiplayerGame = false
  def commandRecorder = throw new Exception("Trying to call commandRecorder on SingleplayerConfig.")
  def isLocalPlayer(player: Player): Boolean = true
}

private[core] case class MultiplayerClientConfig(
  localPlayers: Set[Player],
  remotePlayers: Set[Player],
  server: RemoteServer
) extends MultiplayerConfig {
  def isMultiplayerGame = true
  def isLocalPlayer(player: Player): Boolean = localPlayers.contains(player)
  val commandRecorder = new CommandRecorder
  override def timeoutSecs: Duration = 24.hours
}

private[core] case class AuthoritativeServerConfig(
  localPlayers: Set[Player],
  remotePlayers: Set[Player],
  clients: Set[RemoteClient],
  updateCompleted: DroneWorldSimulator => Unit,
  onTimeout: DroneWorldSimulator => Unit,
  playerTimeoutSecs: Duration
) extends MultiplayerConfig {
  def isMultiplayerGame = true
  def isLocalPlayer(player: Player): Boolean = localPlayers.contains(player)
  override def timeoutSecs: Duration = playerTimeoutSecs
  val commandRecorder = new CommandRecorder

}
