package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core._
import cwinter.codecraft.core.api.GameConstants
import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.graphics.worldstate._
import cwinter.codecraft.util.maths.Vector2
import cwinter.codecraft.util.modules.ModulePosition
import GameConstants.{HarvestingInterval, HarvestingRange}

import scala.collection.mutable

// TODO: rework mineral slots system to find simpler and less error prone solution
private[core] class DroneStorageModule(positions: Seq[Int], owner: DroneImpl, startingResources: Int = 0)
  extends DroneModule(positions, owner) {


  private[this] var storedEnergyGlobes =
    mutable.Stack[EnergyGlobe](Seq.fill(startingResources)(StaticEnergyGlobe): _*)

  private[this] var harvesting: Option[MineralCrystalImpl] = None
  private[this] var harvestCountdown: Int = 0
  private[this] var resourceDepositee: Option[DroneStorageModule] = None

  private[this] var _beamDescriptor: Option[HarvestingBeamsDescriptor] = None


  override def update(availableResources: Int): (Seq[SimulatorEvent], Seq[Vector2], Seq[Vector2]) = {
    if (isHarvesting && owner.hasMoved) updateBeamDescriptor()

    var effects = List.empty[SimulatorEvent]

    resourceDepositee.foreach(performResourceDeposit)

    for {
      m <- harvesting
      event <- harvest(m)
    } effects ::= event

    storedEnergyGlobes = storedEnergyGlobes.map{ x =>
      val updated = x.updated()
      if (x ne updated) owner.mustUpdateModel()
      updated
    }

    (effects, Seq.empty[Vector2], Seq.empty[Vector2])
  }

  private def harvest(mineral: MineralCrystalImpl): Option[SimulatorEvent] = {
    // need to check availableStorage in case another drone gave this one resources
    if (shouldCancelHarvesting(mineral)) {
      cancelHarvesting()
    } else {
      harvestCountdown -= positions.size
      if (harvestCountdown <= 0) {
        return performHarvest(mineral)
      }
    }
    None
  }

  private def shouldCancelHarvesting(mineral: MineralCrystalImpl): Boolean =
    mineral.harvested ||
    availableStorage == 0 ||
    (owner.hasMoved && !owner.isInHarvestingRange(mineral))

  private def performHarvest(mineral: MineralCrystalImpl): Option[SimulatorEvent] = {
    subtractFromResources(-1)
    mineral.decreaseSize()
    harvestCountdown = HarvestingInterval

    if (mineral.size == 0) Some(MineralCrystalHarvested(mineral))
    else None
  }

  private def performResourceDeposit(depositee: DroneStorageModule): Unit = {
    val capacity = depositee.availableStorage
    if (capacity == 0) {
      owner.inform(s"Cannot deposit minerals - storage is completely full.")
    } else {
      val amount = math.min(storedResources, capacity)
      for (i <- 1 to amount) {
        val localGlobePos = withdrawEnergyGlobe()
        val transformedGlobePos = convertGlobeReferenceFrame(depositee.owner, localGlobePos)
        depositee.depositEnergyGlobe(transformedGlobePos)
      }
      resourceDepositee = None
    }
  }

  def subtractFromResources(amount: Int): Unit = {
    owner.mustUpdateModel()
    if (amount > 0) {
      for (_ <- 0 until amount) storedEnergyGlobes.pop()
    } else if (amount < 0) {
      for (_ <- 0 until -amount) storedEnergyGlobes.push(StaticEnergyGlobe)
    }
  }

  private def calculateEnergyGlobePosition(index: Int): Vector2 = {
    val container = index / 7
    val pos = ModulePosition(owner.size, positions(container)) +
      ModulePosition.energyPosition(index % 7)
    Vector2(pos.x, pos.y)
  }

  def convertGlobeReferenceFrame(other: DroneImpl, pos: Vector2): Vector2 = {
    (pos - other.position).rotated(-other.dynamics.orientation)
  }

  def depositEnergyGlobe(position: Vector2): Unit = {
    val targetPosition = calculateEnergyGlobePosition(storedResources)
    val newEnergyGlobe = new MovingEnergyGlobe(targetPosition, position, 20)
    storedEnergyGlobes.push(newEnergyGlobe)
  }

  def withdrawEnergyGlobe(): Vector2 = {
    owner.mustUpdateModel()
    storedEnergyGlobes.pop() match {
      case StaticEnergyGlobe => calculateEnergyGlobePosition(storedResources)
      case meg: MovingEnergyGlobe => meg.position
    }
  }.rotated(owner.dynamics.orientation) + owner.position

  def harvestMineral(mineralCrystal: MineralCrystalImpl): Unit = {
    if (availableStorage == 0) {
      owner.warn(s"Trying to harvest mineral crystal, but storage is completely filled.")
    } else if (!owner.isInHarvestingRange(mineralCrystal)) {
      val dist = (owner.position - mineralCrystal.position).length
      owner.warn(s"Too far away from mineral crystal to harvest. " +
        s"Required: $HarvestingRange Actual: $dist.")
    } else if (mineralCrystal.harvested) {
      owner.warn("Trying to harvest mineral crystal that has already been harvested.")
    } else if (harvesting.contains(mineralCrystal)) {
      //owner.inform("This drone is already harvesting.")
    } else if (mineralCrystal.claimedBy.exists(_ != this)) {
      owner.warn("Trying to harvest a mineral crystal that is already being harvested by another drone.")
    } else {
      harvestCountdown = HarvestingInterval
      mineralCrystal.claimedBy = Some(this)
      harvesting = Some(mineralCrystal)
      updateBeamDescriptor()
    }
  }

  def droneHasDied(): Unit = {
    cancelHarvesting()
  }

  def cancelHarvesting(): Unit = {
    harvesting.foreach(_.claimedBy = None)
    harvesting = None
    updateBeamDescriptor()
  }

  def depositResources(other: Option[DroneStorageModule]): Unit = {
    resourceDepositee = other
  }

  def storedResources: Int = storedEnergyGlobes.size

  def availableStorage: Int =
    positions.size * 7 - storedResources

  def isHarvesting: Boolean = harvesting.nonEmpty

  override def descriptors: Seq[DroneModuleDescriptor] = {
    val globeStorageIndices: Seq[Int] = positions
    val energyStorageDescriptors =
      for ((group, i) <- storedEnergyGlobes.reverseIterator.grouped(7).zipAll(globeStorageIndices.iterator, Seq(), 0)) yield {
        val globes = {
          for (
            (eg, i) <- group.zipWithIndex
            if eg == StaticEnergyGlobe
          ) yield i
        }.toSet
        StorageModuleDescriptor(
          i,
          EnergyStorage(globes)
        )
      }

    energyStorageDescriptors.toSeq
  }

  def beamDescriptor: Option[HarvestingBeamsDescriptor] = _beamDescriptor

  private def updateBeamDescriptor(): Unit =
    _beamDescriptor =
      for {
        m <- harvesting
        relativeMineralPos = (m.position - owner.position).rotated(-owner.dynamics.orientation)
      } yield HarvestingBeamsDescriptor(owner.size, positions, relativeMineralPos)

  def energyGlobeAnimations: Seq[ModelDescriptor[_]] = {
    for {
      eg <- storedEnergyGlobes
      if eg.isInstanceOf[MovingEnergyGlobe]
      meg = eg.asInstanceOf[MovingEnergyGlobe]
      position = meg.position.rotated(owner.dynamics.orientation) + owner.position
      xPos = position.x.toFloat
      yPos = position.y.toFloat
    } yield ModelDescriptor(
      PositionDescriptor(xPos, yPos, 0),
      PlainEnergyGlobeDescriptor
    )
  }


  override def cancelMovement: Boolean = resourceDepositee.nonEmpty
}


private[drone] trait EnergyGlobe {
  def updated(): EnergyGlobe
}

private[drone] case object StaticEnergyGlobe extends EnergyGlobe {
  def updated(): StaticEnergyGlobe.type = this
}

private[drone] class MovingEnergyGlobe(
  val targetPosition: Vector2,
  var position: Vector2,
  var tta: Int
) extends EnergyGlobe {
  val velocity: Vector2 = (targetPosition - position) / tta

  def updated(): EnergyGlobe = {
    tta -= 1
    position += velocity
    if (tta == 0) StaticEnergyGlobe
    else this
  }
}


