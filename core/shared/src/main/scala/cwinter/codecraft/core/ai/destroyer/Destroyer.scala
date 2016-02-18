package cwinter.codecraft.core.ai.destroyer

import cwinter.codecraft.core.ai.shared.MissionExecutor
import cwinter.codecraft.core.api.MineralCrystal
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
    case Attack(enemy, notFound) =>
      if ((enemy.lastKnownPosition - position).lengthSquared < 100 * 100 && !enemy.isVisible) {
        notFound()
      } else moveTo(enemy.lastKnownPosition)
    case MoveTo(position) =>
      moveTo(position)
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
}

