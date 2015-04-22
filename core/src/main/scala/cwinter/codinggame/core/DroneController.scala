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
    drone.command_=(MoveInDirection(direction))
  }

  def moveToPosition(position: Vector2): Unit = {
    drone.command_=(MoveToPosition(position))
  }

  def harvestMineral(mineralCrystal: MineralCrystal): Unit = {
    drone.command_=(HarvestMineralCrystal(mineralCrystal))
  }

  def position: Vector2 = drone.position
  def availableStorage: Int = drone.availableStorage

  private[core] def initialise(drone: Drone): Unit = this.drone = drone
}
