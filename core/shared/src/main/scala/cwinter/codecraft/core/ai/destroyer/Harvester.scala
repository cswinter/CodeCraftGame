package cwinter.codecraft.core.ai.destroyer

import cwinter.codecraft.core.ai.shared.AugmentedController
import cwinter.codecraft.core.api.{Drone, MineralCrystal}


class Harvester(ctx: DestroyerContext)
extends AugmentedController('Harvester, ctx) {
  var hasReturned = false
  var nextCrystal: Option[MineralCrystal] = None


  override def onTick(): Unit = {
    if (nextCrystal.exists(_.harvested)) nextCrystal = None
    if (nextCrystal.isEmpty && availableStorage > 0)
      nextCrystal = context.harvestCoordinator.findClosestMineral(position)

    if (enemies.nonEmpty && closestEnemy.spec.missileBatteries > 0) {
      moveInDirection(position - closestEnemy.position)
    } else {
      if (availableStorage == 0 && !hasReturned) {
        moveTo(context.mothership)
        for (m <- nextCrystal if !m.harvested)
          context.harvestCoordinator.abortHarvestingMission(m)
        nextCrystal = None
      } else if (hasReturned && availableStorage > 0 || nextCrystal.isEmpty) {
        hasReturned = false
        scout()
      } else {
        for (c <- nextCrystal if !isHarvesting)
          moveTo(c)
      }
    }
  }

  override def onArrivesAtMineral(m: MineralCrystal): Unit = {
    if (!m.harvested) {
      harvest(m)
    }
  }

  override def onArrivesAtDrone(drone: Drone): Unit = {
    giveMineralsTo(drone)
    hasReturned = true
  }

  override def onDeath(): Unit = {
    super.onDeath()
    for (m <- nextCrystal)
      context.harvestCoordinator.abortHarvestingMission(m)
  }
}

