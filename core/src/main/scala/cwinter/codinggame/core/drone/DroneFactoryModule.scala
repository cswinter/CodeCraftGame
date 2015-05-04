package cwinter.codinggame.core.drone

import cwinter.codinggame.core._
import cwinter.codinggame.util.maths.Rng

class DroneFactoryModule(positions: Seq[Int], owner: Drone)
    extends DroneModule(positions, owner) {

  final val ResourceProcessingPeriod = 15


  private[this] var newDrones = List.empty[Drone]
  private[this] var newMinerals = List.empty[MineralCrystal]
  private[this] var droneConstructions = List.empty[(Drone, Int)]
  private[this] var mineralProcessing = List.empty[(MineralCrystal, Int)]


  override def update(availableResources: Int): (Seq[SimulatorEvent], Int) = {
    var effects = List.empty[SimulatorEvent]
    var remainingResources = availableResources

    // start new drone constructions
    for (drone <- newDrones) {
      droneConstructions ::= (drone, 0)
      droneConstructions = droneConstructions.sortBy { case (c, p) => -drone.requiredFactories }
      drone.dynamics.orientation = owner.dynamics.orientation
      effects ::= DroneConstructionStarted(drone)
    }
    newDrones = List.empty[Drone]

    // start new mineral constructions
    for (mineral <- newMinerals) {
      mineralProcessing ::= (mineral, mineral.size * 7 * ResourceProcessingPeriod)
      effects ::= MineralCrystalActivated(mineral)
    }
    newMinerals = List.empty[MineralCrystal]


    // determine factory positions
    val partitioning = droneConstructions.map(_._1.requiredFactories) ++ mineralProcessing.map(_._1.size)
    val centers = absoluteMergedModulePositions(partitioning)

    // perform drone constructions
    droneConstructions =
      for (((drone, progress), center) <- droneConstructions zip centers.take(droneConstructions.size))
        yield {
          drone.dynamics.setPosition(center)
          drone.dynamics.orientation = owner.dynamics.orientation
          drone.constructionProgress = Some(progress)

          if (progress == drone.buildTime) {
            effects ::= SpawnDrone(drone)
            drone.constructionProgress = None
            drone.dynamics.setPosition(owner.position - 150 * Rng.vector2())
          }
          if (progress % drone.resourceDepletionPeriod == 0) {
            if (remainingResources > 0) {
              remainingResources -= 1
              (drone, progress + 1)
            } else {
              (drone, progress)
            }
          } else {
            (drone, progress + 1)
          }
        }

    droneConstructions = droneConstructions.filter {
      case (drone, progress) =>
        drone.buildTime >= progress
    }

    // perform mineral processing
    mineralProcessing =
      for (((mineral, progress), center) <- mineralProcessing zip centers.drop(droneConstructions.size))
        yield {
          mineral.position = center

          if (progress % ResourceProcessingPeriod == 0) {
            remainingResources += 1
          }
          (mineral, progress - 1)
        }

    mineralProcessing = mineralProcessing.filter {
      case (mineral, remaining) =>
        if (remaining <= 0) effects ::= MineralCrystalDestroyed(mineral)
        remaining > 0
    }


    (effects, availableResources - remainingResources)
  }


  def startDroneConstruction(command: ConstructDrone): Unit = {
    newDrones ::= command.drone
  }


  def startMineralProcessing(mineral: MineralCrystal): Unit = {
    // TODO: need a capacity check somewhere
    newMinerals ::= mineral
  }


  def currentCapacity: Int = positions.length -
    droneConstructions.foldLeft(0)(_ + _._1.requiredFactories) -
    mineralProcessing.foldLeft(0)(_ + _._1.size) -
    newMinerals.foldLeft(0)(_ + _.size) -
    newDrones.foldLeft(0)(_ + _.requiredFactories)

  def contents: Seq[Int] = (
    droneConstructions.map(_._1.requiredFactories) ++
      mineralProcessing.map(_._1.size)
  ).sorted.reverse
}

