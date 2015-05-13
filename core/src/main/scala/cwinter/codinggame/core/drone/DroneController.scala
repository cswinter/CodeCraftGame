package cwinter.codinggame.core.drone

import cwinter.codinggame.core.MineralCrystal
import cwinter.codinggame.util.maths.Vector2

abstract class DroneController {
  // TODO: make private again once drone handles exist
  var drone: Drone = null

  // abstract methods for event handling
  def onSpawn(): Unit
  def onDeath(): Unit
  def onTick(): Unit
  def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit
  def onDroneEntersVision(drone: Drone): Unit
  def onArrival(): Unit

  // wrapper around drone properties and commands
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

  def shootWeapons(target: Drone): Unit = {
    assert(target != drone)
    drone.fireWeapons(target)
  }

  def isConstructing: Boolean = drone.isConstructing
  def position: Vector2 = drone.position
  def availableStorage: Int = drone.availableStorage
  def availableFactories: Int = drone.availableFactories
  def storedMinerals: Seq[MineralCrystal] = drone.storedMinerals.toSeq // TODO: remove this conversion
  def dronesInSight: Set[Drone] = drone.dronesInSight
  def weaponsCooldown: Int = drone.weaponsCooldown

  private[core] def initialise(drone: Drone): Unit = this.drone = drone
}
