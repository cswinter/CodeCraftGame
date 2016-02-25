package cwinter.codecraft.core.ai.replicator

import cwinter.codecraft.core.ai.replicator.combat._
import cwinter.codecraft.core.ai.shared.{MissionExecutor, Mission}
import cwinter.codecraft.core.api.Drone
import cwinter.codecraft.core.objects.drone.DroneConstants
import cwinter.codecraft.util.maths.{Rng, Vector2}


class Soldier(ctx: ReplicatorContext) extends ReplicatorController(ctx)
with MissionExecutor[ReplicatorCommand] {
  private[this] var _target = Option.empty[Drone]

  override def onSpawn(): Unit = {
    super.onSpawn()
    context.battleCoordinator.online(this)
  }

  override def onTick(): Unit = {
    handleWeapons()
    mission.foreach(executeInstructions)
    handleEnemies()
  }

  def executeInstructions(mission: Mission[ReplicatorCommand]): Unit = mission.missionInstructions match {
    case Scout => scout()
    case Attack(maxDist, enemy, origin) =>
      if ((enemy.lastKnownPosition - position).lengthSquared > (maxDist - 250) * (maxDist - 250)) {
        approachCarefully(enemy.lastKnownPosition)
      } else halt()
      if ((enemy.lastKnownPosition - position).lengthSquared < 100 * 100) origin.notFound()
    case Search(position, radius) =>
      if (!isMoving) {
        approachCarefully(position + radius * Vector2(2 * math.Pi * context.rng.nextDouble()))
      }
    case AttackMove(position) =>
      approachCarefully(position)
    case Circle(center, radius) =>
      val smoothnessFactor = 0.005f
      val distance = (center - position).length - radius
      val approachVector = (center - position).normalized
      val orbitVector = Vector2(approachVector.y, -approachVector.x)
      val weight = math.exp(-distance * smoothnessFactor * distance * smoothnessFactor)

      val movementVector =
        weight * orbitVector +
        (1 - weight) * math.signum(distance) * approachVector

      moveInDirection(movementVector)
    case Observe(enemy, notFound) =>
      if ((enemy.lastKnownPosition - position).lengthSquared < 25 * 25) notFound()
      else approachCarefully(enemy.lastKnownPosition)
  }

  override def scout(): Unit = {
    if (searchToken.isEmpty) searchToken = requestSearchToken()
    for (t <- searchToken) {
      if ((position - t.pos).lengthSquared < 1) searchToken = None
      else approachCarefully(t.pos)
    }
    if (searchToken.isEmpty && !isMoving) {
      moveTo(0.9 * Rng.vector2(worldSize))
    }
  }

  def handleEnemies(): Unit = {
    val enemies = this.enemies
    if (enemies.nonEmpty) {
      val armed = enemies.filter(_.spec.missileBatteries > 0)
      target = findClosest(armed)
      if (target.exists(context.battleCoordinator.isCovered))
        moveTo(target.get.lastKnownPosition)
      else huntCivilians()
    }
  }

  def target = _target
  def target_=(value: Option[Drone]): Unit = {
    if (_target != value) {
      for (t <- _target) context.battleCoordinator.notTargeting(t, this)
      for (t <- value) context.battleCoordinator.targeting(t, this)
      _target = value
    }
  }

  def huntCivilians(): Unit = {
    val civilians = enemies.filter (_.spec.missileBatteries == 0)
    if (civilians.nonEmpty && shouldHunt) approachCarefully (civilians.head.lastKnownPosition)
  }

  def shouldHunt: Boolean = true

  def approachCarefully(target: Vector2): Unit = {
    val threats = armedEnemies
    if (threats.isEmpty) moveTo(target)
    else bypassThreats(target, threats)
  }

  def bypassThreats(target: Vector2, threats: Iterable[Drone]): Unit = {
    var targetDirection = (target - position).normalized

    val dangerZone = DroneConstants.MissileLockOnRadius
    val safeZone = DroneConstants.MissileLockOnRadius + 150
    for (threat <- threats) {
      val delta = position - threat.lastKnownPosition
      val weight = 1.5f * (
        if (delta.lengthSquared < dangerZone * dangerZone) 1
        else if (delta.lengthSquared > safeZone * safeZone) 0
        else (safeZone - delta.length) / (safeZone - dangerZone)
      )
      targetDirection += weight * delta.normalized
    }

    moveInDirection(targetDirection)
  }

  def findClosest(drones: Iterable[Drone]): Option[Drone] =
    if (drones.isEmpty) None
    else Some(drones.minBy(x => (x.position - position).lengthSquared))


  override def onDeath(): Unit = {
    super.onDeath()
    context.battleCoordinator.offline(this)
    abortMission()
    target = None
  }

  def isIdle: Boolean = mission.isEmpty
}

