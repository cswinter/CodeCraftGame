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
      effects ::= MineralCrystalActivated(mineral)
    }
    newMinerals = List.empty[MineralCrystalImpl]


    // determine factory positions
    val centers = absoluteMergedModulePositions(contents)

    // perform mineral processing
    mineralProcessing =
      for (((mineral, progress), center) <- mineralProcessing zip centers)
        yield {
          mineral.position = center

          if (progress % (MineralProcessingPeriod / mineral.size) == 1) {
            spawnedResources ::= center
          }
          (mineral, progress - 1)
        }

    mineralProcessing = mineralProcessing.filter {
      case (mineral, remaining) =>
        if (remaining <= 0) effects ::= MineralCrystalDestroyed(mineral)
        remaining > 0
    }


    (effects, Seq.empty[Vector2], spawnedResources)
  }


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

  private def contents: Seq[Int] = mineralProcessing.map(_._1.size).sorted.reverse

  override def descriptors: Seq[DroneModuleDescriptor] = {
    val partitioning = contents
    val processingMinerals = partitionIndices(partitioning)
    val idle = positions.drop(contents.sum).map(Seq(_))
    (processingMinerals ++ idle).map(ProcessingModuleDescriptor(_))
  }

  def mineralCrystals: Seq[MineralCrystalImpl] = newMinerals ::: mineralProcessing.map(_._1)
}

object DroneRefineryModule {
  final val MineralProcessingPeriod = 100
  final val MineralResourceYield = 2
}
