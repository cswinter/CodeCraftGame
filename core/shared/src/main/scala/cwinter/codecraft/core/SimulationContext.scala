package cwinter.codecraft.core

import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.core.objects.drone.DroneImpl


case class SimulationContext(
  droneRegistry: Map[Int, DroneImpl],
  mineralRegistry: Map[Int, MineralCrystalImpl]
) {
  def drone(id: Int): DroneImpl = droneRegistry(id)
  def mineral(id: Int): MineralCrystalImpl = mineralRegistry(id)
}
