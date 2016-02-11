package cwinter.codecraft.core.ai.destroyer

import cwinter.codecraft.core.ai.shared.{Mission, BattleCoordinator, BasicHarvestCoordinator, SharedContext}
import cwinter.codecraft.core.api.Drone
import cwinter.codecraft.util.maths.Rectangle


class DestroyerContext extends SharedContext[DestroyerCommand] {
  val harvestCoordinator = new BasicHarvestCoordinator
  override val battleCoordinator = new DestroyerBattleCoordinator
  private var _mothership: Mothership = null
  def mothership: Mothership = _mothership


  def initialise(worldSize: Rectangle, mothership: Mothership): Unit = {
    initialise(worldSize)
    _mothership = mothership
  }
}

class DestroyerBattleCoordinator extends BattleCoordinator[DestroyerCommand] {
  override def foundCapitalShip(drone: Drone): Unit = {
    if (!enemyCapitalShips.contains(drone)) {
      val newMission = new AssaultCapitalShip(drone)
      addMission(newMission)
    }
    super.foundCapitalShip(drone)
  }
}


sealed trait DestroyerCommand

case class Attack(enemy: Drone, notFound: () => Unit) extends DestroyerCommand

class AssaultCapitalShip(enemy: Drone) extends Mission[DestroyerCommand] {
  val minRequired =
    math.ceil(math.sqrt(enemy.spec.maxHitpoints * enemy.spec.missileBatteries / (22 * 2.0))).toInt
  val maxRequired = (minRequired * 1.5).toInt
  val priority = 10 - enemy.spec.missileBatteries + enemy.spec.constructors


  def locationPreference = Some(enemy.lastKnownPosition)

  def missionInstructions: DestroyerCommand = Attack(enemy, notFound)

  def notFound(): Unit = {
    deactivate()
  }

  override def update(): Unit = {
    if (isDeactivated && enemy.isVisible) reactivate()
  }

  def hasExpired = enemy.isDead
}

