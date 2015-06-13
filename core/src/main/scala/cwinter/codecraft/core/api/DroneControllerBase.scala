package cwinter.codecraft.core.api

import cwinter.codecraft.core.objects.drone._
import cwinter.codecraft.util.maths.{Rectangle, Rng, Vector2}
import cwinter.codecraft.worldstate.Player

private[core] abstract class DroneControllerBase extends DroneHandle {
  private[this] var _drone: Drone = null

  // abstract methods for event handling
  def onSpawn(): Unit = ()
  def onDeath(): Unit = ()
  def onTick(): Unit = ()
  def onMineralEntersVision(mineralCrystal: MineralCrystalHandle): Unit = ()
  def onDroneEntersVision(drone: DroneHandle): Unit = ()
  def onArrivesAtPosition(): Unit = ()
  def onArrivesAtMineral(mineralCrystal: MineralCrystalHandle): Unit = ()
  def onArrivesAtDrone(drone: DroneHandle): Unit = ()

  // drone commands
  def moveInDirection(directionVector: Vector2): Unit = {
    drone ! MoveInDirection(directionVector.orientation)
  }

  def moveInDirection(direction: Double): Unit = {
    drone ! MoveInDirection(direction)
  }

  def moveTo(otherDrone: DroneHandle): Unit = {
    drone ! MoveToDrone(otherDrone.drone)
  }
  
  def moveTo(mineralCrystal: MineralCrystalHandle): Unit = {
    drone ! MoveToMineralCrystal(mineralCrystal.mineralCrystal)
  }

  def moveTo(position: Vector2): Unit = {
    drone ! MoveToPosition(position)
  }

  def harvest(mineralCrystal: MineralCrystalHandle): Unit = {
    drone ! HarvestMineral(mineralCrystal.mineralCrystal)
  }

  def giveMineralsTo(otherDrone: DroneHandle): Unit = {
    drone ! DepositMinerals(otherDrone.drone)
  }

  def buildDrone(spec: DroneSpec, controller: DroneControllerBase): Unit = {
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
    (droneHandle.position - drone.position).lengthSquared <=
      DroneConstants.MissileLockOnRadius * DroneConstants.MissileLockOnRadius
  def isConstructing: Boolean = drone.isConstructing
  def availableStorage: Int = drone.availableStorage
  def availableFactories: Int = drone.availableFactories
  // TODO: make this O(1)
  private[core] def storedMineralsScala: Seq[MineralCrystalHandle] =
    drone.storedMinerals.toSeq.map(new MineralCrystalHandle(_, player))
  private[core] def dronesInSightScala: Set[DroneHandle] = drone.dronesInSight.map( d =>
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


