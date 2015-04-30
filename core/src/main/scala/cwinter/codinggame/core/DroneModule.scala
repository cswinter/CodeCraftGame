package cwinter.codinggame.core

import cwinter.codinggame.util.maths.Vector2
import cwinter.codinggame.util.modules.ModulePosition


abstract class DroneModule(
  val positions: Seq[Int],
  val owner: Drone
) {

  /**
   * Perform all module actions.
   * Method is called on every tick.
   * @param availableResources The amount of resources available to the module.
   * @return Returns all simulator events and the amount of resources consumed.
   */
  def update(availableResources: Int): (Seq[SimulatorEvent], Int)


  protected def absoluteModulePositions: Seq[Vector2] =
    for (p <- positions)
      yield owner.position + Vector2(ModulePosition(owner.size, p))

  protected final val NoEffects = (Seq.empty[SimulatorEvent], 0)
}

