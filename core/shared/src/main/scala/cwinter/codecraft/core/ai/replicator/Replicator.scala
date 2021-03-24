package cwinter.codecraft.core.ai.replicator

import cwinter.codecraft.core.ai.shared.HarvestingZone
import cwinter.codecraft.core.api.{DroneController, DroneSpec, MineralCrystal}


private[codecraft] class Replicator(ctx: ReplicatorContext) extends ReplicatorController(ctx) with TargetAcquisition {
  import context.{mothershipCoordinator, battleCoordinator, harvestCoordinator, droneCount, rng}
  val harvesterSpec = DroneSpec(storageModules = 1)
  val hunterSpec = DroneSpec(missileBatteries = 1)
  val destroyerSpec = DroneSpec(missileBatteries = 3, shieldGenerators = 1)
  val replicatorSpec = DroneSpec(storageModules = 1, constructors = 2, missileBatteries = 1)
  val shieldedReplicatorSpec = DroneSpec(storageModules = 1, constructors = 2, shieldGenerators = 1)
  val minimalReplicatorSpec = DroneSpec(storageModules = 1, constructors = 1)
  var nextReplicatorSpec = chooseNextReplicatorSpec()

  var nextCrystal: Option[MineralCrystal] = None
  var assignedZone: Option[HarvestingZone] = None
  var currentConstruction: Option[DroneSpec] = None

  lazy val normalizedStrength = math.sqrt(spec.maxHitpoints * spec.missileBatteries) / 2

  def this(greedy: Boolean = false, confident: Boolean = false, aggressive: Boolean = false) =
    this(new ReplicatorContext(greedy, confident, aggressive))

  context.isReplicatorInConstruction = true


  private var workers = Set.empty[Harvester]


  override def onSpawn(): Unit = {
    super.onSpawn()
    context.isReplicatorInConstruction = false
    mothershipCoordinator.online(this)
  }

  override def onTick(): Unit = {
    if (!isConstructing) {
      currentConstruction = None
      maybeRequestZone()
      nextConstructionSpec match {
        case Some((spec, controller))
        if shouldBeginConstruction(spec.resourceCost) =>
          buildDrone(controller(), spec)
          currentConstruction = Some(spec)
          if (spec == nextReplicatorSpec) nextReplicatorSpec = chooseNextReplicatorSpec()
        case _ =>
          harvest()
      }
    } else if (!isHarvesting) {
      for (n <- nextCrystal) {
        harvestCoordinator.abortHarvestingMission(n)
        nextCrystal = None
      }
    }

    if (!currentConstruction.contains(harvesterSpec) && needMoreSlaves)
      mothershipCoordinator.requestHarvester(this)
    if (isStuck) mothershipCoordinator.stuck(this)

    assessThreatLevel()
    handleWeapons()
  }

  def maybeRequestZone(): Unit = {
    if (assignedZone.exists(_.exhausted)) assignedZone = None
    if (assignedZone.isEmpty && spec.moduleCount != 2)
      assignedZone = harvestCoordinator.requestHarvestingZone(position)
  }

  def shouldBeginConstruction(resourceCost: Int): Boolean = {
    val allResourcesAvailable =
      storedResources >= resourceCost
    val shouldStartEarly =
      availableStorage == 0 || storedResources >= 14
    allResourcesAvailable || shouldStartEarly
  }

  def harvest(): Unit = {
    if (nextCrystal.exists(_.harvested)) nextCrystal = None
    if (nextCrystal.isEmpty && availableStorage > 0)
      nextCrystal = harvestCoordinator.findClosestMineral(position, assignedZone)
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
      Some((nextReplicatorSpec, () => new Replicator(context)))
    } else {
      Some((hunterSpec, () => new Soldier(context)))
    }
  }

  private def chooseNextReplicatorSpec(): DroneSpec =
    rng.int(10) match {
      case 0 if droneCount(classOf[Soldier]) >= 5 => shieldedReplicatorSpec
      case 1 | 2 => minimalReplicatorSpec
      case _ => replicatorSpec
    }

  private def needMoreSlaves: Boolean =
    workers.size < this.spec.constructors - 1

  private def shouldBuildReplicator = !(
    spec.constructors <= 1 ||
    (context.isReplicatorInConstruction && !context.greedy) ||
    context.aggressive ||
    battleCoordinator.needMoreSoldiers ||
    (
      droneCount(classOf[Replicator]) >= 2 &&
      harvestCoordinator.freeZoneCount <= droneCount(classOf[Replicator]) * 2
    )
  )

  private def isStuck: Boolean =
    workers.isEmpty && isConstructing && !isHarvesting && storedResources == 0

  private def assessThreatLevel(): Unit = {
    val enemyStrength = normalizedEnemyCount
    if (enemyStrength > 0) {
      battleCoordinator.requestGuards(this, math.ceil(enemyStrength).toInt)
      battleCoordinator.requestAssistance(this)
      if (spec.shieldGenerators == 0) moveInDirection(position - closestEnemy.position)
    }
  }

  override def handleWeapons(): Unit =
    if (missileCooldown <= 0) {
      val targetOption = optimalTarget
      for (target <- targetOption)
        fireMissilesAt(target)
      maybeClosest = targetOption.filter(_.spec.missileBatteries > 0)
    }

  override def onDeath(): Unit = {
    super.onDeath()
    for (m <- nextCrystal)
      harvestCoordinator.abortHarvestingMission(m)
    mothershipCoordinator.offline(this)
    maybeClosest = None
  }

  override def onConstructionCancelled(): Unit = {
    super.onConstructionCancelled()
    context.isReplicatorInConstruction = false
  }

  def hasSpareSlave: Boolean =
    workers.size > 1 || workers.size == 1 && !isConstructing

  def hasPlentySlaves: Boolean =
    workers.size > 1 && storedResources > 10

  def relieveSlave(): Option[Harvester] = {
    val s = workers.headOption
    s.foreach(workers -= _)
    s
  }

  def registerSlave(worker: Harvester): Unit = {
    workers += worker
  }

  def workerFailed(worker: Harvester): Unit = {
    workers -= worker
  }
}

