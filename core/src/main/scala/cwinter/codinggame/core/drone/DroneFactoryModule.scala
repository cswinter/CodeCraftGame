package cwinter.codinggame.core.drone

import cwinter.codinggame.worldstate.{DroneModuleDescriptor, ProcessingModuleDescriptor}
import cwinter.codinggame.core._
import cwinter.codinggame.util.maths.{Vector2, Rng}

class DroneFactoryModule(positions: Seq[Int], owner: Drone)
    extends DroneModule(positions, owner) {

  final val MineralProcessingPeriod = 15
  final val MineralResourceYield = 1


  private[this] var newMinerals = List.empty[MineralCrystal]
  private[this] var mineralProcessing = List.empty[(MineralCrystal, Int)]


  override def update(availableResources: Int): (Seq[SimulatorEvent], Seq[Vector2], Seq[Vector2]) = {
    var effects = List.empty[SimulatorEvent]
    var spawnedResources = List.empty[Vector2]

    // start new mineral constructions
    for (mineral <- newMinerals) {
      mineralProcessing ::= ((mineral, mineral.size * MineralResourceYield * MineralProcessingPeriod))
      effects ::= MineralCrystalActivated(mineral)
    }
    newMinerals = List.empty[MineralCrystal]


    // determine factory positions
    val centers = absoluteMergedModulePositions(contents)

    // perform mineral processing
    mineralProcessing =
      for (((mineral, progress), center) <- mineralProcessing zip centers)
        yield {
          mineral.position = center

          if (progress % MineralProcessingPeriod == 0) {
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


  def startMineralProcessing(mineral: MineralCrystal): Unit = {
    // TODO: need a capacity check somewhere
    newMinerals ::= mineral
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

  def mineralCrystals: Seq[MineralCrystal] = newMinerals ::: mineralProcessing.map(_._1)
}

