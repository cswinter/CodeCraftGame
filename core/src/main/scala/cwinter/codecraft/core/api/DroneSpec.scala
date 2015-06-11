package cwinter.codecraft.core.api

import cwinter.codecraft.core.objects.drone._
import cwinter.codecraft.util.maths.{Geometry, Vector2}
import cwinter.codecraft.util.modules.ModulePosition

/**
 * @param size The size of the drone (number of sides/edges). Allowed values are from 3 to 7.
 * @param storageModules Number of storage modules. Allows for storage of mineral crystals and energy globes.
 * @param missileBatteries Number of missile batteries. Allows for firing homing missiles.
 * @param refineries Number of refineries. Allows for processing mineral crystals into energy globes.
 * @param manipulators Number of manipulators. Allows for constructing new drones and moving minerals from/to other drones.
 * @param engines Number of engines. Increases move speed.
 * @param shieldGenerators Number of shield generators. Create shield that absorbs damage and regenerates over time.
 */
case class DroneSpec(
  size: Int,
  storageModules: Int = 0,
  missileBatteries: Int = 0,
  refineries: Int = 0,
  manipulators: Int = 0,
  engines: Int = 0,
  shieldGenerators: Int = 0
) {
  require(storageModules >= 0)
  require(missileBatteries >= 0)
  require(refineries >= 0)
  require(manipulators >= 0)
  require(engines >= 0)
  require(shieldGenerators >= 0)

  val moduleCount =
    storageModules + missileBatteries + refineries +
      manipulators + engines + shieldGenerators

  require(ModulePosition.moduleCount(size) == moduleCount)



  import DroneSpec._

  def resourceCost: Int = ModulePosition.moduleCount(size) * ResourceCost
  def buildTime: Int = ConstructionPeriod * (size - 1)
  def resourceDepletionPeriod: Int = buildTime / resourceCost // TODO: this is not always accurate bc integer division
  def weight = size + moduleCount
  def maximumSpeed: Double = 1000 * (1 + engines)  / weight
  def requiredFactories: Int = ModulePosition.moduleCount(size) * 2
  val radius: Double = {
    val radiusBody = 0.5f * SideLength / math.sin(math.Pi / size).toFloat
    radiusBody + 0.5f * Geometry.circumradius(4, size)
  }


  private[core] def constructDynamics(owner: Drone, initialPos: Vector2, time: Double): DroneDynamics =
    new DroneDynamics(owner, maximumSpeed, weight, radius, initialPos, time)

  private[core] def constructStorage(owner: Drone, startingResources: Int = 0): Option[DroneStorageModule] =
    if (storageModules > 0) Some(
      new DroneStorageModule(0 until storageModules, owner, startingResources)
    )
    else None

  private[core] def constructMissilesBatteries(owner: Drone): Option[DroneMissileBatteryModule] =
    if (missileBatteries > 0) Some(
      new DroneMissileBatteryModule(storageModules until (storageModules + missileBatteries), owner)
    )
    else None

  private[core] def constructRefineries(owner: Drone): Option[DroneRefineryModule] =
    if (refineries > 0) {
      val startIndex = storageModules + missileBatteries
      Some(new DroneRefineryModule(startIndex until startIndex + refineries, owner))
    } else None

  private[core] def constructManipulatorModules(owner: Drone): Option[DroneManipulatorModule] =
    if (manipulators > 0) {
      val startIndex = storageModules + missileBatteries + refineries
      Some(new DroneManipulatorModule(startIndex until startIndex + manipulators, owner))
    } else None

  private[core] def constructEngineModules(owner: Drone): Option[DroneEnginesModule] =
    if (engines > 0) {
      val startIndex = storageModules + missileBatteries + refineries + manipulators
      Some(new DroneEnginesModule(startIndex until startIndex + engines, owner))
    } else None

  private[core] def constructShieldGenerators(owner: Drone): Option[DroneShieldGeneratorModule] =
    if (shieldGenerators > 0) {
      val startIndex = storageModules + missileBatteries + refineries +
        manipulators + engines
      Some(new DroneShieldGeneratorModule(startIndex until startIndex + shieldGenerators, owner))
    } else None
}


object DroneSpec {
  // constants for drone construction
  final val ConstructionPeriod = 100
  final val ResourceCost = 5
  final val SideLength = 40
  final val SightRadius = 500
}
