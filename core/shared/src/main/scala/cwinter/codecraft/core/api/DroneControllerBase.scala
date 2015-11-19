package cwinter.codecraft.core.api

import cwinter.codecraft.core.objects.drone._
import cwinter.codecraft.util.maths.{Rectangle, Rng, Vector2}

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

  // abstract methods for event handling
  /**
   * Called once when the drone is spawned. Called before any other `onEvent` method is called.
   */
  def onSpawn(): Unit = ()

  /**
   * Called if the drone is destroyed.
   */
  def onDeath(): Unit = ()

  /**
   * Called once every tick, after all other `onEvent` methods have been called.
   */
  def onTick(): Unit = ()

  /**
   * Called when a mineral crystal enters the sight radius of this drone.
   *
   * @param mineralCrystal The [[MineralCrystal]] that is now in sight.
   */
  def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = ()

  /**
   * Called when another drone enters the sight radius of this drone.
   *
   * @param drone The [[Drone]] that is now in sight.
   */
  def onDroneEntersVision(drone: Drone): Unit = ()

  /**
   * Called when this drone arrives a position after invoking the one of the `moveTo` methods.
   */
  def onArrivesAtPosition(): Unit = ()

  /**
   * Called when this drone arrives at a mineral crystal after invoking the moveTo(mineral: MineralCrystalHandle) command.
   *
   * @param mineralCrystal The [[MineralCrystal]] that the drone arrived at.
   */
  def onArrivesAtMineral(mineralCrystal: MineralCrystal): Unit = ()

  /**
   * Called when this drone arrives at another drone after invoking the moveTo(drone: DroneHandle) command.
   *
   * @param drone The [[Drone]] at which this drone has arrived.
   */
  def onArrivesAtDrone(drone: Drone): Unit = ()

  // drone commands
  /**
   * Order the drone to keep moving in the direction of `directionVector`.
   *
   * @param directionVector The direction to move in.
   */
  def moveInDirection(directionVector: Vector2): Unit = {
    drone ! MoveInDirection(directionVector.orientation)
  }

  /**
   * Order the drone to keep moving in the direction of `direction`.
   *
   * @param direction The direction to be moved in in radians.
   */
  def moveInDirection(direction: Double): Unit = {
    drone ! MoveInDirection(direction)
  }

  /**
   * Order the drone to move towards `otherDrone`, until it is within 10 units distance of colliding.
   *
   * @param otherDrone The drone to be moved towards.
   */
  def moveTo(otherDrone: Drone): Unit = {
    // TODO: most validation is in DroneImpl, maybe make consistent?
    if (otherDrone.isDead) {
      drone.warn("Trying to moveTo a dead drone!")
    } else {
      drone ! MoveToDrone(otherDrone.drone)
    }
  }

  /**
   * Order the drone to move towards `mineralCrystal`.
   *
   * @param mineralCrystal The [[MineralCrystal]] to be moved towards.
   */
  def moveTo(mineralCrystal: MineralCrystal): Unit = {
    drone ! MoveToMineralCrystal(mineralCrystal.mineralCrystal)
  }

  /**
   * Order the drone to move to `position`.
   *
   * @param position The position to be moved towards.
   */
  def moveTo(position: Vector2): Unit = {
    drone ! MoveToPosition(position)
  }

  /**
   * Order the drone to move to coordinates (`x`, `y`).
   */
  def moveTo(x: Double, y: Double): Unit = {
    moveTo(Vector2(x, y))
  }

  /**
   * Order the drone to harvest `mineralCrystal`.
   * Must already be at the position of the `mineralCrystal`.
   *
   * @param mineralCrystal The mineral crystal to be harvested.
   */
  def harvest(mineralCrystal: MineralCrystal): Unit = {
    drone ! HarvestMineral(mineralCrystal.mineralCrystal)
  }

  /**
   * Order the drone to give all its minerals to `otherDrone`.
   *
   * @param otherDrone The drone which will receive the minerals.
   */
  def giveMineralsTo(otherDrone: Drone): Unit = {
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
   * @param spec The specification for the number of copies for each module equipped by the new drone.
   * @param controller The drone controller that will govern the behaviour of the new drone.
   */
  def buildDrone(spec: DroneSpec, controller: DroneControllerBase): Unit = {
    drone ! ConstructDrone(spec, controller, drone.position - 110 * Rng.vector2())
  }

  /**
   * Order the drone to start the construction of a new drone.
   * While construction is in progress, the drone cannot move.
   *
   * @param controller The drone controller that will govern the behaviour of the new drone.
   * @param storageModules The new drone's number of storage modules.
   * @param missileBatteries The new drone's number of missile batteries.
   * @param refineries The new drone's number of refineries.
   * @param constructors The new drone's number of constructors.
   * @param engines The new drone's number of engines.
   * @param shieldGenerators The new drone's number of shield generators.
   */
  def buildDrone(
    controller: DroneControllerBase,
    storageModules: Int = 0,
    missileBatteries: Int = 0,
    refineries: Int = 0,
    constructors: Int = 0,
    engines: Int = 0,
    shieldGenerators: Int = 0
  ): Unit = {
    val spec = new DroneSpec(storageModules, missileBatteries, refineries, constructors, engines, shieldGenerators)
    buildDrone(spec, controller)
  }

  /**
   * Fires all homing missiles at `target`.
   *
   * @param target The drone to be shot at.
   */
  def fireMissilesAt(target: Drone): Unit = {
    drone ! FireMissiles(target.drone)
  }

  // drone properties
  override def position: Vector2 = drone.position

  override def weaponsCooldown: Int = drone.weaponsCooldown

  override def isVisible: Boolean = true

  override def spec: DroneSpec = if (drone == null) null else drone.spec

  override def playerID: Int = drone.player.id

  override def hitpoints: Int = drone.hitpoints

  /**
   * Always returns false.
   */
  override def isEnemy: Boolean = false

  @inline final override private[core] def drone: DroneImpl = _drone

  /**
   * Returns true if `otherDrone` is within range of this drones homing missiles, otherwise false.
   *
   * @param otherDrone The drone you want to shoot at.
   */
  def isInMissileRange(otherDrone: Drone): Boolean =
    (otherDrone.position - drone.position).lengthSquared <=
      DroneConstants.MissileLockOnRadius * DroneConstants.MissileLockOnRadius

  /**
   * Returns true if this drone is currently constructing another drone.
   */
  def isConstructing: Boolean = drone.isConstructing

  /**
   * Returns the number of free storage modules.
   */
  def availableStorage: Int = drone.availableStorage

  /**
   * Returns the total amount of resources available to this drone.
   * If the drone has refineries, this includes unprocessed mineral crystals.
   */
  def totalAvailableResources: Int = drone.totalAvailableResources

  /**
   * Returns the number of unused refinery modules.
   */
  def availableRefineries: Int = drone.availableFactories

  // TODO: make this O(1)
  private[core] def storedMineralsScala: Seq[MineralCrystal] =
    drone.storedMinerals.toSeq.map(new MineralCrystal(_, drone.player))
  private[core] def dronesInSightScala: Set[Drone] = drone.dronesInSight.map( d =>
      if (d.player == drone.player) d.controller
      else new EnemyDrone(d, drone.player)   // TODO: maybe create drone handles once for each player
    )

  /**
   * Returns the confines of the game world.
   */
  def worldSize: Rectangle = drone.worldConfig.size

  /**
   * Returns the drones orientation in radians.
   */
  def orientation: Double = drone.dynamics.orientation


  private[core] def initialise(drone: DroneImpl): Unit = {
    require(_drone == null, "DroneController must only be initialised once.")
    _drone = drone
  }
}


