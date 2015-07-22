package cwinter.codecraft.core.api

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSExportAll


@JSExportAll
class JSDroneController extends DroneControllerBase {
  /**
   * Gets all mineral crystals stored by this drone.
   */
  def storedMinerals: js.Array[MineralCrystal] = super.storedMineralsScala.toJSArray

  /**
   * Gets all drones currently within the sight radius of this drone.
   */
  def dronesInSight: js.Array[Drone] = super.dronesInSightScala.toJSArray
}
