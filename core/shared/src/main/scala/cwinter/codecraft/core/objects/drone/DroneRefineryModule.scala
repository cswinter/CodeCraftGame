package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.core._
import cwinter.codecraft.graphics.worldstate.{ProcessingModuleDescriptor, DroneModuleDescriptor}
import cwinter.codecraft.util.maths.{Vector2, Rng}

private[core] class DroneRefineryModule(positions: Seq[Int], owner: DroneImpl)
    extends DroneModule(positions, owner) {

  import DroneRefineryModule._
  private[this] var newMinerals = List.empty[MineralCrystalImpl]
  private[this] var mineralProcessing = List.empty[(MineralCrystalImpl, Int)]


  override def update(availableResources: Int): (Seq[SimulatorEvent], Seq[Vector2], Seq[Vector2]) = {
    var effects = List.empty[SimulatorEvent]
    var spawnedResources = List.empty[Vector2]

    // start new mineral constructions
    for (mineral <- newMinerals) {
      mineralProcessing ::= ((mineral, MineralResourceYield * MineralProcessingPeriod))
      mineralProcessing = mineralProcessing.sortBy(-_._1.size)
    }
    newMinerals = List.empty[MineralCrystalImpl]


    // determine factory positions
    val centers = absoluteMergedModulePositions(contents)

    // perform mineral processing
    mineralProcessing =
      for (((mineral, progress), center) <- mineralProcessing zip centers)
        yield {
          mineral.position = center

          if (unprocessedAmount(progress, mineral.size) > unprocessedAmount(progress - 1, mineral.size)) {
            spawnedResources ::= center
          }
          (mineral, progress - 1)
        }

    mineralProcessing = mineralProcessing.filter {
      case (mineral, remaining) =>
        remaining > 0
    }


    (effects, Seq.empty[Vector2], spawnedResources)
  }

  private def unprocessedAmount(progress: Int, size: Int): Int =
    math.ceil(size * progress.toDouble / MineralProcessingPeriod).toInt

  def startMineralProcessing(mineral: MineralCrystalImpl): Unit = {
    if (mineral.size > currentCapacity) {
      owner.warn(s"Mineral size (${mineral.size}) exceeds current processing capacity ($currentCapacity)")
    } else {
      newMinerals ::= mineral
    }
  }

  def currentCapacity: Int = positions.length -
    mineralProcessing.foldLeft(0)(_ + _._1.size) -
    newMinerals.foldLeft(0)(_ + _.size)

  /**
   * Returns the amount of resources that will be obtained from the current module contents.
   */
  def unprocessedResourceAmount: Int = {
    val contributions = for ((mineral, progress) <- mineralProcessing) yield
      unprocessedAmount(progress, mineral.size)
    contributions.sum
  }

  private def contents: Seq[Int] = mineralProcessing.map(_._1.size).sorted.reverse

  override def descriptors: Seq[DroneModuleDescriptor] = {
    val partitioning = contents
    val processingMinerals = partitionIndices(partitioning)
    val idle = positions.drop(contents.sum).map(Seq(_))
    (processingMinerals ++ idle).map(ProcessingModuleDescriptor(_))
  }

  def mineralCrystals: Seq[MineralCrystalImpl] = newMinerals ::: mineralProcessing.map(_._1)
}

// TODO: aggregate all constants
private[core] object DroneRefineryModule {
  final val MineralProcessingPeriod = 100
  final val MineralResourceYield = 2
}
