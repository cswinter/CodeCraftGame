package cwinter.codinggame.core.objects.drone

import cwinter.codinggame.core._
import cwinter.codinggame.util.maths.Vector2
import cwinter.codinggame.worldstate.{DroneModuleDescriptor, ManipulatorDescriptor, ManipulatorArm}

class DroneManipulatorModule(positions: Seq[Int], owner: Drone)
  extends DroneModule(positions, owner) {

  private[this] var newDrone: Option[Drone] = None
  private[this] var droneConstruction: Option[(Drone, Int)] = None


  override def update(availableResources: Int): (Seq[SimulatorEvent], Seq[Vector2], Seq[Vector2]) = {
    var effects = List.empty[SimulatorEvent]
    var remainingResources = availableResources
    var resourceDepletions = List.empty[Vector2]

    // start new drone constructions
    for (newDrone <- newDrone) {
      droneConstruction = Some((newDrone, 0))
      effects ::= DroneConstructionStarted(newDrone)
    }
    newDrone = None

    // perform drone constructions
    droneConstruction =
      for ((drone, progress) <- droneConstruction)
        yield {
          val progress2 =
            if (progress % drone.spec.resourceDepletionPeriod == 0) {
              if (remainingResources > 0) {
                remainingResources -= 1
                // TODO: use all manipulator modules
                resourceDepletions ::= absoluteModulePositions.head
                progress + 1
              } else {
                progress
              }
            } else {
              progress + 1
            }

          drone.constructionProgress = Some(progress2)

          if (progress2 == drone.spec.buildTime) {
            effects ::= SpawnDrone(drone)
            drone.constructionProgress = None
          }

          (drone, progress2)
        }

    droneConstruction = droneConstruction.filter {
      case (drone, progress) =>
        progress < drone.spec.buildTime
    }

    (effects, resourceDepletions, Seq.empty[Vector2])
  }

  def isConstructing: Boolean = droneConstruction != None || newDrone != None

  def startDroneConstruction(command: ConstructDrone): Unit = {
    if (droneConstruction == None) {
      val ConstructDrone(spec, controller, pos) = command
      val d = new Drone(spec, controller, owner.player, pos, -1, owner.worldConfig, owner.replayRecorder)
      newDrone = Some(d)
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

