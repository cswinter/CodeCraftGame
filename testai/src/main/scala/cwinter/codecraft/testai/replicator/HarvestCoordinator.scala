package cwinter.codecraft.testai.replicator

import cwinter.codecraft.core.api.MineralCrystal
import cwinter.codecraft.util.maths.Vector2


class HarvestCoordinator {
  var minerals = Set.empty[MineralCrystal]
  var claimedMinerals = Set.empty[MineralCrystal]

  def findClosestMineral(position: Vector2): Option[MineralCrystal] = {
    minerals = minerals.filter(!_.harvested)
    val filtered = minerals -- claimedMinerals
    val result =
      if (filtered.isEmpty) None
      else Some(filtered.minBy(m => (m.position - position).lengthSquared))
    for (m <- result) {
      claimedMinerals += m
    }
    result
  }

  def registerMineral(mineralCrystal: MineralCrystal): Unit = {
    minerals += mineralCrystal
  }

  def abortHarvestingMission(mineralCrystal: MineralCrystal): Unit = {
    claimedMinerals -= mineralCrystal
  }
}

