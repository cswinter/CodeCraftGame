package cwinter.codecraft.core

import cwinter.codecraft.core.objects.drone.DroneCommand


private[core] class CommandRecorder {
  private var commands = List.empty[(Int, DroneCommand)]

  def record(droneID: Int, droneCommand: DroneCommand): Unit = {
    commands ::= ((droneID, droneCommand))
  }

  def popAll(): Seq[(Int, DroneCommand)] = {
    val tmp = commands
    commands = List.empty[(Int, DroneCommand)]
    tmp
  }
}

