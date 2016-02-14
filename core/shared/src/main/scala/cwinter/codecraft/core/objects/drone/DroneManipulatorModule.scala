package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core._
import cwinter.codecraft.core.api.DroneSpec
import cwinter.codecraft.graphics.worldstate.{DroneModuleDescriptor, ManipulatorDescriptor}
import cwinter.codecraft.util.maths.Vector2

private[core] class DroneManipulatorModule(positions: Seq[Int], owner: DroneImpl)
  extends DroneModule(positions, owner) {

  private[this] var newDrone: Option[DroneImpl] = None
  private[this] var droneConstruction: Option[(DroneImpl, Int)] = None
  private[this] val constructorEnergy = new Array[Int](positions.length)


  override def update(availableResources: Int): (Seq[SimulatorEvent], Seq[Vector2], Seq[Vector2]) = {
    if (isConstructing && owner.hasMoved) owner.mustUpdateModel()

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
      for ((drone, progress) <- droneConstruction) yield {
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
          owner.mustUpdateModel()
          effects ::= SpawnDrone(drone)
          droneConstruction = None
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
      owner.mustUpdateModel()
      val ConstructDrone(spec, controller, pos) = command
      newDrone = Some(new DroneImpl(spec, controller, owner.context, pos, -1))
    }
  }

  override def descriptors: Seq[DroneModuleDescriptor] =
    for ((i, energy) <- positions zip constructorEnergy) yield
      ManipulatorDescriptor(
        i,
        droneInConstruction.map(d => (d.position - owner.position).rotated(-owner.dynamics.orientation)),
        energy > 0
      )

  def droneInConstruction: Option[DroneImpl] = droneConstruction.map(_._1)
  override def cancelMovement: Boolean = isConstructing
}


