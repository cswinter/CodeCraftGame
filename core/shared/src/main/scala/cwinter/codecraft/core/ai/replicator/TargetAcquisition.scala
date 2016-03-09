package cwinter.codecraft.core.ai.replicator

import cwinter.codecraft.core.api.Drone


private[codecraft] trait TargetAcquisition {
  self: ReplicatorController =>

  val normalizedStrength: Double

  private[this] var _target = Option.empty[Drone]
  private[this] var _attack = Option.empty[Drone]

  def target = _target
  def target_=(value: Option[Drone]): Unit = {
    if (_target != value) {
      for (t <- _target) self.context.battleCoordinator.notTargeting(t, this)
      for (t <- value) self.context.battleCoordinator.targeting(t, this)
      _target = value
    }
  }

  def isCommited = _attack.exists(!_.isDead)
  def attack(enemy: Drone): Unit = _attack = Some(enemy)
}
