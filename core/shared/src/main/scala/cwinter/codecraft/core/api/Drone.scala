package cwinter.codecraft.core.api

import cwinter.codecraft.core.errors.Errors
import cwinter.codecraft.core.objects.drone.DroneImpl
import cwinter.codecraft.graphics.engine.Debug
import cwinter.codecraft.util.maths.{ColorRGB, ColorRGBA, Vector2}

import scala.scalajs.js.annotation.JSExportAll


@JSExportAll
trait Drone {
  private[core] def drone: DroneImpl

  /** The position of this drone at the last time it was seen by any of your drones. */
  def lastKnownPosition: Vector2

  /** The orientation of this drone at the last time it was seen by any of your drones. */
  def lastKnownOrientation: Double

  /** Returns whether this drone is within the sight radius of any of your drones.
    *
    * This property always returns true for your own drones.
    * If the drone is an enemy and [[isVisible]] is `false`, you will
    * be unable to query properties such as [[position]].
    */
  def isVisible: Boolean

  /** Returns true if this drone is an enemy, false if it is one of your own drones. */
  def isEnemy: Boolean

  /** Returns the drone's position. */
  def position: Vector2 =
    ensureVisible(drone.position, " (You may want to use `lastKnownPosition`.)")

  /** Returns the drones orientation in radians. */
  def orientation: Double =
    ensureVisible(drone.dynamics.orientation, " (You may want to use `lastKnownOrientation`.)")

  /** Returns the drone's homing missile cooldown. */
  def missileCooldown: Int = ensureVisible(drone.missileCooldown)

  /** Returns the amount of resources store by this drone. */
  def storedResources: Int = ensureVisible(drone.storedResources)

  /** Returns the current number of hitpoints, including shields. */
  def hitpoints: Int = ensureVisible(drone.hitpoints)

  /** Returns the number of free storage capacity. */
  def availableStorage: Int = ensureVisible(drone.availableStorage)

  /** Returns true if this drone is currently constructing another drone. */
  def isConstructing: Boolean = ensureVisible(drone.isConstructing)

  /** Returns true if this drone is currently harvesting a mineral. */
  def isHarvesting: Boolean = ensureVisible(drone.isHarvesting)

  /** Returns true of this drone has a movement command active or queued up, false otherwise. */
  def isMoving: Boolean = ensureVisible(drone.isMoving)

  /** Returns the identifier of the player that owns this drone. */
  def playerID: Int = drone.player.id

  /** Returns true if this drone is dead, false otherwise. */
  def isDead: Boolean = drone.hitpoints <= 0

  /** Draws the specified text at the position of the drone on this timestep. */
  def showText(text: String): Unit =
    if (drone.context.settings.allowMessages)
      drone.showText(text)

  /** Draws the specified text at the specified position on this timestep. */
  def showText(text: String, position: Vector2): Unit =
    if (drone.context.settings.allowMessages)
      drone.showText(text, position.x, position.y)

  /** Returns an object that specifies how many copies of each module the drone has. */
  @inline
  final def spec: DroneSpec = drone.spec
  /** Returns the number of storage modules. */
  final def storageModules: Int = spec.storageModules
  /** Returns the number of missile battery modules. */
  final def missileBatteries: Int = spec.missileBatteries
  /** Returns the number of constructor modules */
  final def constructors: Int = spec.constructors
  /** Returns the number of engine modules */
  final def engines: Int = spec.engines
  /** Returns the number of shield generator modules */
  final def shieldGenerators: Int = spec.shieldGenerators
  /** Returns the amount of hitpoints at full health. */
  final def maxHitpoints: Int = spec.maxHitpoints
  /** Returns the drone's maximum speed. */
  final def maxSpeed: Double = spec.maxSpeed

  private def ensureVisible[T](property: => T, message: String = ""): T = {
    if (isVisible) property
    else drone.context.errors.error(
      new ObjectNotVisibleException(
        s"Trying to access state of an enemy drone that is not inside the sight radius of any of your drones.$message"),
      drone.position
    )
  }

  /** Returns a string that encodes various properties of the drone. */
  def displayString: String = {
    if (drone == null) { spec.maxHitpoints
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
       |Hitpoints: ${if (isVisible) hitpoints else "?"}/${spec.maxHitpoints}
       |Position:  (${lastKnownPosition.x},${lastKnownPosition.y})
       |Max Speed: ${spec.maxSpeed}
       |Radius:    ${spec.radius}
    """.stripMargin
  }

  /** Returns the total amount of resources available to this drone.
    * This includes any mineral crystals that are small enough to be processed by this drone.
    */
  @deprecated("Use `storedResources` instead.", "0.2.4.0")
  def totalAvailableResources: Int = storedResources

  @deprecated("Use missileCooldown instead.", "0.2.4.3")
  def weaponsCooldown: Int = missileCooldown
}

