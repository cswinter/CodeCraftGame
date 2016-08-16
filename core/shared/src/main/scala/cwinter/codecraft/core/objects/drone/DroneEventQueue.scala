package cwinter.codecraft.core.objects.drone


private[core] trait DroneEventQueue { self: DroneImpl =>
  private[this] val eventQueue = collection.mutable.Queue[DroneEvent](Spawned)
  private[this] var t = 0

  def processEvents(): Unit = {
    resetMessageDisplay()
    controller.willProcessEvents()

    t += 1
    if (isDead) controller.onDeath()
    else {
      eventQueue foreach {
        case Destroyed => // this should never be executed
        case MineralEntersSightRadius(mineral) =>
          controller.onMineralEntersVision(mineral.getHandle(player))
        case ArrivedAtPosition => controller.onArrivesAtPosition()
        case ArrivedAtDrone(drone) => controller.onArrivesAtDrone(drone.wrapperFor(player))
        case ArrivedAtMineral(mineral) => controller.onArrivesAtMineral(mineral.getHandle(player))
        case DroneEntersSightRadius(drone) => controller.onDroneEntersVision(drone.wrapperFor(player))
        case Spawned => // handled by simulator to ensure onSpawn is called before any other events
        case event => throw new Exception(s"Unhandled event! $event")
      }
      eventQueue.clear()
      controller.onTick()
    }
  }

  private[core] def enqueueEvent(event: DroneEvent): Unit = eventQueue.enqueue(event)
}
