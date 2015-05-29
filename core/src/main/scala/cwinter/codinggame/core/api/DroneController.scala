package cwinter.codinggame.core.api

import cwinter.codinggame.core.errors.{CodingGameException, Errors}
import cwinter.codinggame.core.objects.drone._
import cwinter.codinggame.graphics.models.DroneMissileBatteryModelBuilder
import cwinter.codinggame.util.maths.{Rng, Rectangle, Vector2}
import cwinter.codinggame.worldstate.Player

abstract class DroneController extends DroneHandle {
  private[this] var _drone: Drone = null

  // abstract methods for event handling
  def onSpawn(): Unit
  def onDeath(): Unit
  def onTick(): Unit
  def onMineralEntersVision(mineralCrystal: MineralCrystalHandle): Unit
  def onDroneEntersVision(drone: DroneHandle): Unit
  def onArrival(): Unit

  // drone commands
  def moveInDirection(direction: Vector2): Unit = {
    drone ! MoveInDirection(direction)
  }

  def moveToDrone(otherDrone: DroneHandle): Unit = {
    val other = otherDrone.drone
    val targetDirection = (other.position - position).normalized
    val targetPos = other.position - (other.radius + drone.radius + 10) * targetDirection
    moveToPosition(targetPos)
  }
  
  def moveToMineral(mineralCrystal: MineralCrystalHandle): Unit = {
    drone ! MoveToPosition(mineralCrystal.position)
  }

  def moveToPosition(position: Vector2): Unit = {
    drone ! MoveToPosition(position)
  }

  def harvestMineral(mineralCrystal: MineralCrystalHandle): Unit = {
    drone ! HarvestMineral(mineralCrystal.mineralCrystal)
  }

  def depositMinerals(otherDrone: DroneHandle): Unit = {
    drone ! DepositMinerals(otherDrone.drone)
  }

  def buildDrone(spec: DroneSpec, controller: DroneController): Unit = {
    drone ! ConstructDrone(spec, controller, drone.position - 110 * Rng.vector2())
  }

  def processMineral(mineralCrystal: MineralCrystalHandle): Unit = {
    drone ! ProcessMineral(mineralCrystal.mineralCrystal)
  }

  def shootMissiles(target: DroneHandle): Unit = {
    drone ! FireMissiles(target.drone)
  }

  // drone properties
  override def position: Vector2 = drone.position
  override def weaponsCooldown: Int = drone.weaponsCooldown
  override def isVisible: Boolean = true
  override def spec: DroneSpec = drone.spec
  override def player: Player = drone.player
  override def hitpoints: Int = drone.hitpoints
  override def isEnemy: Boolean = false
  @inline final override private[core] def drone: Drone = _drone

  def isInMissileRange(droneHandle: DroneHandle): Boolean =
    (droneHandle.position - drone.position).magnitudeSquared <=
      DroneConstants.MissileLockOnRadius * DroneConstants.MissileLockOnRadius
  def isConstructing: Boolean = drone.isConstructing
  def availableStorage: Int = drone.availableStorage
  def availableFactories: Int = drone.availableFactories
  // TODO: make this O(1)
  def storedMinerals: Seq[MineralCrystalHandle] =
    drone.storedMinerals.toSeq.map(new MineralCrystalHandle(_, player))
  def dronesInSight: Set[DroneHandle] = drone.dronesInSight.map( d =>
      if (d.player == drone.player) d.controller
      else new EnemyDroneHandle(d, drone.player)   // TODO: maybe create drone handles once for each player
    )
  def worldSize: Rectangle = drone.worldConfig.size
  def orientation: Double = drone.dynamics.orientation


  private[core] def initialise(drone: Drone): Unit = {
    require(_drone == null, "DroneController must only be initialised once.")
    _drone = drone
  }
}


