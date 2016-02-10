package cwinter.codecraft.core.ai.replicator

import cwinter.codecraft.core.api.{Drone, MineralCrystal}


class Harvester(
  mothership: Replicator,
  ctx: ReplicatorContext
) extends ReplicatorController(ctx) {
  var hasReturned = false
  var nextCrystal: Option[MineralCrystal] = None
  var master: Option[Replicator] = Some(mothership)


  override def onSpawn(): Unit = {
    super.onSpawn()
    master.foreach(_.registerSlave(this))
  }

  override def onTick(): Unit = {
    if (master.exists(_.isDead)) {
      context.mothershipCoordinator.registerOrphan(this)
      master = None
    }
    if (nextCrystal.exists(_.harvested)) nextCrystal = None
    if (nextCrystal.isEmpty && availableStorage > 0)
      nextCrystal = context.harvestCoordinator.findClosestMineral(position)

    if (enemies.nonEmpty && closestEnemy.spec.missileBatteries > 0) {
      moveInDirection(position - closestEnemy.position)
    } else {
      if (availableStorage == 0 && !hasReturned) {
        master.foreach(moveTo)
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
    master.foreach(_.slaveFailed(this))
  }

  def assignNewMaster(newMaster: Replicator): Unit = {
    master = Some(newMaster)
    newMaster.registerSlave(this)
  }
}
