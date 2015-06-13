package cwinter.codecraft.core.api

import scala.collection.JavaConverters._

class JDroneController extends DroneControllerBase {
  def storedMinerals: java.util.List[MineralCrystalHandle] = super.storedMineralsScala.asJava
  def dronesInSight: java.util.Set[DroneHandle] = super.dronesInSightScala.asJava
}

