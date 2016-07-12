package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.collisions.{VisionTracking, ActiveVisionTracking}
import cwinter.codecraft.core.api.{MineralCrystal, Drone}
import cwinter.codecraft.core.objects.MineralCrystalImpl


private[codecraft] trait DroneVisionTracker extends ActiveVisionTracking { self: DroneImpl =>
  private[this] var _mineralsInSight = Set.empty[MineralCrystal]
  private[this] var _dronesInSight = Set.empty[Drone]
  private[this] var _enemiesInSight = Set.empty[Drone]
  private[this] var _alliesInSight = Set.empty[Drone]


  override def objectEnteredVision(obj: VisionTracking): Unit = obj match {
    case mineral: MineralCrystalImpl =>
      _mineralsInSight += mineral.getHandle(player)
      enqueueEvent(MineralEntersSightRadius(mineral))
    case drone: DroneImpl =>
      val wrapped = drone.wrapperFor(player)
      _dronesInSight += wrapped
      if (wrapped.isEnemy) _enemiesInSight += wrapped
      else _alliesInSight += wrapped
      enqueueEvent(DroneEntersSightRadius(drone))
  }

  override def objectLeftVision(obj: VisionTracking): Unit = obj match {
    case mineral: MineralCrystalImpl =>
      _mineralsInSight -= mineral.getHandle(player)
    case drone: DroneImpl =>
      val wrapped = drone.wrapperFor(player)
      _dronesInSight -= wrapped
      if (wrapped.isEnemy) _enemiesInSight -= wrapped
      else _alliesInSight -= wrapped
  }

  override def objectRemoved(obj: VisionTracking): Unit = {
    val wasVisible =
      obj match {
        case mineral: MineralCrystalImpl => _mineralsInSight.contains(mineral.getHandle(player))
        case drone: DroneImpl => _dronesInSight.contains(drone.wrapperFor(player))
      }
    if (wasVisible) objectLeftVision(obj)
  }


  def dronesInSight: Set[Drone] = if (isDead) Set.empty else _dronesInSight
  def enemiesInSight: Set[Drone] = if (isDead) Set.empty else _enemiesInSight
  def alliesInSight: Set[Drone] = if (isDead) Set.empty else _alliesInSight
}
