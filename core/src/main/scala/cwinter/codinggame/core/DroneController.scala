package cwinter.codinggame.core

import cwinter.codinggame.maths.Vector2

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

  def moveToPosition(position: Vector2): Unit = {
    drone.giveMovementCommand(MoveToPosition(position))
  }

  def harvestMineral(mineralCrystal: MineralCrystal): Unit = {
    drone.giveMovementCommand(HarvestMineralCrystal(mineralCrystal))
  }

  def buildSmallDrone(module1: Module, module2: Module): Unit = {
    drone.startDroneConstruction(ConstructSmallDrone(module1, module2))
  }

  def position: Vector2 = drone.position
  def availableStorage: Int = drone.availableStorage

  private[core] def initialise(drone: Drone): Unit = this.drone = drone
}
