package cwinter.codinggame.core

// TODO: move all logic from drone into this class, remove setter methods
class DroneStorageModule(positions: Seq[Int], owner: Drone, startingResources: Int = 0)
    extends DroneModule(positions, owner) {

  private[this] var storedEnergyGlobes: Int = startingResources
  private var _storedMinerals = Set.empty[MineralCrystal]



  override def update(availableResources: Int): (Seq[SimulatorEvent], Int) = {
    NoEffects
  }

  def depositMineralCrystal(mineralCrystal: MineralCrystal): Unit = {
    _storedMinerals += mineralCrystal
  }

  def removeMineralCrystal(mineralCrystal: MineralCrystal): Unit = {
    _storedMinerals -= mineralCrystal
  }

  def modifyResources(amount: Int): Unit = {
    storedEnergyGlobes -= amount
  }

  def clear(): Unit = _storedMinerals = Set.empty[MineralCrystal]

  def storedMinerals: Set[MineralCrystal] = _storedMinerals
  def availableResources: Int = storedEnergyGlobes
  def availableStorage: Int =
    positions.size - _storedMinerals.foldLeft(0)(_ + _.size) - (storedEnergyGlobes + 6) / 7
}

