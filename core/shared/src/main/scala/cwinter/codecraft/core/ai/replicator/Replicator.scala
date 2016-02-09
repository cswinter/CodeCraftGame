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
  var currentConstruction: Option[DroneSpec] = None

  def this() = this(new ReplicatorContext)


  private var slaves = Set.empty[Harvester]


  override def onSpawn(): Unit = {
    super.onSpawn()
    context.initialise(worldSize)
  }

  override def onTick(): Unit = {
    if (!isConstructing) {
      currentConstruction = None
      maybeRequestZone()
      nextConstructionSpec match {
        case Some((spec, controller))
        if shouldBeginConstruction(spec.resourceCost) =>
          buildDrone(spec, controller())
          currentConstruction = Some(spec)
        case _ =>
          harvest()
      }
    } else if (!isHarvesting) {
      for (n <- nextCrystal) {
        context.harvestCoordinator.abortHarvestingMission(n)
        nextCrystal = None
      }
    }

    if (!currentConstruction.contains(harvesterSpec) && needMoreSlaves)
      context.mothershipCoordinator.requestHarvester(this)
    if (isStuck) context.mothershipCoordinator.stuck(this)

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
    if (needMoreSlaves) {
      Some((harvesterSpec, () => new Harvester(this, context)))
    } else if (shouldBuildReplicator) {
      Some((replicatorSpec, () => new Replicator(context)))
    } else {
      Some((hunterSpec, () => new Soldier(context)))
    }
  }

  private def needMoreSlaves: Boolean =
    slaves.size < this.spec.constructors - 1

  private def shouldBuildReplicator =
    (context.droneCount('Replicator) < 2 ||
      context.harvestCoordinator.freeZoneCount > context.droneCount('Replicator) * 2) &&
      context.droneCount('Replicator) < 4

  private def isStuck: Boolean =
    slaves.isEmpty && isConstructing && !isHarvesting && storedResources == 0

  private def assessThreatLevel(): Unit = {
    val enemyFirepower = enemies.foldLeft(0)(_ + _.spec.missileBatteries)
    val strength = spec.missileBatteries + spec.shieldGenerators
    if (enemyFirepower >= strength) {
      context.battleCoordinator.requestAssistance(this)
      context.battleCoordinator.requestGuards(this, enemyFirepower - strength + 2)
      moveInDirection(position - closestEnemy.position)
    }
  }

  override def onDeath(): Unit = {
    super.onDeath()
    for (m <- nextCrystal)
      context.harvestCoordinator.abortHarvestingMission(m)
  }

  def hasSpareSlave: Boolean =
    slaves.size > 1 || slaves.size == 1 && !isConstructing

  def relieveSlave(): Option[Harvester] = {
    val s = slaves.headOption
    s.foreach(slaves -= _)
    s
  }

  def registerSlave(slave: Harvester): Unit = {
    slaves += slave
  }

  def slaveFailed(slave: Harvester): Unit = {
    slaves -= slave
  }

  override def metaController = Some(context)
}

