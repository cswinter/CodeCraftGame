package cwinter.codecraft.core.api

import cwinter.codecraft.core.objects.drone.DroneImpl
import cwinter.codecraft.util.maths.Vector2

import scala.scalajs.js.annotation.JSExportAll


@JSExportAll
trait Drone {
  /**
   * Returns the drone's position.
   */
  def position: Vector2

  /**
   * Returns an object that specifies how many copies of each module the drone has.
   */
  def spec: DroneSpec

  /**
   * Returns the drone's homing missile cooldown.
   */
  def weaponsCooldown: Int

  /**
   * Returns whether this drone is within the sight radius of any of your drones.
   *
   * This property always returns true for your own drones.
   * If the drone is an enemy and [[isVisible]] is `false`, you will
   * be unable to query properties such as [[position]].
   */
  def isVisible: Boolean

  /**
   * Returns the identifier of the player that owns this drone.
   */
  def playerID: Int

  /**
   * Returns the current number of hitpoints.
   */
  def hitpoints: Int

  /**
   * Returns true if this drone is an enemy, false if it is one of your own drones.
   */
  def isEnemy: Boolean

  /**
   * Returns the total amount of resources available to this drone.
   * This includes any mineral crystals that are small enough to be processed by this drone.
   */
  @deprecated("The `storedResources` method now returns the same result and should be used instead.", "0.2.4.0")
  def totalAvailableResources: Int

  /**
    * Returns the amount of resources store by this drone.
    */
  def storedResources: Int

  /**
   * Returns true if this drone is dead, false otherwise.
   */
  def isDead: Boolean = hitpoints <= 0

  private[core] def drone: DroneImpl

  override def toString: String = {
    if (this == null) {

    }
    if (spec == null) {
      return "[Uninitialised drone controller]"
    }
    def m(count: Int, descr: String): Option[String] = count match {
      case 0 => None
      case 1 => Some(descr)
      case c if descr.last == 'y' => Some(c + " " + descr.substring(0, descr.length - 1) + "ies")
      case c => Some(c + " " + descr + "s")
    }
    val moduleDescrs = Seq(
      m(spec.engines, "Engine"),
      m(spec.shieldGenerators, "Shield Generator"),
      m(spec.constructors, "Constructor"),
      m(spec.missileBatteries, "Missile Battery"),
      m(spec.storageModules, "Storage Module")
    )
    val moduleString =
      (for (Some(s) <- moduleDescrs) yield s).mkString("[", ", ", "]")
    s"""Modules:   $moduleString
       |Hitpoints: $hitpoints/${spec.maxHitpoints}
       |Position:  (${position.x},${position.y})
       |Max Speed: ${spec.maximumSpeed}
       |Radius:    ${spec.radius}
    """.stripMargin
  }
}

