package cwinter.codinggame.core.objects.drone

import cwinter.codinggame.core.errors.Errors
import cwinter.codinggame.core.objects.MineralCrystal
import cwinter.codinggame.util.maths.Vector2
import cwinter.codinggame.worldstate.Player

abstract class DroneController extends DroneHandle {
  private[this] var _drone: Drone = null

  // abstract methods for event handling
  def onSpawn(): Unit
  def onDeath(): Unit
  def onTick(): Unit
  def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit
  def onDroneEntersVision(drone: Drone): Unit
  def onArrival(): Unit

  // drone commands
  def moveInDirection(direction: Vector2): Unit = {
    drone.giveMovementCommand(MoveInDirection(direction))
  }

  def moveToDrone(otherDrone: DroneController): Unit = {
    val other = otherDrone.drone
    val targetDirection = (other.position - position).normalized
    val targetPos = other.position - (other.radius + drone.radius + 10) * targetDirection
    moveToPosition(targetPos)
  }

  def moveToPosition(position: Vector2): Unit = {
    drone.giveMovementCommand(MoveToPosition(position))
  }

  def harvestMineral(mineralCrystal: MineralCrystal): Unit = {
    drone.harvestResource(mineralCrystal)
  }

  def depositMineralCrystals(otherDrone: DroneController): Unit = {
    drone.depositMinerals(otherDrone.drone)
  }

  def buildDrone(spec: DroneSpec, controller: DroneController): Unit = {
    val newDrone = new Drone(spec, controller, drone.player, Vector2.NullVector, -1)
    drone.startDroneConstruction(ConstructDrone(newDrone))
  }

  def processMineral(mineralCrystal: MineralCrystal): Unit = {
    drone.startMineralProcessing(mineralCrystal)
  }

  def shootWeapons(target: DroneHandle): Unit = {
    if (target.drone == drone) {
      Errors.warn("Drone tried to shoot itself!", drone.position)
    } else {
      drone.fireWeapons(target.drone)
    }
  }

  // drone properties
  override def position: Vector2 = drone.position
  override def weaponsCooldown: Int = drone.weaponsCooldown
  override def isVisible: Boolean = true
  override def spec: DroneSpec = drone.spec
  override def player: Player = drone.player
  @inline final override private[core] def drone: Drone = _drone

  def isConstructing: Boolean = drone.isConstructing
  def availableStorage: Int = drone.availableStorage
  def availableFactories: Int = drone.availableFactories
  def storedMinerals: Seq[MineralCrystal] = drone.storedMinerals.toSeq // TODO: remove this conversion
  def dronesInSight: Set[DroneHandle] = drone.dronesInSight.map( d =>
      if (d.player == drone.player) d.controller
      else new EnemyDroneHandle(d, drone.player)   // TODO: maybe create drone handles once for each player
    )


  private[core] def initialise(drone: Drone): Unit = {
    require(_drone == null)
    _drone = drone
  }
}


