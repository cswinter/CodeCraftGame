package cwinter.codecraft.core.ai.destroyer

import cwinter.codecraft.core.ai.shared.HarvestingZone
import cwinter.codecraft.core.api.{DroneController, DroneSpec, MineralCrystal}


class Mothership(ctx: DestroyerContext) extends DestroyerController(ctx) {
  val harvesterSpec = DroneSpec(storageModules = 1)
  val scoutSpec = DroneSpec(engines = 1)
  val destroyerSpec = DroneSpec(shieldGenerators = 2, missileBatteries = 2)
  val destroyerSpec2 = DroneSpec(shieldGenerators = 3, missileBatteries = 3, engines = 1)
  val battlecruiserSpec = DroneSpec(shieldGenerators = 3, missileBatteries = 4, engines = 3)

  var nextCrystal: Option[MineralCrystal] = None
  var assignedZone: Option[HarvestingZone] = None
  var currentConstruction: Option[DroneSpec] = None
  var lastOneLarge = false

  def this() = this(new DestroyerContext)


  override def onSpawn(): Unit = {
    super.onSpawn()
    context.initialise(worldSize, this)
  }

  override def onTick(): Unit = {
    if (!isConstructing) {
      currentConstruction = None
      nextConstructionSpec match {
        case Some((spec, controller))
        if shouldBeginConstruction(spec.resourceCost) =>
          buildDrone(spec, controller())
          currentConstruction = Some(spec)
          if (spec == battlecruiserSpec || spec == destroyerSpec2) lastOneLarge = true
          else if (spec == destroyerSpec) lastOneLarge = false
        case _ =>
          harvest()
      }
    } else if (!isHarvesting) {
      for (n <- nextCrystal) {
        context.harvestCoordinator.abortHarvestingMission(n)
        nextCrystal = None
      }
    }

    handleWeapons()
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
      nextCrystal = context.harvestCoordinator.findClosestMineral(position)
    for (m <- nextCrystal if !isHarvesting) moveTo(m)
  }

  override def onArrivesAtMineral(m: MineralCrystal): Unit = {
    if (!m.harvested) {
      harvest(m)
    }
  }

  private def nextConstructionSpec: Option[(DroneSpec, () => DroneController)] = {
    if (context.droneCount(classOf[Harvester]) < 2) {
      Some((harvesterSpec, () => new Harvester(context)))
    } else if (context.droneCount(classOf[Scout]) < 2 && context.battleCoordinator.needScouting) {
      Some((scoutSpec, () => new Scout(context)))
    } else if (context.droneCount(classOf[Destroyer]) < 4 || lastOneLarge) {
      Some((destroyerSpec, () => new Destroyer(context)))
    } else if (context.droneCount(classOf[Destroyer]) < 6) {
      Some((destroyerSpec2, () => new Destroyer(context)))
    } else {
      Some((battlecruiserSpec, () => new Destroyer(context)))
    }
  }
}

