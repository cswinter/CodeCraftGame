package cwinter.codecraft.core.api

import cwinter.codecraft.core.api.GameConstants.MissileLockOnRange
import cwinter.codecraft.core.objects.drone._
import cwinter.codecraft.util.maths._

import scala.scalajs.js.annotation.JSExportAll


// The description part of this Scaladoc is identical to that in DroneController and JDroneController.
// It might be possible to deduplicate using doc macros, but I couldn't find a way to use @define
// when the comments are in different files.
/**
 * A drone controller is an object that governs the behaviour of a drone.
 * It exposes a wide range of methods to query the underlying drone's state and give it commands.
 * You can inherit from this class and override the `onEvent` methods to implement a
 * drone controller with custom behaviour.
 *
 * NOTE: You should not actually use this class, but one of it's specialisations.
 * In Scala, use [[DroneController]] and in Java use [[JDroneController]].
 */
@JSExportAll
trait DroneControllerBase extends Drone {
  private[this] var _drone: DroneImpl = null


  /** Called once when the drone is spawned. Called before any other `onEvent` method is called. */
  def onSpawn(): Unit = ()

  /** Called if the drone is destroyed. */
  def onDeath(): Unit = ()

  /** Called once every tick, after all other `onEvent` methods have been called. */
  def onTick(): Unit = ()

  /** Called when a mineral crystal enters the sight radius of this drone. */
  def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = ()

  /** Called when another drone enters the sight radius of this drone. */
  def onDroneEntersVision(drone: Drone): Unit = ()

  /** Called when this drone arrives a position after invoking the one of the `moveTo` methods. */
  def onArrivesAtPosition(): Unit = ()

  /** Called when this drone arrives at a mineral crystal after invoking the moveTo(mineral: MineralCrystalHandle) command. */
  def onArrivesAtMineral(mineralCrystal: MineralCrystal): Unit = ()

  /** Called when this drone arrives at another drone after invoking the moveTo(drone: DroneHandle) command. */
  def onArrivesAtDrone(drone: Drone): Unit = ()

  /** Called if the drone constructing this drone is destroyed before construction completes.
    * In such a case, this is the only event function to be called.
    */
  def onConstructionCancelled(): Unit = ()

  /** When you start the game, this method will be called once on your initial drone controller
    * to give you an opportunity to provide a [[cwinter.codecraft.core.api.MetaController]].
    */
  def metaController: Option[MetaController] = None


  /** Order the drone to keep moving in the direction of `directionVector`. */
  def moveInDirection(directionVector: Vector2): Unit = {
    if (directionVector == Vector2.Null) halt()
    else drone ! MoveInDirection(directionVector.orientation)
  }

  /** Order the drone to keep moving in the direction of `direction`. */
  def moveInDirection(direction: Double): Unit = drone ! MoveInDirection(direction.toFloat)

  /** Order the drone to keep moving in the direction of `direction`. */
  def moveInDirection(direction: Float): Unit = drone ! MoveInDirection(direction)

  /** Order the drone to move towards `otherDrone`, until it is within 10 units distance of colliding. */
  def moveTo(otherDrone: Drone): Unit = {
    // TODO: most validation is in DroneImpl, maybe make consistent?
    if (otherDrone.isEnemy && !otherDrone.isVisible) {
      drone.error("Cannot moveTo enemy drone that is not inside the sight radius of any of your drones.")
    } else if (otherDrone.isDead) {
      drone.warn("Trying to moveTo a dead drone!")
    } else {
      drone ! MoveToDrone(otherDrone.drone)
    }
  }

  /** Order the drone to move towards `mineralCrystal`. */
  def moveTo(mineralCrystal: MineralCrystal): Unit =
    drone ! MoveToMineralCrystal(mineralCrystal.mineralCrystal)

  /** Order the drone to move to coordinates (`x`, `y`). */
  def moveTo(x: Double, y: Double): Unit = moveTo(Vector2(x, y))

  /** Order the drone to move to `position`. */
  def moveTo(position: Vector2): Unit = {
    val clippedPosition = clipToRectangle(position, worldSize)
    drone ! MoveToPosition(clippedPosition)
  }

  private def clipToRectangle(vector: Vector2, rectangle: Rectangle): Vector2 = {
    val xClipped = clip(vector.x, worldSize.xMin, worldSize.xMax)
    val yClipped = clip(vector.y, worldSize.yMin, worldSize.yMax)
    Vector2(xClipped, yClipped)
  }

  private def clip(value: Double, min: Double, max: Double): Double =
    if (value <= min) min
    else if (value >= max) max
    else value

  /** Order the drone to stop moving. */
  def halt(): Unit = {
    drone ! HoldPosition
  }

