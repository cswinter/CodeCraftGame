package cwinter.codecraft.core.ai.replicator

import cwinter.codecraft.core.ai.replicator.combat._
import cwinter.codecraft.core.ai.shared.{MissionExecutor, Mission}
import cwinter.codecraft.util.maths.{Rng, Vector2}


class Soldier(ctx: ReplicatorContext) extends ReplicatorController(ctx)
with MissionExecutor[ReplicatorCommand] {
  private var flightTimer = 0
  private var onRoute = false


  override def onSpawn(): Unit = {
    super.onSpawn()
    context.battleCoordinator.online(this)
  }

  override def onTick(): Unit = {
    handleWeapons()
    flightTimer -= 1

    if (enemies.nonEmpty) {
      if ((mission.isEmpty || mission.contains(ScoutingMission)) && enemyMuchStronger) {
        val closest = closestEnemy
        moveInDirection(position - closest.position)
        for (s <- searchToken) {
          searchToken = None
          context.searchCoordinator.dangerous(s)
          flightTimer = 150
        }
      } else {
        val (closest, dist2) = closestEnemyAndDist2
        val armed = closest.spec.missileBatteries > 0
        if (dist2 <= 330 * 330 || !onRoute || !enemyMuchStronger) {
          if (!armed || dist2 > 200 * 200) moveInDirection(closest.position - position)
          else halt()
          if (armed) context.battleCoordinator.requestAssistance(this)
        }
      }
    } else {
      onRoute = false
      mission.foreach(executeInstructions)
    }
  }

  def executeInstructions(mission: Mission[ReplicatorCommand]): Unit = mission.missionInstructions match {
    case Scout =>
      if (flightTimer <= 0) {
        scout()
        if (searchToken.isEmpty && Rng.bernoulli(0.005)) {
          moveTo(0.9 * Rng.vector2(worldSize))
        }
      }
    case Attack(enemy, origin) =>
      onRoute = (enemy.lastKnownPosition - position).lengthSquared > 800 * 800
      moveTo(enemy.lastKnownPosition)
      if ((enemy.lastKnownPosition - position).lengthSquared < 50 * 50) origin.notFound()
    case Search(position, radius) =>
      if (!isMoving) {
        moveTo(position + radius * Vector2(2 * math.Pi * context.rng.nextDouble()))
      }
    case AttackMove(position) =>
      onRoute = (position - this.position).lengthSquared > 800 * 800
      moveTo(position)
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

      onRoute = distance > 500
  }

  override def onDeath(): Unit = {
    super.onDeath()
    context.battleCoordinator.offline(this)
    abortMission()
  }

  def isIdle: Boolean = mission.isEmpty
}

