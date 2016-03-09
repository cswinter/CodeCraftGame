package cwinter.codecraft.core.ai.shared

import cwinter.codecraft.core.api.MineralCrystal
import cwinter.codecraft.util.maths.Vector2


private[codecraft] class HarvestCoordinatorWithZones extends BasicHarvestCoordinator {
  import HarvestCoordinatorWithZones.ZoneWidth
  private var zones = Map.empty[(Int, Int), HarvestingZone]
  private var assignedZones = Set.empty[(Int, Int)]


  override def registerMineral(mineral: MineralCrystal): Unit = {
    super.registerMineral(mineral)

    val zone@(x, y) = determineZone(mineral.position)
    if (!zones.contains(zone)) {
      zones += zone -> new HarvestingZone(x, y)
    }
    zones(zone).register(mineral)
  }

  def findClosestMineral(position: Vector2, maybeZone: Option[HarvestingZone]): Option[MineralCrystal] = {
    maybeZone match {
      case None => findClosestMineral(position)
      case Some(zone) => closestUnclaimedMineral(position, zone.minerals)
    }
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

  override def update(): Unit = {
    super.update()
    for ((_, zone) <- zones) zone.update()
    zones = zones.filter(!_._2.exhausted)
  }

  def determineZone(pos: Vector2): (Int, Int) =
    ((pos.x / ZoneWidth).toInt, (pos.y / ZoneWidth).toInt)
}

private[codecraft] object HarvestCoordinatorWithZones {
  final val ZoneWidth = 750
}

private[codecraft] class HarvestingZone(
  val x: Int,
  val y: Int
) {
  val midpoint = Vector2(x + 0.5f, y + 0.5f) * HarvestCoordinatorWithZones.ZoneWidth
  private var _minerals = Set.empty[MineralCrystal]
  def minerals: Set[MineralCrystal] = _minerals

  def weightedMidpoint = _minerals.map(_.position).foldLeft(Vector2.Null)(_ + _) / _minerals.size

  def register(mineralCrystal: MineralCrystal): Unit = {
    _minerals += mineralCrystal
  }

  def exhausted: Boolean = _minerals.isEmpty

  def update(): Unit = _minerals = _minerals.filter(!_.harvested)
}



