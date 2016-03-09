package cwinter.codecraft.core.ai.destroyer

import cwinter.codecraft.core.api.GameConstants.MissileLockOnRange
import cwinter.codecraft.core.ai.shared.MissionExecutor
import cwinter.codecraft.core.api.{Drone, MineralCrystal}
import cwinter.codecraft.util.maths.Vector2


private[codecraft] class Destroyer(ctx: DestroyerContext) extends DestroyerController(ctx)
with MissionExecutor[DestroyerCommand] {
  var hasReturned = false
  var nextCrystal: Option[MineralCrystal] = None
  var flightTimer = 0

  override def onSpawn(): Unit = {
    super.onSpawn()
    context.battleCoordinator.online(this)
  }

  override def onTick(): Unit = {
    mission match {
      case None => patrolMinerals()
      case Some(mission) => executeCommand(mission.missionInstructions)
    }
    handleWeapons()
  }

  def executeCommand(command: DestroyerCommand): Unit = command match {
    case Attack(enemy, notFound, metResistance) =>
      if ((enemy.lastKnownPosition - position).lengthSquared < 100 * 100 && !enemy.isVisible) {
        notFound()
      } else {
        if (canFinish(enemy)) moveTo(enemy.lastKnownPosition)
        else approachCarefully(enemy.lastKnownPosition)
      }
      if (armedEnemies.nonEmpty) metResistance(calculateStrength(armedEnemies))
    case MoveTo(position) =>
      approachCarefully(position)
    case MoveClose(targetPos, targetDist) =>
      val dist2 = (targetPos - position).lengthSquared
      if (dist2 > (targetDist + 25) * (targetDist + 25))
        moveInDirection(targetPos - position)
      else if (dist2 < (targetDist - 25) * (targetDist - 25))
        moveInDirection(position - targetPos)
      else halt()
  }

  def canFinish(enemy: Drone): Boolean = {
    val minDist = MissileLockOnRange + 150
    if (!enemy.isVisible || (position - enemy.lastKnownPosition).lengthSquared >= minDist * minDist)
      return false
    val enemyFirepower = armedEnemies.foldLeft(0)(_ + _.spec.missileBatteries)
    val tmp = spec.missileBatteries * hitpoints > enemy.hitpoints * enemyFirepower
    enemyFirepower == 0 || tmp
  }

  def approachCarefully(target: Vector2): Unit = {
    val threats = armedEnemies
    if (threats.isEmpty) moveWithoutCollision(target - position)
    else {
      val movementVector = moveAround(target, threats)
      moveWithoutCollision(movementVector)
    }
  }

  def patrolMinerals(): Unit = {
    if (isMoving) return
    val minerals = context.harvestCoordinator.minerals
    if (minerals.isEmpty) return

    val totalSize = minerals.foldLeft(0)(_ + _.size)
    val rand = context.rng.nextInt(totalSize)

    var cumulative = 0
    val randMineral = minerals.find { m =>
      cumulative += m.size
      rand < cumulative
    }

    randMineral.foreach(moveTo)
  }

  override def abortMission(): Unit = {
    super.abortMission()
    if (!isDead) halt()
  }

  override def onDeath(): Unit = {
    super.onDeath()
    context.battleCoordinator.offline(this)
    abortMission()
  }

  def moveAround(target: Vector2, threats: Iterable[Drone]): Vector2 = {
    var threatVector = Vector2.Null
    var threatWeight = 0.0
    for (threat <- threats) {
      val delta = position - threat.lastKnownPosition
      val dangerZone = MissileLockOnRange + 50
      val safeZone = MissileLockOnRange + 150
      val distanceModifier =
        if (delta.lengthSquared < dangerZone * dangerZone) 1
        else if (delta.lengthSquared > safeZone * safeZone) 0
        else (safeZone - delta.length) / (safeZone - dangerZone)
      val strength = threat.spec.missileBatteries + threat.spec.shieldGenerators
      val weight = strength * distanceModifier
      threatWeight += weight
      threatVector += weight * delta.normalized
    }

    val targetContribution = 2 * (target - position).normalized
    if (threatVector == Vector2.Null) targetContribution
    else {
      val alliedStrength = dronesInSight.filter(!_.isEnemy).foldLeft(0) {
          case (acc, d) => acc + d.spec.missileBatteries + d.spec.shieldGenerators
        }
      val adjustedEnemyWeight = math.max(0, threatWeight - alliedStrength * 0.8f)

      targetContribution + threatVector.normalized * adjustedEnemyWeight
    }
  }

  def moveWithoutCollision(targetDirection: Vector2): Unit = {
    val abstand = 30.0
    val obstacles = dronesInSight.filter{d =>
      val dist2 = (d.position - position).lengthSquared
      val minDist = d.spec.radius + spec.radius + abstand
      d.spec.missileBatteries > 0 && dist2 <= minDist * minDist
    }
    if (obstacles.isEmpty) moveInDirection(targetDirection)
    else {
      val obstacle = obstacles.head
      val avoidanceVector = position - obstacle.position
      val weight = (avoidanceVector.length - obstacle.spec.radius - spec.radius) / abstand
      moveInDirection(weight * targetDirection.normalized + (1 - weight) * avoidanceVector.normalized)
    }
  }
}

