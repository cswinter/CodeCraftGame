package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core._
import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.graphics.worldstate._
import cwinter.codecraft.util.maths.Vector2
import cwinter.codecraft.util.modules.ModulePosition

import scala.collection.mutable

// TODO: rework mineral slots system to find simpler and less error prone solution
private[core] class DroneStorageModule(positions: Seq[Int], owner: DroneImpl, startingResources: Int = 0)
  extends DroneModule(positions, owner) {

  import DroneStorageModule._


  private[this] var storedEnergyGlobes =
    mutable.Stack[EnergyGlobe](Seq.fill(startingResources)(StaticEnergyGlobe): _*)

  private[this] var deposit: Option[DroneStorageModule] = None
  private[this] var harvesting: Option[MineralCrystalImpl] = None
  private[this] var harvestCountdown: Int = 0
  private[this] var resourceDepositee: Option[DroneStorageModule] = None


  override def update(availableResources: Int): (Seq[SimulatorEvent], Seq[Vector2], Seq[Vector2]) = {
    var effects = List.empty[SimulatorEvent]

    // TODO: make sure this does not cause determinism problems
    resourceDepositee.foreach(performResourceDeposit)

    for {
      m <- harvesting
      event <- harvest(m)
    } effects ::= event

    storedEnergyGlobes = storedEnergyGlobes.map(_.updated())

    (effects, Seq.empty[Vector2], Seq.empty[Vector2])
  }

  private def harvest(mineral: MineralCrystalImpl): Option[SimulatorEvent] = {
    // need to check availableStorage in case another drone gave this one resources
    if (mineral.harvested || availableStorage == 0) {
      harvesting = None
    } else {
      harvestCountdown -= positions.size
      if (harvestCountdown <= 0) {
        return performHarvest(mineral)
      }
    }
    None
  }

  private def performHarvest(mineral: MineralCrystalImpl): Option[SimulatorEvent] = {
    subtractFromResources(-1)
    mineral.decreaseSize()
    harvestCountdown = HarvestingDuration

    if (mineral.size == 0 || availableStorage == 0) {
      harvesting = None
    }

    if (mineral.size == 0) Some(MineralCrystalHarvested(mineral))
    else None
  }

  private def performResourceDeposit(depositee: DroneStorageModule): Unit = {
    val capacity = depositee.availableStorage
    if (capacity == 0) {
      owner.inform(s"Cannot deposit minerals - storage is completely full.")
    } else {
      val amount = math.min(storedResources, capacity)
      depositee.subtractFromResources(-amount)
      subtractFromResources(amount)
      resourceDepositee = None
    }
  }

  def subtractFromResources(amount: Int): Unit = {
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

  def depositEnergyGlobe(position: Vector2): Unit = {
    val targetPosition = calculateEnergyGlobePosition(storedResources)
    val newEnergyGlobe = new MovingEnergyGlobe(targetPosition, position - owner.position, 20)
    storedEnergyGlobes.push(newEnergyGlobe)
  }

  def withdrawEnergyGlobe(): Vector2 = {
    storedEnergyGlobes.pop() match {
      case StaticEnergyGlobe => calculateEnergyGlobePosition(storedResources)
      case meg: MovingEnergyGlobe => meg.position
    }
  }.rotated(owner.dynamics.orientation) + owner.position

  def harvestMineral(mineralCrystal: MineralCrystalImpl): Unit = {
    if (availableStorage == 0) {
      owner.warn(s"Trying to harvest mineral crystal, but storage is completely filled.")
    } else if (owner.position !~ mineralCrystal.position) {
      owner.warn("To far away from mineral crystal to harvest.")
    } else if (mineralCrystal.harvested) {
      owner.warn("Trying to harvest mineral crystal that has already been harvested.")
    } else {
      harvestCountdown = HarvestingDuration
      harvesting = Some(mineralCrystal)
    }
  }

  def depositResources(other: Option[DroneStorageModule]): Unit = {
    resourceDepositee = other
  }

  def storedResources: Int = storedEnergyGlobes.size

  def availableStorage: Int =
    positions.size * 7 - storedResources

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
        StorageModuleDescriptor(Seq(i), EnergyStorage(globes))
      }

    energyStorageDescriptors.toSeq
  }

  def energyGlobeAnimations: Seq[WorldObjectDescriptor] = {
    for {
      eg <- storedEnergyGlobes
      if eg.isInstanceOf[MovingEnergyGlobe]
      meg = eg.asInstanceOf[MovingEnergyGlobe]
      position = meg.position.rotated(owner.dynamics.orientation) + owner.position
    } yield EnergyGlobeDescriptor(position.x.toFloat, position.y.toFloat)
  }


  override def cancelMovement: Boolean = harvesting.nonEmpty || resourceDepositee.nonEmpty
}

// TODO: aggregate all constants
private[core] object DroneStorageModule {
  final val HarvestingDuration = 50
  final val MineralDepositTime = 45
  final val UnmergingTime = 20
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


