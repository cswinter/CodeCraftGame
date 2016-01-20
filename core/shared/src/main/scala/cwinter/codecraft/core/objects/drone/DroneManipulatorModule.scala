package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core._
import cwinter.codecraft.core.api.DroneSpec
import cwinter.codecraft.graphics.worldstate.{ManipulatorArm, ManipulatorDescriptor, DroneModuleDescriptor}
import cwinter.codecraft.util.maths.Vector2

private[core] class DroneManipulatorModule(positions: Seq[Int], owner: DroneImpl)
  extends DroneModule(positions, owner) {

  private[this] var newDrone: Option[DroneImpl] = None
  private[this] var droneConstruction: Option[(DroneImpl, Int)] = None
  private[this] val constructorEnergy = new Array[Int](positions.length)


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
          var furtherProgress = 0
          for (i <- constructorEnergy.indices) {
            if (constructorEnergy(i) == 0 && remainingResources > 0) {
              remainingResources -= 1
              resourceDepletions ::= absoluteModulePositions(i)
              constructorEnergy(i) = DroneSpec.ConstructionPeriod
            }
            if (constructorEnergy(i) > 0 && progress + furtherProgress < drone.spec.buildTime) {
              constructorEnergy(i) -= 1
              furtherProgress += 1
            }
          }

          val progress2 = progress + furtherProgress
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

  def isConstructing: Boolean = droneConstruction.isDefined || newDrone.isDefined

  def startDroneConstruction(command: ConstructDrone): Unit = {
    if (droneConstruction.isEmpty) {
      val ConstructDrone(spec, controller, pos) = command
      newDrone = Some(new DroneImpl(spec, controller, owner.context, pos, -1))
    }
  }


  def manipulatorGraphics: Seq[ManipulatorArm] =
    droneConstruction.toSeq.flatMap {
      case (drone, progress) =>
        var i = 0
        for (
          (pos, energy) <- absoluteModulePositions zip constructorEnergy
          if energy > 0
        ) yield {
          val t = 50 * i + progress
          i += 1
          val offset = 0.8f * drone.radius * Vector2(math.sin(t / (10.5 - i)), math.cos(t / (7.1 + i)))
          ManipulatorArm(
            owner.player.color,
            pos.x.toFloat, pos.y.toFloat,
            (offset.x + drone.position.x).toFloat, (offset.y + drone.position.y).toFloat)
        }
    }

  override def descriptors: Seq[DroneModuleDescriptor] = positions.map(ManipulatorDescriptor)

  def droneInConstruction: Option[DroneImpl] = droneConstruction.map(_._1)
  override def cancelMovement: Boolean = isConstructing
}


