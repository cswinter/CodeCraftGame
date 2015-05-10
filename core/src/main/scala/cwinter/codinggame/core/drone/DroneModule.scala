package cwinter.codinggame.core.drone

import cwinter.codinggame.core.SimulatorEvent
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
  def descriptors: Seq[cwinter.worldstate.DroneModule]
  def cancelMovement: Boolean = false


  protected def absoluteModulePositions: Seq[Vector2] = {
    val rotation = owner.dynamics.orientation
    for (p <- positions)
      yield owner.position + Vector2(ModulePosition(owner.size, p)).rotated(rotation)
  }

  def partitionIndices(partitions: Seq[Int]): List[Seq[Int]] = {
    def _partitionIndices(elems: Seq[Int], partitions: Seq[Int]): List[Seq[Int]] = partitions match {
      case Seq(p, rest@_*) =>
        val (takep, dropp) = elems.splitAt(p)
        takep :: _partitionIndices(dropp, rest)
      case _ => Nil
    }
    _partitionIndices(positions, partitions)
  }

  protected def absoluteMergedModulePositions(partitioning: Seq[Int]): Seq[Vector2] = {
    val rotation = owner.dynamics.orientation

    for {
      poss <- partitionIndices(partitioning)
      offset = Vector2(ModulePosition.center(owner.size, poss)).rotated(rotation)
    } yield owner.position + offset
  }

  protected final val NoEffects = (Seq.empty[SimulatorEvent], 0)
}

