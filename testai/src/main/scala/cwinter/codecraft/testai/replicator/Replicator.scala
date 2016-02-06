package cwinter.codecraft.testai.replicator

import cwinter.codecraft.core.api.{MineralCrystal, DroneController, DroneSpec}


class Replicator(
  ctx: ReplicatorContext
) extends ReplicatorBase('Replicator, ctx) {
  val harvesterSpec = DroneSpec(storageModules = 1)
  val hunterSpec = DroneSpec(missileBatteries = 1)
  val destroyerSpec = DroneSpec(missileBatteries = 3, shieldGenerators = 1)
  val replicatorSpec = DroneSpec(storageModules = 2, constructors = 2)

  var nextCrystal: Option[MineralCrystal] = None
  var assignedZone: Option[HarvestingZone] = None

  def this() = this(new ReplicatorContext)


  private var slaves = Set.empty[Harvester]


  override def onSpawn(): Unit = {
    super.onSpawn()
    context.initialise(worldSize)
  }

  override def onTick(): Unit = {
    if (!isConstructing) {
      maybeRequestZone()
      nextConstructionSpec match {
        case Some((spec, controller))
        if shouldBeginConstruction(spec.resourceCost) =>
          buildDrone(spec, controller())
        case _ =>
          harvest()
      }
    }

    handleWeapons()
  }

  def maybeRequestZone(): Unit = {
    if (assignedZone.exists(_.exhausted)) assignedZone = None
    if (assignedZone.isEmpty)
      assignedZone = context.harvestCoordinator.requestHarvestingZone(position)
  }

  def shouldBeginConstruction(resourceCost: Int): Boolean = {
    val allResourcesAvailable = storedResources >= spec.resourceCost
    val mustStartEarly =
      storedResources + availableStorage < spec.resourceCost && storedResources >= 7
    allResourcesAvailable || mustStartEarly
  }

  def harvest(): Unit = {
    if (nextCrystal.exists(_.harvested)) nextCrystal = None
    if (nextCrystal.isEmpty && availableStorage > 0) {
      nextCrystal = context.harvestCoordinator.findClosestMineral(position, assignedZone)
      nextCrystal.foreach(moveTo)
    }
  }


  override def onArrivesAtMineral(m: MineralCrystal): Unit = {
    if (!m.harvested) {
      harvest(m)
    }
  }

  private def nextConstructionSpec: Option[(DroneSpec, () => DroneController)] = {
    if (slaves.size < this.spec.constructors) {
      Some((harvesterSpec, () => new Harvester(this, context)))
    } else if (shouldBuildReplicator) {
      Some((replicatorSpec, () => new Replicator(context)))
    } else {
      Some((hunterSpec, () => new Hunter(context)))
    }
  }

  private def shouldBuildReplicator =
    (context.droneCount('Replicator) < 2 ||
      context.harvestCoordinator.freeZoneCount > context.droneCount('Replicator) * 2) &&
      context.droneCount('Replicator) < 4

  override def onDeath(): Unit = {
    super.onDeath()
    for (m <- nextCrystal)
      context.harvestCoordinator.abortHarvestingMission(m)
  }

  def registerSlave(slave: Harvester): Unit = {
    slaves += slave
  }

  def slaveFailed(slave: Harvester): Unit = {
    slaves -= slave
  }

  object DroneCount {
    private[this] var counts = Map.empty[Symbol, Int]

    def apply(name: Symbol): Int = {
      counts.getOrElse(name, 0)
    }

    def increment(name: Symbol): Unit = {
      counts = counts.updated(name, DroneCount(name) + 1)
    }

    def decrement(name: Symbol): Unit = {
      counts = counts.updated(name, DroneCount(name) - 1)
    }
  }
}