  /** Order the drone to harvest `mineralCrystal`.
    * Must already be close to the position of the `mineralCrystal`.
    */
  def harvest(mineralCrystal: MineralCrystal): Unit =
    drone ! HarvestMineral(mineralCrystal.mineralCrystal)

  /** Order the drone to give all its resources to `otherDrone`.
    *
    * @param otherDrone The drone which will receive the resources.
    */
  def giveResourcesTo(otherDrone: Drone): Unit = {
    // TODO: isDead checks are all in this class, since they may cause replay errors if the command is recorded. Restructure would be good to make things less cluttered.
    if (otherDrone.isDead) {
      drone.warn("Trying to give minerals to a drone that does not exist anymore!")
    } else {
      drone ! DepositMinerals(otherDrone.drone)
    }
  }

  /**
    * Order the drone to start the construction of a new drone.
    * While construction is in progress, the drone cannot move.
    *
    * @param controller The drone controller that will govern the behaviour of the new drone.
    * @param spec The specification for the number of copies for each module equipped by the new drone.
    */
  def buildDrone(controller: DroneControllerBase, spec: DroneSpec): Unit = {
    def cap(value: Double, min: Double, max: Double): Double =
      math.min(math.max(value, min), max)
    val pos = drone.position - 110 * Rng.vector2()
    val cappedPos = Vector2(
      cap(pos.x, worldSize.xMin, worldSize.xMax),
      cap(pos.y, worldSize.yMin, worldSize.yMax)
    )
    drone ! ConstructDrone(spec, controller, drone.position - 110 * Vector2(_drone.dynamics.orientation))
  }

  /** Order the drone to start the construction of a new drone.
    * While construction is in progress, the drone cannot move.
    *
    * @param controller The drone controller that will govern the behaviour of the new drone.
    * @param storageModules The new drone's number of storage modules.
    * @param missileBatteries The new drone's number of missile batteries.
    * @param constructors The new drone's number of constructors.
    * @param engines The new drone's number of engines.
    * @param shieldGenerators The new drone's number of shield generators.
    */
  def buildDrone(
    controller: DroneControllerBase,
    storageModules: Int = 0,
    missileBatteries: Int = 0,
    constructors: Int = 0,
    engines: Int = 0,
    shieldGenerators: Int = 0
  ): Unit = {
    val spec = new DroneSpec(storageModules, missileBatteries, constructors, engines, shieldGenerators)
    buildDrone(controller, spec)
  }

  /** Fires all homing missiles at `target`. */
  def fireMissilesAt(target: Drone): Unit = {
    if (target.isDead) {
      drone.warn("Trying to fireMissilesAt a drone that does not exist anymore!")
    } else {
      drone ! FireMissiles(target.drone)
    }
  }

  /** Always returns true */
  override def isVisible: Boolean = true

  /** Always returns false. */
  override def isEnemy: Boolean = false

  override def lastKnownPosition: Vector2 = position

  override def lastKnownOrientation: Double = orientation

  @inline final override private[core] def drone: DroneImpl = _drone

  /** Returns true if `otherDrone` is within range of this drones homing missiles, otherwise false. */
  def isInMissileRange(otherDrone: Drone): Boolean =
    (otherDrone.position - drone.position).lengthSquared <= MissileLockOnRange * MissileLockOnRange

  /** Returns true if `mineralCrystal` is within harvesting range, otherwise false. */
  def isInHarvestingRange(mineralCrystal: MineralCrystal): Boolean =
    drone.isInHarvestingRange(mineralCrystal.mineralCrystal)

  @inline private[core] final def dronesInSightScala: Set[Drone] = drone.dronesInSight
  @inline private[core] final def alliesInSightScala: Set[Drone] = drone.alliesInSight
  @inline private[core] final def enemiesInSightScala: Set[Drone] = drone.enemiesInSight

  /** Returns the confines of the game world. */
  def worldSize: Rectangle = drone.context.worldConfig.size

  private[core] def willProcessEvents(): Unit = {}

  private[core] def initialise(drone: DroneImpl): Unit = {
    require(_drone == null, "DroneController must only be initialised once.")
    _drone = drone
  }

  @deprecated("Use giveResourcesTo instead.", "0.2.4.3")
  def giveMineralsTo(otherDrone: Drone): Unit = {
    giveResourcesTo(otherDrone)
  }

  /** Order the drone to start the construction of a new drone.
    * While construction is in progress, the drone cannot move.
    *
    * @param spec The specification for the number of copies for each module equipped by the new drone.
    * @param controller The drone controller that will govern the behaviour of the new drone.
    */
  @deprecated("Swap the positions of the `spec` and `controller` arguments.", "0.2.4.3")
  def buildDrone(spec: DroneSpec, controller: DroneControllerBase): Unit =
    buildDrone(controller, spec)

  /** Returns 0. */
  @deprecated("The refinery module has been removed.", "0.2.4.0")
  def availableRefineries: Int = 0
}

