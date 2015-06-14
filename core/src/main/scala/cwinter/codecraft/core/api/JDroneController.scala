package cwinter.codecraft.core.api

import scala.collection.JavaConverters._

class JDroneController extends DroneControllerBase {
  /**
   * Gets all mineral crystals stored by this drone.
   */
  def storedMinerals: java.util.List[MineralCrystal] = super.storedMineralsScala.asJava
  /**
   * Gets all drones currently within the sight radius of this drone.
   */
  def dronesInSight: java.util.Set[Drone] = super.dronesInSightScala.asJava
}

