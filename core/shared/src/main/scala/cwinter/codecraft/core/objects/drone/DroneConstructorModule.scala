package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.api.{GameConstants, DroneSpec}
import GameConstants.DroneConstructionTime
import cwinter.codecraft.core._
import cwinter.codecraft.core.api.DroneSpec
import cwinter.codecraft.graphics.worldstate.{ConstructionBeamDescriptor, DroneModuleDescriptor, ManipulatorDescriptor}
import cwinter.codecraft.util.maths.Vector2

private[core] class DroneConstructorModule(positions: Seq[Int], owner: DroneImpl)
  extends DroneModule(positions, owner) {

  private[this] var newDrone: Option[ConstructDrone] = None
  private[this] var droneConstruction: Option[(DroneImpl, Int)] = None
  private[this] val constructorEnergy = new Array[Int](positions.length)

  private[this] var _beamDescriptor: Option[ConstructionBeamDescriptor] = None


  override def update(availableResources: Int): (Seq[SimulatorEvent], Seq[Vector2], Seq[Vector2]) = {
    var shouldUpdateBeamDescriptor = false
    if (isConstructing && owner.hasMoved) updateBeamDescriptor()

    var effects = List.empty[SimulatorEvent]
    var remainingResources = availableResources
    var resourceDepletions = List.empty[Vector2]

    // start new drone constructions
    for (ConstructDrone(spec, controller, pos) <- newDrone) {
      val newDrone = new DroneImpl(spec, controller, owner.context, pos, -1)
      droneConstruction = Some((newDrone, 0))
      shouldUpdateBeamDescriptor = true
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
            constructorEnergy(i) = DroneConstructionTime
            shouldUpdateBeamDescriptor = true
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
          droneConstruction = None
          drone.constructionProgress = None
          shouldUpdateBeamDescriptor = true
        }

        (drone, progress2)
      }

    droneConstruction = droneConstruction.filter {
      case (drone, progress) =>
        progress < drone.spec.buildTime
    }

    if (shouldUpdateBeamDescriptor) updateBeamDescriptor()

    (effects, resourceDepletions, Seq.empty[Vector2])
  }

  def isConstructing: Boolean = droneConstruction.isDefined || newDrone.isDefined

  def startDroneConstruction(command: ConstructDrone): Unit = {
    if (droneConstruction.isEmpty) {
      newDrone = Some(command)
    }
  }

  override def descriptors: Seq[DroneModuleDescriptor] =
    for ((i, energy) <- positions zip constructorEnergy) yield
      ManipulatorDescriptor(i)

  def beamDescriptor = _beamDescriptor

  private def updateBeamDescriptor(): Unit =
    _beamDescriptor =
      for {
        d <- droneInConstruction
        relativeConstructionPos = (d.position - owner.position).rotated(-owner.dynamics.orientation)
        modules = positions zip constructorEnergy.map(_ > 0)
      } yield ConstructionBeamDescriptor(owner.size, modules, relativeConstructionPos, owner.player.color)

  def droneInConstruction: Option[DroneImpl] = droneConstruction.map(_._1)
  override def cancelMovement: Boolean = isConstructing
}


