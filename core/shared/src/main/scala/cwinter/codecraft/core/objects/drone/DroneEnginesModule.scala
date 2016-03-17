package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.{SimulatorEvent, SpawnHomingMissile}
import cwinter.codecraft.graphics.worldstate.{EnginesDescriptor, DroneModuleDescriptor}
import cwinter.codecraft.util.maths.Vector2

private[core] class DroneEnginesModule(positions: Seq[Int], owner: DroneImpl)
  extends DroneModule(positions, owner) {

  override def update(availableResources: Int): (Seq[SimulatorEvent], Seq[Vector2], Seq[Vector2]) = {
    if (owner.context.settings.allowModuleAnimation) owner.mustUpdateModel()
    NoEffects
  }

  override def descriptors: Seq[DroneModuleDescriptor] = positions.map(EnginesDescriptor)
}

