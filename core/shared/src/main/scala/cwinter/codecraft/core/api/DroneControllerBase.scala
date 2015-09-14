package cwinter.codecraft.core.api

import cwinter.codecraft.core.objects.drone._
import cwinter.codecraft.graphics.worldstate.Player
import cwinter.codecraft.util.maths.{Rectangle, Rng, Vector2}

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
trait DroneControllerBase extends Drone {
  private[this] var _drone: DroneImpl = null

  // abstract methods for event handling
  /**
   * Called once when the drone is spawned. Called before any other onEvent method is called.
   */
  def onSpawn(): Unit = ()

  /**
   * Called if the drone is destroyed.
   */
  def onDeath(): Unit = ()

  /**
   * Called once every tick, after all other event methods have been called.
   */
  def onTick(): Unit = ()

  /**
   * Called when a mineral crystal enters the sight radius of this drone.
   * @param mineralCrystal The mineral crystal that is now in sight.
   */
  def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = ()

  /**
   * Called when a drone enters the sight radius of this drone.
   * @param drone The drone that is now in sight.
   */
  def onDroneEntersVision(drone: Drone): Unit = ()

  /**
   * Called when this drone arrives a position after invoking the moveTo(position: Vector2) command.
   */
  def onArrivesAtPosition(): Unit = ()

  /**
   * Called when this drone arrives at a mineral crystal after invoking the moveTo(mineral: MineralCrystalHandle) command.
   * @param mineralCrystal
   */
  def onArrivesAtMineral(mineralCrystal: MineralCrystal): Unit = ()

  /**
   * Called when this drone arrives at another drone after invoking the moveTo(drone: DroneHandle) command.
   * @param drone The drone at which this drone has arrived.
   */
  def onArrivesAtDrone(drone: Drone): Unit = ()

  // drone commands
  /**
   * This drone will keep moving in the direction of `directionVector`.
   * @param directionVector The direction to move in.
   */
  def moveInDirection(directionVector: Vector2): Unit = {
    drone ! MoveInDirection(directionVector.orientation)
  }

  /**
   * This drone will keep moving in the direction of `direction`.
   * @param direction The direction to be moved in in radians.
   */
  def moveInDirection(direction: Double): Unit = {
    drone ! MoveInDirection(direction)
  }

  /**
   * This drone will move towards `otherDrone`, until it is within 10 units length of colliding.
   * @param otherDrone The drone to be moved towards.
   */
  def moveTo(otherDrone: Drone): Unit = {
    drone ! MoveToDrone(otherDrone.drone)
  }

  /**
   * This drone will move towards `mineralCrystal`.
   * @param mineralCrystal The mineral crystal to be moved towards.
   */
  def moveTo(mineralCrystal: MineralCrystal): Unit = {
    drone ! MoveToMineralCrystal(mineralCrystal.mineralCrystal)
  }

  /**
   * This drone will move to `position`.
   * @param position The position to be moved towards.
   */
  def moveTo(position: Vector2): Unit = {
    drone ! MoveToPosition(position)
  }

  /**
   * This drone will  move to the coordinate (`x`, `y`).
   */
  def moveTo(x: Double, y: Double): Unit = {
    moveTo(Vector2(x, y))
  }

  /**
   * Drone will harvest this mineral crystal. Must already be at the position of the mineral crystal.
   * @param mineralCrystal The mineral crystal to be harvested.
   */
  def harvest(mineralCrystal: MineralCrystal): Unit = {
    drone ! HarvestMineral(mineralCrystal.mineralCrystal)
  }

  /**
   * Gives all minerals stored in this drone to `otherDrone`.
   * @param otherDrone The drone which will receive the minerals.
   */
  def giveMineralsTo(otherDrone: Drone): Unit = {
    drone ! DepositMinerals(otherDrone.drone)
  }

  /**
   * Starts construction of a new drone.
   * @param spec The specification for the modules equipped by the new drone.
   * @param controller The drone controller that will govern the behaviour of the new drone.
   */
  def buildDrone(spec: DroneSpec, controller: DroneControllerBase): Unit = {
    drone ! ConstructDrone(spec, controller, drone.position - 110 * Rng.vector2())
  }

  def buildDrone(
    controller: DroneController,
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
   * @param target The drone to be shot.
   */
  def fireMissilesAt(target: Drone): Unit = {
    drone ! FireMissiles(target.drone)
  }

  // drone properties
  /**
   * Returns the current position of this drone.
   */
  override def position: Vector2 = drone.position

  /**
   * The cooldown of the missile batteries.
   *
   * Cooldown decreases by 1 each turn, and you can only fire once cooldown reaches 0.
   */
  override def weaponsCooldown: Int = drone.weaponsCooldown

  /**
   * Always true, since this is one of your own drones.
   */
  override def isVisible: Boolean = true

  /**
   * Returns the module specification of this drone.
   */
  override def spec: DroneSpec = if (drone == null) null else drone.spec

  /**
   * Returns the player that commands this drone.
   */
  override def player: Player = drone.player

  /**
   * Returns the number of hitpoints this drone has left.
   *
   * Once hitpoints reach zero, this drone dies.
   */
  override def hitpoints: Int = drone.hitpoints

  /**
   * Always false, since this is one of your own drones.
   */
  override def isEnemy: Boolean = false
  @inline final override private[core] def drone: DroneImpl = _drone

  /**
   * Returns true if `otherDrone` is within range of this drones missiles, otherwise false.
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
   * Returns the number of unused refinery modules.
   */
  def availableRefineries: Int = drone.availableFactories
  // TODO: make this O(1)
  private[core] def storedMineralsScala: Seq[MineralCrystal] =
    drone.storedMinerals.toSeq.map(new MineralCrystal(_, player))
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


