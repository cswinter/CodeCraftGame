package cwinter.codecraft.core.ai.basicplus

import cwinter.codecraft.core.api.{Drone, MineralCrystal}

private[core] class ScoutingDroneController(val mothership: Mothership) extends BasicPlusController('Harvester) {
  var hasReturned = false
  var nextCrystal: Option[MineralCrystal] = None


  override def onTick(): Unit = {
    if (nextCrystal.exists(_.harvested)) nextCrystal = None
    if (nextCrystal.isEmpty && availableStorage > 0) nextCrystal = mothership.findClosestMineral(position)

    if (enemies.nonEmpty && closestEnemy.spec.missileBatteries > 0) {
      moveInDirection(position - closestEnemy.position)
    } else {
      if (availableStorage <= 0 && !hasReturned) {
        moveTo(mothership)
        nextCrystal.foreach(mothership.abortHarvestingMission)
        nextCrystal = None
      } else if (hasReturned && availableStorage > 0 || (nextCrystal.isEmpty && !isHarvesting)) {
        hasReturned = false
        scout()
      } else {
        for (
          c <- nextCrystal
          if !isHarvesting
        ) moveTo(c)
      }
    }
  }

  override def onArrivesAtMineral(mineral: MineralCrystal): Unit = {
    harvest(mineral)
  }

  override def onArrivesAtDrone(drone: Drone): Unit = {
    giveResourcesTo(drone)
    hasReturned = true
  }

  override def onDeath(): Unit = {
    for (m <- nextCrystal)
      mothership.abortHarvestingMission(m)
  }
}
