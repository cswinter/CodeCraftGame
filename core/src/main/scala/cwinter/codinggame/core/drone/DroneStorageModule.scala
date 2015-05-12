package cwinter.codinggame.core.drone

import cwinter.codinggame.core.{MineralCrystal, MineralCrystalHarvested, SimulatorEvent}
import cwinter.codinggame.util.maths.Vector2
import cwinter.codinggame.util.modules.ModulePosition
import cwinter.codinggame.worldstate._
import scala.collection.{BitSet, mutable}

class DroneStorageModule(positions: Seq[Int], owner: Drone, startingResources: Int = 0)
  extends DroneModule(positions, owner) {

  final val HarvestingTime = 50

  private[this] var storedEnergyGlobes = mutable.Stack[EnergyGlobe](Seq.fill(startingResources)(StaticEnergyGlobe): _*)
  private var _storedMinerals = Set.empty[MineralCrystal]

  private[this] var harvesting = List.empty[(MineralCrystal, Int)]

  private[this] var deposit: Option[DroneStorageModule] = None


  override def update(availableResources: Int): (Seq[SimulatorEvent], Seq[Vector2], Seq[Vector2]) = {
    var effects = List.empty[SimulatorEvent]

    harvesting = for ((m, t) <- harvesting) yield (m, t - 1)

    // TODO: animation (merging + lift)
    val (ongoing, finished) = harvesting.partition { case (m, t) => t > 0 }
    harvesting = ongoing

    for ((m, t) <- finished) {
      _storedMinerals += m
      effects ::= MineralCrystalHarvested(m)
    }

    // TODO: make this take time, + animation
    // TODO: range check
    for (s <- deposit) {
      if (s.availableStorage >= storedMinerals.foldLeft(0)(_ + _.size)) {
        s._storedMinerals ++= _storedMinerals
        _storedMinerals = Set.empty[MineralCrystal]
        deposit = None
      }
    }

    storedEnergyGlobes = storedEnergyGlobes.map(_.update())

    (effects, Seq.empty[Vector2], Seq.empty[Vector2])
  }

  def removeMineralCrystal(m: MineralCrystal): Unit = {
    _storedMinerals -= m
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


  def harvestMineral(mineralCrystal: MineralCrystal): Unit = {
    assert(mineralCrystal.size <= availableStorage, s"Crystal size is ${mineralCrystal.size} and storage is only $availableStorage")
    assert(owner.position ~ mineralCrystal.position)
    if (!mineralCrystal.harvested) {
      harvesting ::= ((mineralCrystal, HarvestingTime))
      // TODO: what if harvesting cancelled/drone killed?
      mineralCrystal.harvested = true
    }
  }

  def clear(): Unit = _storedMinerals = Set.empty[MineralCrystal]

  def storedMinerals: Set[MineralCrystal] = _storedMinerals

  def availableResources: Int = storedEnergyGlobes.size

  def availableStorage: Int =
    positions.size - harvesting.size - _storedMinerals.foldLeft(0)(_ + _.size) - (availableResources + 6) / 7

  override def descriptors: Seq[DroneModuleDescriptor] = {
    val mineralStorage = {
      (for (s <- storedMinerals.toSeq) yield (s, 1f)) ++
        (for ((m, p) <- harvesting) yield (m, (HarvestingTime - p).toFloat / HarvestingTime))
    }.sortBy(_._1.size)

    val partitioning = mineralStorage.map(_._1.size)
    val mineralStorageIndices = partitionIndices(partitioning)


    val mineralStorageDescriptors =
      for (((_, p), i) <- mineralStorage zip mineralStorageIndices) yield {
        if (p == 1) StorageModuleDescriptor(i, MineralStorage)
        else if (p > 0.5f) StorageModuleDescriptor(i, EmptyStorage)
        else StorageModuleDescriptor(i, EmptyStorage, Some(p * 2))
      }

    val globeStorageIndices: Seq[Int] = positions.drop(mineralStorage.foldLeft(0)(_ + _._1.size)).reverse
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


  override def cancelMovement: Boolean = harvesting.nonEmpty
}


trait EnergyGlobe {
  def update(): EnergyGlobe
}
case object StaticEnergyGlobe extends EnergyGlobe {
  def update(): StaticEnergyGlobe.type = this
}
class MovingEnergyGlobe(
  val targetPosition: Vector2,
  var position: Vector2,
  var tta: Int
) extends EnergyGlobe {
  val velocity: Vector2 = (targetPosition - position) / tta

  def update(): EnergyGlobe = {
    tta -= 1
    position += velocity
    if (tta == 0) StaticEnergyGlobe
    else this
  }
}


