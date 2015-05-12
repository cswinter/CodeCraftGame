package cwinter.codinggame.core.drone

import cwinter.codinggame.core.{SimulatorEvent, SpawnLaserMissile}
import cwinter.codinggame.util.maths.Vector2
import cwinter.codinggame.worldstate.EnginesDescriptor

class DroneEnginesModule(positions: Seq[Int], owner: Drone)
  extends DroneModule(positions, owner) {

  override def update(availableResources: Int): (Seq[SimulatorEvent], Seq[Vector2], Seq[Vector2]) = NoEffects

  override def descriptors: Seq[cwinter.codinggame.worldstate.DroneModuleDescriptor] = positions.map(EnginesDescriptor)
}

