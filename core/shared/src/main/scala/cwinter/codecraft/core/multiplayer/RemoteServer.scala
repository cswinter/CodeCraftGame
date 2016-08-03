package cwinter.codecraft.core.multiplayer

import cwinter.codecraft.core.game.SimulationContext
import cwinter.codecraft.core.objects.drone._

import scala.concurrent.Future

private[core] trait RemoteServer {
  type Result[T] = Future[Either[T, GameClosed.Reason]]
  def receiveCommands()(implicit context: SimulationContext): Result[Seq[(Int, DroneCommand)]]
  def receiveWorldState(): Result[WorldStateMessage]
  def sendCommands(commands: Seq[(Int, DroneCommand)]): Unit
  def gameClosed: Option[GameClosed.Reason]
}

