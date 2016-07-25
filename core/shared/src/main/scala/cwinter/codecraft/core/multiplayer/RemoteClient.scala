package cwinter.codecraft.core.multiplayer

import cwinter.codecraft.core.api.Player
import cwinter.codecraft.core.game.SimulationContext
import cwinter.codecraft.core.objects.drone.{WorldStateMessage, MissileHit, DroneCommand, DroneMovementMsg}

import scala.concurrent.Future


private[core] trait RemoteClient {
  def waitForCommands()(implicit context: SimulationContext): Future[Seq[(Int, DroneCommand)]]
  def sendCommands(commands: Seq[(Int, DroneCommand)]): Unit
  def sendWorldState(worldStateMessage: WorldStateMessage): Unit
  def players: Set[Player]
}

