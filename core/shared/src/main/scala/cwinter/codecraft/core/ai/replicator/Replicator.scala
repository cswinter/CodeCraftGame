package cwinter.codecraft.core.ai.replicator

import cwinter.codecraft.core.api.{DroneController, DroneSpec, MineralCrystal}


class Replicator(
  ctx: ReplicatorContext
) extends ReplicatorBase('Replicator, ctx) {
  val harvesterSpec = DroneSpec(storageModules = 1)
  val hunterSpec = DroneSpec(missileBatteries = 1)
  val destroyerSpec = DroneSpec(missileBatteries = 3, shieldGenerators = 1)
  val replicatorSpec = DroneSpec(storageModules = 1, constructors = 2, missileBatteries = 1)

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
    } else {
      for (
        n <- nextCrystal
        if !isHarvesting
      ) {
        nextCrystal.foreach(context.harvestCoordinator.abortHarvestingMission)
        nextCrystal = None
      }
    }

    assessThreatLevel()
    handleWeapons()
  }

  def maybeRequestZone(): Unit = {
    if (assignedZone.exists(_.exhausted)) assignedZone = None
    if (assignedZone.isEmpty)
      assignedZone = context.harvestCoordinator.requestHarvestingZone(position)
  }

  def shouldBeginConstruction(resourceCost: Int): Boolean = {
    val allResourcesAvailable =
      storedResources >= resourceCost
    val mustStartEarly =
      storedResources + availableStorage < resourceCost && (
        storedResources >= 14 || availableStorage == 0)
    allResourcesAvailable || mustStartEarly
  }

  def harvest(): Unit = {
    if (nextCrystal.exists(_.harvested)) nextCrystal = None
    if (nextCrystal.isEmpty && availableStorage > 0)
      nextCrystal = context.harvestCoordinator.findClosestMineral(position, assignedZone)
    for (m <- nextCrystal if !isHarvesting) moveTo(m)
  }


  override def onArrivesAtMineral(m: MineralCrystal): Unit = {
    if (!m.harvested) {
      harvest(m)
    }
  }

  private def nextConstructionSpec: Option[(DroneSpec, () => DroneController)] = {
    if (slaves.size < this.spec.constructors - 1) {
      Some((harvesterSpec, () => new Harvester(this, context)))
    } else if (shouldBuildReplicator) {
      Some((replicatorSpec, () => new Replicator(context)))
    } else {
      Some((hunterSpec, () => new Soldier(context)))
    }
  }

  private def shouldBuildReplicator =
    (context.droneCount('Replicator) < 2 ||
      context.harvestCoordinator.freeZoneCount > context.droneCount('Replicator) * 2) &&
      context.droneCount('Replicator) < 4

  private def assessThreatLevel(): Unit = {
    if (spec.missileBatteries <= 3) {
      if (enemies.exists(_.spec.missileBatteries > 0)) {
        context.battleCoordinator.requestAssistance(this)
      }
    }
  }

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

  override def metaController = Some(context)
}

