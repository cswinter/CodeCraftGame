package cwinter.codecraft.core.multiplayer

import cwinter.codecraft.core.game.SimulationContext
import cwinter.codecraft.core.objects.drone.{WorldStateMessage, MissileHit, DroneMovementMsg, DroneCommand}

import scala.concurrent.Future

private[core] trait RemoteServer {
  def receiveCommands()(implicit context: SimulationContext): Future[Seq[(Int, DroneCommand)]]
  def receiveWorldState(): Future[WorldStateMessage]
  def sendCommands(commands: Seq[(Int, DroneCommand)]): Unit
}
