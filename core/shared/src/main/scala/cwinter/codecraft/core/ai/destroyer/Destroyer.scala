package cwinter.codecraft.core.ai.destroyer

import cwinter.codecraft.core.api.MineralCrystal
import cwinter.codecraft.util.maths.Vector2


class Destroyer(ctx: DestroyerContext)
extends DestroyerController('Destroyer, ctx) {
  var hasReturned = false
  var nextCrystal: Option[MineralCrystal] = None
  var flightTimer = 0

  override def onTick(): Unit = {
    context.battleCoordinator.getTarget match {
      case None => scoutRandomly()
      case Some(target) => moveTo(target.lastKnownPosition)
    }
    handleWeapons()
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

  def avoidThreats(): Unit = {
    val threats = enemies.filter(_.spec.missileBatteries > 0)
    if (threats.nonEmpty) {
      val (closest, dist2) = {
        for (threat <- threats)
          yield (threat, (threat.position - position).lengthSquared)
      }.minBy(_._2)

      if (dist2 < 300 * 300) {
        moveInDirection(position - closest.position)
        flightTimer = 20
        for (n <- searchToken) {
          context.searchCoordinator.dangerous(n)
          searchToken = None
        }
      }
    }
  }
}

