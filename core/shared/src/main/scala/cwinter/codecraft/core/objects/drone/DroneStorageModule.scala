package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core._
import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.graphics.worldstate._
import cwinter.codecraft.util.maths.{Float0To1, Vector2}
import cwinter.codecraft.util.modules.ModulePosition

import scala.collection.mutable

// TODO: rework mineral slots system to find simpler and less error prone solution
private[core] class DroneStorageModule(positions: Seq[Int], owner: DroneImpl, startingResources: Int = 0)
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
    _storedMinerals = _storedMinerals.map(_.updated).filterNot(_.contents == NoContents)

    storedEnergyGlobes = storedEnergyGlobes.map(_.updated())

    (effects, Seq.empty[Vector2], Seq.empty[Vector2])
  }

  def removeMineralCrystal(m: MineralCrystalImpl): Unit = {
    _storedMinerals = _storedMinerals.map { slot =>
      if (slot.contents.mineralCrystalOption == Some(m)) {
        if (m.size > 1) {
          new MineralSlot(Seq(), Unmerging(m.size, UnmergingTime))
        } else {
          new MineralSlot(Seq(), NoContents)
        }
      } else {
        slot
      }
    }
    _storedMinerals = _storedMinerals.filterNot(_.contents == NoContents)
    reassignMineralStorageIndices()
  }

  private def reassignMineralStorageIndices(): Unit = {
    val indices = partitionIndices(_storedMinerals.map(_.size))
    for ((slot, indices) <- _storedMinerals zip indices) {
      slot.positions = indices
    }
  }

  private def createMineralSlot(contents: MineralSlotContents): MineralSlot = {
    _storedMinerals = _storedMinerals.filterNot(_.contents.isInstanceOf[Unmerging])
    val newSlot = new MineralSlot(Seq(), contents)
    _storedMinerals :+= newSlot
    _storedMinerals = _storedMinerals.sortBy(_.size)
    reassignMineralStorageIndices()
    newSlot
  }

  def popMineralCrystal(maxSize: Int): Option[(MineralCrystalImpl, Vector2)] = {
    val mineral = _storedMinerals.find(_.size <= maxSize)

    for {
      slot <- mineral
      m <- slot.contents.mineralCrystalOption
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

  def depositMineral(mineralCrystal: MineralCrystalImpl, position: Vector2): Unit = {
    val dummy = StoresMineral(mineralCrystal)
    createMineralSlot(dummy)
    val targetPos = calculateAbsoluteMineralPosition(mineralCrystal)
    removeMineralCrystal(mineralCrystal)

    val depositing = new ReceivesMineral(mineralCrystal, MineralDepositTime, targetPos, position)
    createMineralSlot(depositing)
  }


  def harvestMineral(mineralCrystal: MineralCrystalImpl): Unit = {
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

  def storedMinerals: Set[MineralCrystalImpl] = allMineralsSorted.toSet // TODO: improve

  def availableResources: Int = storedEnergyGlobes.size

  def totalAvailableResources(refineries: Int): Int =
    availableResources +
      storedMinerals.filter(_.size <= refineries).foldLeft(0)(_ + _.size * DroneRefineryModule.MineralResourceYield)

  def availableStorage: Int =
    positions.size -
      _storedMinerals.foldLeft(0)(_ + _.contents.mineralCrystalOption.map(_.size).getOrElse(0)) -
      (availableResources + 6) / 7

  private def allMineralsSorted =
    for (slot <- _storedMinerals; m <- slot.contents.mineralCrystalOption)
      yield m

  private def calculateAbsoluteMineralPosition(mineralCrystal: MineralCrystalImpl): Vector2 = {
    val slot = _storedMinerals.find(_.contents.mineralCrystalOption == Some(mineralCrystal)).get
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
        case ReceivesMineral(m, p) =>
          if (p * 2 >= MineralDepositTime) StorageModuleDescriptor(sm.positions, EmptyStorage, Some(2 * (MineralDepositTime - p) / MineralDepositTime.toFloat))
          else StorageModuleDescriptor(sm.positions, EmptyStorage)
        case Unmerging(s, p) =>
          StorageModuleDescriptor(sm.positions, EmptyStorage, Some(p.toFloat / UnmergingTime))
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

    def size = contents.size

    override def toString: String = contents.toString
  }


  sealed trait MineralSlotContents {
    val mineralCrystalOption: Option[MineralCrystalImpl]

    def size: Int = mineralCrystalOption.get.size
    def updated: MineralSlotContents = this
    def effect: Option[SimulatorEvent] = None
    def willHarvest: Boolean = false
  }

  case class StoresMineral(mineralCrystal: MineralCrystalImpl) extends MineralSlotContents {
    val mineralCrystalOption = Some(mineralCrystal)
  }

  case class Unmerging(override val size: Int, tta: Int) extends MineralSlotContents {
    val mineralCrystalOption = None

    override def updated: MineralSlotContents =
      if (tta == 0) NoContents
      else Unmerging(size, tta - 1)
  }

  case object NoContents extends MineralSlotContents {
    val mineralCrystalOption = None
  }


  case class HarvestsMineral(mineralCrystal: MineralCrystalImpl, tta: Int) extends MineralSlotContents {
    val mineralCrystalOption = Some(mineralCrystal)

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

  class ReceivesMineral(
    val mineralCrystal: MineralCrystalImpl,
    private var tta: Int,
    targetPos: Vector2,
    startingPos: Vector2
  ) extends MineralSlotContents {
    val mineralCrystalOption = Some(mineralCrystal)
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

  object ReceivesMineral {
    def unapply(mineralSlotContents: MineralSlotContents): Option[(MineralCrystalImpl, Int)] =
      mineralSlotContents match {
        case depositsMineral: ReceivesMineral => Some((depositsMineral.mineralCrystal, depositsMineral.tta))
        case _ => None
      }
  }

}

object DroneStorageModule {
  final val HarvestingTime = 100
  final val MineralDepositTime = 45
  final val UnmergingTime = 20
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


