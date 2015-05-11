package cwinter.codinggame.core.drone

import cwinter.codinggame.core._
import cwinter.codinggame.util.maths.{Vector2, Rng}
import cwinter.codinggame.worldstate.{DroneModuleDescriptor, ManipulatorDescriptor, ManipulatorArm}

class DroneManipulatorModule(positions: Seq[Int], owner: Drone)
  extends DroneModule(positions, owner) {

  private[this] var newDrone: Option[Drone] = None
  private[this] var droneConstruction: Option[(Drone, Int)] = None


  override def update(availableResources: Int): (Seq[SimulatorEvent], Int) = {
    var effects = List.empty[SimulatorEvent]
    var remainingResources = availableResources

    // start new drone constructions
    for (drone <- newDrone) {
      droneConstruction = Some((drone, 0))
      drone.dynamics.setPosition(owner.position - 110 * Rng.vector2())
      effects ::= DroneConstructionStarted(drone)
    }
    newDrone = None

    // perform drone constructions
    droneConstruction =
      for ((drone, progress) <- droneConstruction)
        yield {
          val progress2 =
            if (progress % drone.resourceDepletionPeriod == 0) {
              if (remainingResources > 0) {
                remainingResources -= 1
                progress + 1
              } else {
                progress
              }
            } else {
              progress + 1
            }

          drone.constructionProgress = Some(progress2)

          if (progress2 == drone.buildTime) {
            effects ::= SpawnDrone(drone)
            drone.constructionProgress = None
          }

          (drone, progress2)
        }

    droneConstruction = droneConstruction.filter {
      case (drone, progress) =>
        progress < drone.buildTime
    }

    (effects, availableResources - remainingResources)
  }

  def isConstructing: Boolean = droneConstruction != None || newDrone != None

  def startDroneConstruction(command: ConstructDrone): Unit = {
    if (droneConstruction == None) {
      newDrone = Some(command.drone)
    }
  }


  def manipulatorGraphics: Seq[ManipulatorArm] =
    droneConstruction.toSeq.flatMap {
      case (drone, progress) =>
        var i = 0
        absoluteModulePositions.map(pos => {
          val t = 50 * i + progress
          i += 1
          val offset = 0.8f * drone.radius * Vector2(math.sin(t / (10.0 - i)), math.cos(t / (7.1 + i)))
          ManipulatorArm(
            owner.player,
            pos.x.toFloat, pos.y.toFloat,
            (offset.x + drone.position.x).toFloat, (offset.y + drone.position.y).toFloat)
        })
    }

  override def descriptors: Seq[DroneModuleDescriptor] = positions.map(ManipulatorDescriptor)


  def droneInConstruction: Option[Drone] = droneConstruction.map(_._1)
}

