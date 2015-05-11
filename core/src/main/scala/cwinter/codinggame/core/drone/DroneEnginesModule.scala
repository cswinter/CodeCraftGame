package cwinter.codinggame.core.drone

import cwinter.codinggame.core.{SimulatorEvent, SpawnLaserMissile}
import cwinter.codinggame.worldstate.EnginesDescriptor

class DroneEnginesModule(positions: Seq[Int], owner: Drone)
  extends DroneModule(positions, owner) {

  override def update(availableResources: Int): (Seq[SimulatorEvent], Int) = NoEffects

  override def descriptors: Seq[cwinter.codinggame.worldstate.DroneModuleDescriptor] = positions.map(EnginesDescriptor)
}

