package cwinter.codecraft.testai.replicator

import cwinter.codecraft.core.api.Drone
import cwinter.codecraft.util.maths.Vector2


class BattleCoordinator {
  private[this] var _lastCapitalShipSighting: Option[Vector2] = None
  def lastCapitalShipSighting: Option[Vector2] = _lastCapitalShipSighting


  def foundCapitalShip(drone: Drone): Unit = {
    _lastCapitalShipSighting = Some(drone.position)
  }
}

