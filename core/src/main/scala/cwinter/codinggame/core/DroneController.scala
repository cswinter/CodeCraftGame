package cwinter.codinggame.core

import cwinter.codinggame.util.maths.Vector2

abstract class DroneController {
  private var drone: Drone = null

  // abstract methods for event handling
  def onSpawn(): Unit
  def onTick(): Unit
  def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit
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
    drone.giveMovementCommand(HarvestMineralCrystal(mineralCrystal))
  }

  def depositMineralCrystals(otherDrone: DroneController): Unit = {
    drone.giveMovementCommand(DepositMineralCrystals(otherDrone.drone))
  }

  def buildSmallDrone(module1: Module, module2: Module, controller: DroneController): Unit = {
    val newDrone = new Drone(Seq(module1, module2), 4, controller, Vector2.NullVector, -1)
    drone.startDroneConstruction(ConstructDrone(newDrone))
  }

  def buildTinyDrone(module: Module, controller: DroneController): Unit = {
    val newDrone = new Drone(Seq(module), 3, controller, Vector2.NullVector, -1)
    drone.startDroneConstruction(ConstructDrone(newDrone))
  }

  def processMineral(mineralCrystal: MineralCrystal): Unit = {
    drone.startMineralProcessing(mineralCrystal)
  }

  def position: Vector2 = drone.position
  def availableStorage: Int = drone.availableStorage
  def availableFactories: Int = drone.availableFactories
  def storedMinerals: Seq[MineralCrystal] = drone.storedMinerals

  private[core] def initialise(drone: Drone): Unit = this.drone = drone
}
