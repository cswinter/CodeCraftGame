package cwinter.codecraft.core.ai.replicator


class MothershipCoordinator {
  private var orphanedHarvesters = List.empty[Harvester]
  private var _motherships = Set.empty[Replicator]
  def motherships = _motherships

  def online(mothership: Replicator): Unit = {
    _motherships += mothership
  }

  def offline(mothership: Replicator): Unit = {
    _motherships -= mothership
  }

  def registerOrphan(harvester: Harvester): Unit = {
    orphanedHarvesters ::= harvester
  }

  def stuck(replicator: Replicator): Unit = {
    for {
      m <- _motherships.find(_.hasSpareSlave)
      s <- m.relieveSlave()
    } s.assignNewMaster(replicator)
  }

  def requestHarvester(replicator: Replicator): Unit = {
    if (orphanedHarvesters.nonEmpty) {
      val harvester = orphanedHarvesters.head
      harvester.assignNewMaster(replicator)
      orphanedHarvesters = orphanedHarvesters.tail
    } else {
      for {
        m <- _motherships.find(_.hasPlentySlaves)
        s <- m.relieveSlave()
      } s.assignNewMaster(replicator)
    }
  }
}

