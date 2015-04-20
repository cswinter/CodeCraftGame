package cwinter.codinggame.core

import cwinter.codinggame.maths.Vector2

abstract class DroneController {
  private var drone: Drone = null

  // abstract methods for event handling
  def onSpawn(): Unit
  def onTick(): Unit


  // wrapper around drone properties and commands
  def moveInDirection(direction: Vector2): Unit = {
    drone.moveInDirection(direction)
  }

  private def initialise(drone: Drone): Unit = this.drone = drone
}
