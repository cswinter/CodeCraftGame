package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.SimulatorEvent
import cwinter.codecraft.graphics.worldstate.DroneModuleDescriptor
import cwinter.codecraft.util.maths.Vector2
import cwinter.codecraft.util.modules.ModulePosition


private[core] abstract class DroneModule(
  val positions: Seq[Int],
  val owner: DroneImpl
) {
  /**
   * Perform all module actions.
   * Method is called on every tick.
   * @param availableResources The amount of resources available to the module.
   * @return Returns all simulator events, the amount of resources consumed and a list of positions that have produced a resource.
   */
  def update(availableResources: Int): (Seq[SimulatorEvent], Seq[Vector2], Seq[Vector2])
  def descriptors: Seq[DroneModuleDescriptor]
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

  protected final val NoEffects = (Seq.empty[SimulatorEvent], Seq.empty[Vector2], Seq.empty[Vector2])
}

