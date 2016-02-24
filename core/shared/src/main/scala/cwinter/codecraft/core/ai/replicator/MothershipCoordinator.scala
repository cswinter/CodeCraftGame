package cwinter.codecraft.core.ai.replicator


class MothershipCoordinator {
  private var orphanedHarvesters = List.empty[Harvester]
  private var motherships = Set.empty[Replicator]

  def online(mothership: Replicator): Unit = {
    motherships += mothership
  }

  def offline(mothership: Replicator): Unit = {
    motherships -= mothership
  }

  def registerOrphan(harvester: Harvester): Unit = {
    orphanedHarvesters ::= harvester
  }

  def stuck(replicator: Replicator): Unit = {
    for {
      m <- motherships.find(_.hasSpareSlave)
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
        m <- motherships.find(_.hasPlentySlaves)
        s <- m.relieveSlave()
      } s.assignNewMaster(replicator)
    }
  }
}

