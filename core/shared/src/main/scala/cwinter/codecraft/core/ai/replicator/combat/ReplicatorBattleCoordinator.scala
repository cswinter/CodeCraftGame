package cwinter.codecraft.core.ai.replicator.combat

import cwinter.codecraft.core.ai.replicator.ReplicatorController
import cwinter.codecraft.core.ai.shared.BattleCoordinator
import cwinter.codecraft.core.api.Drone


class ReplicatorBattleCoordinator extends BattleCoordinator[ReplicatorCommand] {
  private[this] var assisting = Map.empty[ReplicatorController, Assist]
  private[this] var guarding = Map.empty[ReplicatorController, Guard]
  private[this] var enemyForces = Set.empty[Drone]
  private[this] var _enemyStrength = 0.0
  def enemyStrength = _enemyStrength
  final val SoldierStrength = 2
  addMission(ScoutingMission)


  override def update(): Unit = {
    super.update()

    enemyForces = enemyForces.filter(!_.isDead)
    _enemyStrength = enemyForces.foldLeft(0.0)(
      (acc, d) => acc + math.sqrt(d.spec.maxHitpoints * d.spec.missileBatteries)
    ) / SoldierStrength

    assisting = assisting.filter(!_._2.hasExpired)
    guarding = guarding.filter(!_._2.hasExpired)
  }

  def requestAssistance(drone: ReplicatorController): Unit = {
    if (assisting.contains(drone)) assisting(drone).refresh()
    else {
      val (priority, radius) =
        if (drone.spec.constructors > 0) (15, 950) else (5, 750)
      val assistMission = new Assist(drone, priority, drone.strengthDelta - 1, radius)
      assisting += drone -> assistMission
      addMission(assistMission)
    }
  }

  def requestGuards(drone: ReplicatorController, amount: Int): Unit = {
    if (guarding.contains(drone)) guarding(drone).refresh(amount)
    else {
      val guardMission = new Guard(drone, amount)
      guarding += drone -> guardMission
      addMission(guardMission)
    }
  }

  override def foundCapitalShip(drone: Drone): Unit = {
    if (!enemyCapitalShips.contains(drone)) {
      val newMission = new AssaultCapitalShip(drone)
      addMission(newMission)
    }
    super.foundCapitalShip(drone)
  }

  def foundArmedEnemy(drone: Drone): Unit = {
    enemyForces += drone
  }
}

