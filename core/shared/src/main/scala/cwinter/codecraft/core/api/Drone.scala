package cwinter.codecraft.core.api

import cwinter.codecraft.core.objects.drone.DroneImpl
import cwinter.codecraft.graphics.worldstate.Player
import cwinter.codecraft.util.maths.Vector2


trait Drone {
  /**
   * Returns the drones position.
   */
  def position: Vector2

  /**
   * Returns the drones module specification.
   */
  def spec: DroneSpec

  /**
   * Returns the drones homing missile cooldown.
   */
  def weaponsCooldown: Int

  /**
   * Returns whether the drone is within the sight radius of any of your drones.
   */
  def isVisible: Boolean

  /**
   * Returns the player who commands this drone.
   */
  def player: Player

  /**
   * Returns the current number of hitpoints.
   */
  def hitpoints: Int

  /**
   * Returns true if this drone is an enemy, false if it is one of your own drones.
   */
  def isEnemy: Boolean

  private[core] def drone: DroneImpl

  override def toString: String = {
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
      m(spec.refineries, "Refinery"),
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

