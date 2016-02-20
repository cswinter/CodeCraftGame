package cwinter.codecraft.core.ai.destroyer

import cwinter.codecraft.core.api.{Drone, MineralCrystal}


class Harvester(ctx: DestroyerContext) extends DestroyerController(ctx) {
  var hasReturned = false
  var nextCrystal: Option[MineralCrystal] = None

  override def onTick(): Unit = {
    if (nextCrystal.exists(_.harvested)) abortHarvestingMission()
    if (nextCrystal.isEmpty && availableStorage > 0)
      nextCrystal = context.harvestCoordinator.findClosestMineral(position)

    if (shouldRunAway) {
      moveInDirection(position - closestEnemy.position)
      context.battleCoordinator.requestBigDaddy()
    } else {
      if (availableStorage == 0 && !hasReturned) {
        moveTo(context.mothership)
        abortHarvestingMission()
      } else if (hasReturned && availableStorage > 0 || nextCrystal.isEmpty) {
        hasReturned = false
        scout()
      } else {
        for (c <- nextCrystal if !isHarvesting)
          moveTo(c)
      }
    }
  }

  def shouldRunAway: Boolean =
    enemies.nonEmpty && closestEnemy.spec.missileBatteries > 0 &&
      (closestEnemy.spec.maximumSpeed > spec.maximumSpeed ||
        (closestEnemy.position - position).lengthSquared <= 380 * 380)


  override def onArrivesAtMineral(m: MineralCrystal): Unit = {
    if (!m.harvested) {
      harvest(m)
    }
  }

  override def onArrivesAtDrone(drone: Drone): Unit = {
    giveMineralsTo(drone)
    hasReturned = true
  }

  def abortHarvestingMission(): Unit = {
    for (m <- nextCrystal)
      context.harvestCoordinator.abortHarvestingMission(m)
    nextCrystal = None
  }

  override def onSpawn(): Unit = {
    super.onSpawn()
    context.battleCoordinator.harvesterOnline(this)
  }

  override def onDeath(): Unit = {
    super.onDeath()
    context.battleCoordinator.harvesterOffline(this)
    abortHarvestingMission()
  }
}

