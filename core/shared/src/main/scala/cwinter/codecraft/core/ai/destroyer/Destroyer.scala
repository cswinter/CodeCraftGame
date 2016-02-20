package cwinter.codecraft.core.ai.destroyer

import cwinter.codecraft.core.ai.shared.MissionExecutor
import cwinter.codecraft.core.api.{Drone, MineralCrystal}
import cwinter.codecraft.core.objects.drone.DroneConstants
import cwinter.codecraft.util.maths.Vector2


class Destroyer(ctx: DestroyerContext) extends DestroyerController(ctx)
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
    case MoveTo(position) =>
      approachCarefully(position)
  }

  def canFinish(enemy: Drone): Boolean = {
    val minDist = DroneConstants.MissileLockOnRadius + 150
    if (!enemy.isVisible || (position - enemy.lastKnownPosition).lengthSquared >= minDist * minDist)
      return false
    val enemyFirepower = armedEnemies.foldLeft(0)(_ + _.spec.missileBatteries)
    val tmp = spec.missileBatteries * hitpoints > enemy.hitpoints * enemyFirepower
    enemyFirepower == 0 || tmp
  }

  def approachCarefully(target: Vector2): Unit = {
    val threats = armedEnemies
    if (threats.isEmpty) moveWithoutCollision(target)
    else {
      val movementVector = moveAround(target, threats)
      moveInDirection(movementVector)
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
      val dangerZone = DroneConstants.MissileLockOnRadius + 50
      val safeZone = DroneConstants.MissileLockOnRadius + 150
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

  def moveWithoutCollision(target: Vector2): Unit = {
    val obstacles = dronesInSight.filter{d =>
      val dist2 = (d.position - position).lengthSquared
      val minDist = d.spec.radius + spec.radius + 10
      d.spec.missileBatteries > 0 && dist2 <= minDist * minDist
    }
    if (obstacles.isEmpty) moveTo(target)
    else {
      val targetVector = target - position
      val avoidanceVector = position - obstacles.head.position
      moveInDirection(targetVector.normalized + avoidanceVector.normalized)
    }
  }
}

