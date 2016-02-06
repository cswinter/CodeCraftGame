package cwinter.codecraft.core.api

import scala.collection.JavaConverters._

// text duplicated in DroneControllerBase and DroneController
/**
 * A drone controller is an object that governs the behaviour of a drone.
 * It exposes a wide range of methods to query the underlying drone's state and give it commands.
 * You can inherit from this class and override the `onEvent` methods to implement a
 * drone controller with custom behaviour.
 *
 * In Scala, use [[DroneController]] instead.
 */
class JDroneController extends DroneControllerBase {
  /**
   * Returns an empty list.
   */
  @deprecated("Drones do not store mineral crystals anymore, only resources.", "0.2.4.0")
  def storedMinerals: java.util.List[MineralCrystal] = new java.util.ArrayList()
  /**
   * Gets all drones currently within the sight radius of this drone.
   */
  def dronesInSight: java.util.Set[Drone] = super.dronesInSightScala.asJava
}

