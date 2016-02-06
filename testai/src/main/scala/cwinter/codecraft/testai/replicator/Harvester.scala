package cwinter.codecraft.testai.replicator

import cwinter.codecraft.core.api.{Drone, MineralCrystal}


class Harvester(
  var mothership: Replicator,
  ctx: ReplicatorContext
) extends BaseController('Harvester, ctx) {
  var hasReturned = false
  var nextCrystal: Option[MineralCrystal] = None


  override def onSpawn(): Unit = {
    mothership.registerSlave(this)
  }

  override def onTick(): Unit = {
    if (nextCrystal.exists(_.harvested)) nextCrystal = None
    if (nextCrystal.isEmpty && availableStorage > 0)
      nextCrystal = context.harvestCoordinator.findClosestMineral(position)

    if (enemies.nonEmpty && closestEnemy.spec.missileBatteries > 0) {
      moveInDirection(position - closestEnemy.position)
    } else {

      if (availableStorage == 0 && !hasReturned) {
        moveTo(mothership)
        for (m <- nextCrystal if !m.harvested)
          context.harvestCoordinator.abortHarvestingMission(m)
        nextCrystal = None
      } else if (hasReturned && availableStorage > 0 || nextCrystal.isEmpty) {
        hasReturned = false
        scout()
      } else {
        for (
          c <- nextCrystal
          if !(c.position ~ position)
        ) moveTo(c)
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
    for (m <- nextCrystal)
      context.harvestCoordinator.abortHarvestingMission(m)
    mothership.slaveFailed(this)
  }
}
