package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.SimulationContext
import cwinter.codecraft.core.objects.MineralCrystalImpl
import upickle.default.key


private[core] sealed trait DroneEvent {
  def toSerializable: SerializableDroneEvent
}

private[core] sealed trait SerializableDroneEvent {
  def toSerializable: SerializableDroneEvent = this
}

@key("SeesMineral") private[core] case class SerializableMineralEntersSightRadius(mineralID: Int) extends SerializableDroneEvent
@key("ReachesMineral") private[core] case class SerializableArrivedAtMineral(mineralID: Int) extends SerializableDroneEvent
@key("ReachesDrone") private[core] case class SerializableArrivedAtDrone(droneID: Int) extends SerializableDroneEvent
@key("SeesDrone") private[core] case class SerializableDroneEntersSightRadius(droneID: Int) extends SerializableDroneEvent
@key("Spawned") private[core] case object Spawned extends DroneEvent with SerializableDroneEvent
@key("Destroyed") private[core] case object Destroyed extends DroneEvent with SerializableDroneEvent

private[core] case class MineralEntersSightRadius(mineralCrystal: MineralCrystalImpl) extends DroneEvent {
  override def toSerializable: SerializableDroneEvent =
    SerializableMineralEntersSightRadius(mineralCrystal.id)
}
@key("ArrivesPosition") private[core] case object ArrivedAtPosition extends DroneEvent with SerializableDroneEvent
private[core] case class ArrivedAtMineral(mineral: MineralCrystalImpl) extends DroneEvent {
  override def toSerializable: SerializableDroneEvent =
    SerializableArrivedAtMineral(mineral.id)
}
private[core] case class ArrivedAtDrone(drone: DroneImpl) extends DroneEvent {
  override def toSerializable: SerializableDroneEvent =
    SerializableArrivedAtDrone(drone.id)
}
private[core] case class DroneEntersSightRadius(drone: DroneImpl) extends DroneEvent {
  override def toSerializable: SerializableDroneEvent =
    SerializableDroneEntersSightRadius(drone.id)
}

private[core] object DroneEvent {
  def apply(serialized: SerializableDroneEvent)(
    implicit context: SimulationContext
  ): DroneEvent = serialized match {
    case SerializableMineralEntersSightRadius(id) => MineralEntersSightRadius(context.mineral(id))
    case SerializableArrivedAtMineral(id) => ArrivedAtMineral(context.mineral(id))
    case SerializableArrivedAtDrone(id) => ArrivedAtDrone(context.drone(id))
    case SerializableDroneEntersSightRadius(id) => DroneEntersSightRadius(context.drone(id))
    case Spawned => Spawned
    case Destroyed => Destroyed
    case ArrivedAtPosition => ArrivedAtPosition
  }
}



