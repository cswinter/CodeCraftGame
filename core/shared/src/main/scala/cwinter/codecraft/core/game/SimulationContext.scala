package cwinter.codecraft.core.game

import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.core.objects.drone.DroneImpl


private[codecraft] case class SimulationContext(
  droneRegistry: Map[Int, DroneImpl],
  mineralRegistry: Map[Int, MineralCrystalImpl],
  timestep: Int
) {
  def drone(id: Int): DroneImpl = droneRegistry(id)
  def mineral(id: Int): MineralCrystalImpl = mineralRegistry(id)
}
