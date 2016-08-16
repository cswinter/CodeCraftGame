package cwinter.codecraft.core.game

import cwinter.codecraft.core.objects.drone.DroneCommand

import scala.collection.mutable.ListBuffer


private[core] class CommandRecorder {
  private val commands = ListBuffer.empty[(Int, DroneCommand)]

  def record(droneID: Int, droneCommand: DroneCommand): Unit = {
    commands.append((droneID, droneCommand))
  }

  def popAll(): Seq[(Int, DroneCommand)] = {
    val result = commands.toList
    commands.clear()
    result
  }
}

