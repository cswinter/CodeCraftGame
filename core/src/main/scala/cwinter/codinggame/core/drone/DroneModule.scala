package cwinter.codinggame.core.drone

import cwinter.codinggame.core.SimulatorEvent
import cwinter.codinggame.util.maths.Vector2
import cwinter.codinggame.util.modules.ModulePosition


abstract class DroneModule(
  val positions: Seq[Int],
  val owner: Drone
) {
  println(s"$this, $positions")

  /**
   * Perform all module actions.
   * Method is called on every tick.
   * @param availableResources The amount of resources available to the module.
   * @return Returns all simulator events and the amount of resources consumed.
   */
  def update(availableResources: Int): (Seq[SimulatorEvent], Int)


  protected def absoluteModulePositions: Seq[Vector2] = {
    val rotation = owner.dynamics.orientation
    for (p <- positions)
      yield owner.position + Vector2(ModulePosition(owner.size, p)).rotated(rotation)
  }

  protected def absoluteMergedModulePositions(partitioning: Seq[Int]): Seq[Vector2] = {
    val rotation = owner.dynamics.orientation

    def partition(elems: Seq[Int], partitions: Seq[Int]): List[Seq[Int]] = partitions match {
      case Seq(p, rest @ _*) => elems.take(p) :: partition(elems.drop(p), rest)
      case _ => Nil
    }

    for {
      poss <- partition(positions, partitioning)
      offset = Vector2(ModulePosition.center(owner.size, poss)).rotated(rotation)
    } yield owner.position + offset
  }

  protected final val NoEffects = (Seq.empty[SimulatorEvent], 0)
}

