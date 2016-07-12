package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.game.SimulatorEvent
import cwinter.codecraft.core.graphics.{EnginesDescriptor, DroneModuleDescriptor}
import cwinter.codecraft.util.maths.Vector2


private[core] class EnginesModule(positions: Seq[Int], owner: DroneImpl)
  extends DroneModule(positions, owner) {

  override def update(availableResources: Int): (Seq[SimulatorEvent], Seq[Vector2], Seq[Vector2]) = {
    if (owner.context.settings.allowModuleAnimation) owner.invalidateModelCache()
    NoEffects
  }

  override def descriptors: Seq[DroneModuleDescriptor] = positions.map(EnginesDescriptor)
}

