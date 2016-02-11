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
      case None => scoutRandomly()
      case Some(mission) => executeCommand(mission.missionInstructions)
    }
    handleWeapons()
  }

  def executeCommand(command: DestroyerCommand): Unit = command match {
    case Attack(enemy, notFound) =>
      if ((enemy.lastKnownPosition - position).lengthSquared < 100 && !enemy.isVisible) {
        notFound()
      } else moveTo(enemy.lastKnownPosition)
    case MoveTo(position) =>
      moveTo(position)
  }

  def scoutRandomly(): Unit = {
    import context.rng
    if (!isMoving) {
      moveTo(Vector2(
        rng.nextDouble() * (worldSize.xMax - worldSize.xMin) + worldSize.xMin,
        rng.nextDouble() * (worldSize.yMax - worldSize.yMin) + worldSize.yMin
      ))
    }
  }


  override def onDeath(): Unit = {
    super.onDeath()
    context.battleCoordinator.offline(this)
    abortMission()
  }
}

