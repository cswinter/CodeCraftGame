package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.{SimulatorEvent, SpawnHomingMissile}
import cwinter.codecraft.util.maths.Vector2
import cwinter.codecraft.worldstate.EnginesDescriptor

class DroneEnginesModule(positions: Seq[Int], owner: DroneImpl)
  extends DroneModule(positions, owner) {

  override def update(availableResources: Int): (Seq[SimulatorEvent], Seq[Vector2], Seq[Vector2]) = NoEffects

  override def descriptors: Seq[cwinter.codecraft.worldstate.DroneModuleDescriptor] = positions.map(EnginesDescriptor)
}

