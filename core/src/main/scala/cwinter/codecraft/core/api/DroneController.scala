package cwinter.codecraft.core.api

class DroneController extends DroneControllerBase {
  /**
   * Gets all mineral crystals stored by this drone.
   */
   def storedMinerals: Seq[MineralCrystal] = super.storedMineralsScala

  /**
   * Gets all drones currently within the sight radius of this drone.
   */
   def dronesInSight: Set[Drone] = super.dronesInSightScala
 }

