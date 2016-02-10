package cwinter.codecraft.core.ai.destroyer

import cwinter.codecraft.core.ai.shared.AugmentedController
import cwinter.codecraft.core.api.{Drone, MineralCrystal}
import cwinter.codecraft.util.maths.Vector2


class Scout(ctx: DestroyerContext)
extends AugmentedController('Scout, ctx) {
  var hasReturned = false
  var nextCrystal: Option[MineralCrystal] = None
  var flightTimer = 0

  override def onTick(): Unit = {
    flightTimer -= 1
    if (flightTimer == 0) halt()

    if (flightTimer <= 0) {
      scout()
      if (searchToken.isEmpty) scoutRandomly()
      avoidThreats()
    }
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

