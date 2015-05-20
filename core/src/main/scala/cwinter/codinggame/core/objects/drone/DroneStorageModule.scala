package cwinter.codinggame.core.objects.drone

import cwinter.codinggame.core._
import cwinter.codinggame.core.objects.MineralCrystal
import cwinter.codinggame.util.maths.{Float0To1, Vector2}
import cwinter.codinggame.util.modules.ModulePosition
import cwinter.codinggame.worldstate._
import scala.collection.{BitSet, mutable}

private[core] class DroneStorageModule(positions: Seq[Int], owner: Drone, startingResources: Int = 0)
  extends DroneModule(positions, owner) {

  import DroneStorageModule._


  private[this] var storedEnergyGlobes = mutable.Stack[EnergyGlobe](Seq.fill(startingResources)(StaticEnergyGlobe): _*)

  private var _storedMinerals = Seq.empty[MineralSlot]

  private[this] var deposit: Option[DroneStorageModule] = None


  override def update(availableResources: Int): (Seq[SimulatorEvent], Seq[Vector2], Seq[Vector2]) = {
    var effects = List.empty[SimulatorEvent]

    for {
      s <- _storedMinerals
      e <- s.contents.effect
    } effects ::= e
    _storedMinerals = _storedMinerals.map(_.updated)

    storedEnergyGlobes = storedEnergyGlobes.map(_.updated())

    (effects, Seq.empty[Vector2], Seq.empty[Vector2])
  }

  def removeMineralCrystal(m: MineralCrystal): Unit = {
    _storedMinerals = _storedMinerals.filterNot(_.contents.mineralCrystal == m)
    reassignMineralStorageIndices()
  }

  private def reassignMineralStorageIndices(): Unit = {
    val indices = partitionIndices(_storedMinerals.map(_.size))
    for ((slot, indices) <- _storedMinerals zip indices) {
      slot.positions = indices
    }
  }

  private def createMineralSlot(contents: MineralSlotContents): MineralSlot = {
    val newSlot = new MineralSlot(Seq(), contents)
    _storedMinerals :+= newSlot
    _storedMinerals = _storedMinerals.sortBy(_.size)
  reassignMineralStorageIndices()
    newSlot
  }

  def popMineralCrystal(maxSize: Int): Option[(MineralCrystal, Vector2)] = {
    val mineral = _storedMinerals.find(_.size <= maxSize)

    for {
      slot <- mineral
      m = slot.contents.mineralCrystal
      pos = calculateAbsoluteMineralPosition(m)
    } yield {
      removeMineralCrystal(m)
      (m, pos)
    }
  }

  def modifyResources(amount: Int): Unit = {
    if (amount > 0) {
      for (_ <- 0 until amount) storedEnergyGlobes.pop()
    } else if (amount < 0) {
      for (_ <- 0 until -amount) storedEnergyGlobes.push(StaticEnergyGlobe)
    }
  }

  private def calculateEnergyGlobePosition(index: Int): Vector2 = {
    val container = index / 7
    val pos = ModulePosition(owner.size, positions.reverse(container)) +
      ModulePosition.energyPosition(index % 7)
    Vector2(pos.x, pos.y)
  }

  def depositEnergyGlobe(position: Vector2): Unit = {
    val targetPosition = calculateEnergyGlobePosition(availableResources)
    val newEnergyGlobe = new MovingEnergyGlobe(targetPosition, position - owner.position, 20)
    storedEnergyGlobes.push(newEnergyGlobe)
  }

  def withdrawEnergyGlobe(): Vector2 = {
    storedEnergyGlobes.pop() match {
      case StaticEnergyGlobe => calculateEnergyGlobePosition(availableResources)
      case meg: MovingEnergyGlobe => meg.position
    }
  }.rotated(owner.dynamics.orientation) + owner.position

  def depositMinerals(other: Option[DroneStorageModule]): Unit = {
    deposit = other
  }

  def depositMineral(mineralCrystal: MineralCrystal, position: Vector2): Unit = {
    val dummy = StoresMineral(mineralCrystal)
    createMineralSlot(dummy)
    val targetPos = calculateAbsoluteMineralPosition(mineralCrystal)
    removeMineralCrystal(mineralCrystal)

    val depositing = new DepositsMineral(mineralCrystal, MineralDepositTime, targetPos, position)
    createMineralSlot(depositing)
  }


  def harvestMineral(mineralCrystal: MineralCrystal): Unit = {
    if (mineralCrystal.size > availableStorage) {
      owner.warn(s"Trying to harvest mineral crystal of size ${mineralCrystal.size}. Available storage is only $availableStorage.")
    } else if (owner.position !~ mineralCrystal.position) {
      owner.warn("To far away from mineral crystal to harvest.")
    } else if (mineralCrystal.harvested) {
      owner.warn("Trying to harvest mineral crystal that has already been harvested.")
    } else {
      createMineralSlot(HarvestsMineral(mineralCrystal, HarvestingTime))
      // TODO: what if harvesting cancelled/drone killed?
      mineralCrystal.harvested = true
      mineralCrystal.harvestPosition = calculateAbsoluteMineralPosition(mineralCrystal)
    }
  }

  def storedMinerals: Set[MineralCrystal] = _storedMinerals.map(_.contents.mineralCrystal).toSet // TODO: improve

  def availableResources: Int = storedEnergyGlobes.size

  def availableStorage: Int =
    positions.size - _storedMinerals.foldLeft(0)(_ + _.size) - (availableResources + 6) / 7

  private def allMineralsSorted = _storedMinerals.map(_.contents.mineralCrystal)

  private def calculateAbsoluteMineralPosition(mineralCrystal: MineralCrystal): Vector2 = {
    val slot = _storedMinerals.find(_.contents.mineralCrystal == mineralCrystal).get
    ModulePosition.center(owner.size, slot.positions).toVector2.rotated(owner.dynamics.orientation) +
      owner.position
  }

  override def descriptors: Seq[DroneModuleDescriptor] = {
    val mineralStorageDescriptors =
      for (sm <- _storedMinerals) yield sm.contents match {
        case StoresMineral(m) => StorageModuleDescriptor(sm.positions, MineralStorage)
        case HarvestsMineral(m, p) =>
          if (p == 0) StorageModuleDescriptor(sm.positions, MineralStorage)
          else if (p < HarvestingTime / 2) StorageModuleDescriptor(sm.positions, EmptyStorage)
          else StorageModuleDescriptor(sm.positions, EmptyStorage, Some(2 * (HarvestingTime - p) / HarvestingTime.toFloat))
        case DepositsMineral(m, p) =>
          if (p * 2 >= MineralDepositTime) StorageModuleDescriptor(sm.positions, EmptyStorage, Some(2 * (MineralDepositTime - p) / MineralDepositTime.toFloat))
          else StorageModuleDescriptor(sm.positions, EmptyStorage)
      }

    val globeStorageIndices: Seq[Int] = positions.drop(_storedMinerals.foldLeft(0)(_ + _.size)).reverse
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

    mineralStorageDescriptors ++ energyStorageDescriptors
  }

  def energyGlobeAnimations: Seq[WorldObjectDescriptor] = {
    for {
      eg <- storedEnergyGlobes
      if eg.isInstanceOf[MovingEnergyGlobe]
      meg = eg.asInstanceOf[MovingEnergyGlobe]
      position = meg.position.rotated(owner.dynamics.orientation) + owner.position
    } yield EnergyGlobeDescriptor(position.x.toFloat, position.y.toFloat)
  }


  override def cancelMovement: Boolean = _storedMinerals.exists(_.contents.isInstanceOf[HarvestsMineral])


  class MineralSlot(
    var positions: Seq[Int],
    val contents: MineralSlotContents
  ) {
    def updated = new MineralSlot(positions, contents.updated)

    def size = contents.mineralCrystal.size

    override def toString: String = contents.toString
  }


  sealed trait MineralSlotContents {
    val mineralCrystal: MineralCrystal

    def updated: MineralSlotContents = this

    def effect: Option[SimulatorEvent] = None
    def willHarvest: Boolean = false
  }

  case class StoresMineral(mineralCrystal: MineralCrystal) extends MineralSlotContents

  case class HarvestsMineral(mineralCrystal: MineralCrystal, tta: Int) extends MineralSlotContents {
    override def willHarvest: Boolean = tta == 1

    override def updated: MineralSlotContents = {
      if (tta == 1) {
        mineralCrystal.harvestProgress = None
        StoresMineral(mineralCrystal)
      } else {
        mineralCrystal.harvestProgress = Some(Float0To1(1 - tta / DroneStorageModule.HarvestingTime.toFloat))
        HarvestsMineral(mineralCrystal, tta - 1)
      }
    }

    override def effect =
      if (tta == 1) Some(MineralCrystalHarvested(mineralCrystal))
      else None
  }

  class DepositsMineral(
    val mineralCrystal: MineralCrystal,
    private var tta: Int,
    targetPos: Vector2,
    startingPos: Vector2
  ) extends MineralSlotContents {
    val velocity = (targetPos - startingPos) / tta
    var pos = startingPos - owner.position
    mineralCrystal.position = startingPos

    override def updated: MineralSlotContents = {
      tta -= 1
      pos += velocity
      mineralCrystal.position = pos + owner.position
      if (tta == 0) {
        StoresMineral(mineralCrystal)
      } else this
    }

    override def effect =
      if (tta == MineralDepositTime) Some(MineralCrystalActivated(mineralCrystal))
      else if (tta == 1) Some(MineralCrystalInactivated(mineralCrystal))
      else None
  }

  object DepositsMineral {
    def unapply(mineralSlotContents: MineralSlotContents): Option[(MineralCrystal, Int)] =
      mineralSlotContents match {
        case depositsMineral: DepositsMineral => Some((depositsMineral.mineralCrystal, depositsMineral.tta))
        case _ => None
      }
  }

}

object DroneStorageModule {
  final val HarvestingTime = 50
  final val MineralDepositTime = 45
}


trait EnergyGlobe {
  def updated(): EnergyGlobe
}

case object StaticEnergyGlobe extends EnergyGlobe {
  def updated(): StaticEnergyGlobe.type = this
}

class MovingEnergyGlobe(
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


