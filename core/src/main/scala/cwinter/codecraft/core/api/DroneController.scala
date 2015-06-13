package cwinter.codecraft.core.api

class DroneController extends DroneControllerBase {
   def storedMinerals: Seq[MineralCrystalHandle] = super.storedMineralsScala
   def dronesInSight: Set[DroneHandle] = super.dronesInSightScala
 }

