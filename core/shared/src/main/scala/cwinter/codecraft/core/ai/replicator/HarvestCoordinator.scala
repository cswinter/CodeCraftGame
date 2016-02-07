package cwinter.codecraft.core.ai.replicator

import cwinter.codecraft.core.api.MineralCrystal
import cwinter.codecraft.util.maths.Vector2


class HarvestCoordinator {
  import HarvestCoordinator._
  private var minerals = Set.empty[MineralCrystal]
  private var claimedMinerals = Set.empty[MineralCrystal]
  private var zones = Map.empty[(Int, Int), HarvestingZone]
  private var assignedZones = Set.empty[(Int, Int)]

  def findClosestMineral(position: Vector2, assignedZone: Option[HarvestingZone] = None): Option[MineralCrystal] = {
    val filtered =
      for (
        m <- minerals -- claimedMinerals
        if !assignedZone.exists(z => (z.x, z.y) != determineZone(m.position))
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
    minerals += mineralCrystal
    val zone@(x, y) = determineZone(mineralCrystal.position)
    if (!zones.contains(zone)) {
      zones += zone -> new HarvestingZone(x, y)
    }
    zones(zone).register(mineralCrystal)
  }

  def abortHarvestingMission(mineralCrystal: MineralCrystal): Unit = {
    claimedMinerals -= mineralCrystal
  }

  def requestHarvestingZone(position: Vector2): Option[HarvestingZone] = {
    val candidates = zones -- assignedZones
    if (candidates.isEmpty) None
    else {
      val (location, zone) =
        candidates.minBy(x => (x._2.midpoint - position).lengthSquared)
      assignedZones += location
      Some(zone)
    }
  }

  def freeZoneCount: Int = {
    (zones -- assignedZones).size
  }

  def update(): Unit = {
    minerals = minerals.filter(!_.harvested)
    for ((_, zone) <- zones) zone.update()
    zones = zones.filter(!_._2.exhausted)
  }
}

object HarvestCoordinator {
  final val ZoneWidth = 750

  def determineZone(pos: Vector2): (Int, Int) =
    ((pos.x / ZoneWidth).toInt, (pos.y / ZoneWidth).toInt)
}

class HarvestingZone(
  val x: Int,
  val y: Int
) {
  val midpoint = Vector2(x + 0.5f, y + 0.5f) * HarvestCoordinator.ZoneWidth
  private var minerals = Set.empty[MineralCrystal]

  def weightedMidpoint = minerals.map(_.position).foldLeft(Vector2.Null)(_ + _) / minerals.size

  def register(mineralCrystal: MineralCrystal): Unit = {
    minerals += mineralCrystal
  }

  def exhausted: Boolean = minerals.isEmpty

  def update(): Unit = minerals = minerals.filter(!_.harvested)
}


