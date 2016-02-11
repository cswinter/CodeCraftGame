package cwinter.codecraft.core.ai.shared

import cwinter.codecraft.core.api.MineralCrystal
import cwinter.codecraft.util.maths.Vector2


class BasicHarvestCoordinator {
  private var _minerals = Set.empty[MineralCrystal]
  def minerals = _minerals
  private var claimedMinerals = Set.empty[MineralCrystal]

  def findClosestMineral(position: Vector2): Option[MineralCrystal] = {
    closestUnclaimedMineral(position, _minerals)
  }

  protected def closestUnclaimedMineral(position: Vector2, eligible: Set[MineralCrystal]): Option[MineralCrystal] = {
    val filtered =
      for (
        m <- _minerals -- claimedMinerals
      ) yield m
    val result =
      if (filtered.isEmpty) None
      else Some(filtered.minBy(m => (m.position - position).lengthSquared))
    for (m <- result) {
      claimedMinerals += m
    }
    result
  }

  def registerMineral(mineralCrystal: MineralCrystal): Unit = {
    _minerals += mineralCrystal
  }

  def abortHarvestingMission(mineralCrystal: MineralCrystal): Unit = {
    claimedMinerals -= mineralCrystal
  }

  def update(): Unit = {
    _minerals = _minerals.filter(!_.harvested)
  }
}

