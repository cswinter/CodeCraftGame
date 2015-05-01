package cwinter.codinggame.core


class DroneStorageModule(positions: Seq[Int], owner: Drone, startingResources: Int = 0)
  extends DroneModule(positions, owner) {

  private[this] var storedEnergyGlobes: Int = startingResources
  private var _storedMinerals = Set.empty[MineralCrystal]

  private[this] var harvesting = List.empty[(MineralCrystal, Int)]

  private[this] var deposit: Option[DroneStorageModule] = None


  override def update(availableResources: Int): (Seq[SimulatorEvent], Int) = {
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

    (effects, 0)
  }

  def removeMineralCrystal(m: MineralCrystal): Unit = {
    _storedMinerals -= m
  }

  def modifyResources(amount: Int): Unit = {
    storedEnergyGlobes -= amount
  }

  def depositMinerals(other: Option[DroneStorageModule]): Unit = {
    deposit = other
  }


  def harvestMineral(mineralCrystal: MineralCrystal): Unit = {
    assert(mineralCrystal.size <= availableStorage, s"Crystal size is ${mineralCrystal.size} and storage is only $availableStorage")
    assert(owner.position ~ mineralCrystal.position)
    if (!mineralCrystal.harvested) {
      harvesting ::=(mineralCrystal, 1)
      println(harvesting)
    }
  }

  def clear(): Unit = _storedMinerals = Set.empty[MineralCrystal]

  def storedMinerals: Set[MineralCrystal] = _storedMinerals

  def availableResources: Int = storedEnergyGlobes

  def availableStorage: Int =
    positions.size - _storedMinerals.foldLeft(0)(_ + _.size) - (storedEnergyGlobes + 6) / 7
}

