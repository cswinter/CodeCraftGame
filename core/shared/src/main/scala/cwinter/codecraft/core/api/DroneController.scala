package cwinter.codecraft.core.api

// text duplicated in BaseDroneController and JDroneController
/**
 * A drone controller is an object that governs the behaviour of a drone.
 * It exposes a wide range of methods to query the underlying drone's state and give it commands.
 * You can inherit from this class to and override the `onEvent` methods to implement a
 * drone controller with custom behaviour.
 *
 * In Java, use [[JDroneController]] instead.
 */
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

