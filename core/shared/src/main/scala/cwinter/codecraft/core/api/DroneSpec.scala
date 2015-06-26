package cwinter.codecraft.core.api

import cwinter.codecraft.core.objects.drone._
import cwinter.codecraft.util.maths.{Geometry, Vector2}
import cwinter.codecraft.util.modules.ModulePosition

/**
 * Specifies the modules equipped by a drone.
 *
 * @param storageModules Number of storage modules. Allows for storage of mineral crystals and energy globes.
 * @param missileBatteries Number of missile batteries. Allows for firing homing missiles.
 * @param refineries Number of refineries. Allows for processing mineral crystals into energy globes.
 * @param constructors Number of constructors. Allows for constructing new drones and moving minerals from/to other drones.
 * @param engines Number of engines. Increases move speed.
 * @param shieldGenerators Number of shield generators. Create shield that absorbs damage and regenerates over time.
 */
case class DroneSpec(
  storageModules: Int = 0,
  missileBatteries: Int = 0,
  refineries: Int = 0,
  constructors: Int = 0,
  engines: Int = 0,
  shieldGenerators: Int = 0
) {
  require(storageModules >= 0)
  require(missileBatteries >= 0)
  require(refineries >= 0)
  require(constructors >= 0)
  require(engines >= 0)
  require(shieldGenerators >= 0)

  val moduleCount =
    storageModules + missileBatteries + refineries +
      constructors + engines + shieldGenerators

  require(moduleCount <= ModulePosition.MaxModules, s"A drone cannot have more than ${ModulePosition.MaxModules} modules")

  val size = ModulePosition.size(moduleCount)



  import DroneSpec._

  def resourceCost: Int = ModulePosition.moduleCount(size) * ResourceCost
  def buildTime: Int = ConstructionPeriod * resourceCost
  def weight = size + moduleCount
  def maximumSpeed: Double = 1000 * (1 + engines)  / weight
  val radius: Double = {
    val radiusBody = 0.5f * SideLength / math.sin(math.Pi / size).toFloat
    radiusBody + 0.5f * Geometry.circumradius(4, size)
  }


  private[core] def constructDynamics(owner: DroneImpl, initialPos: Vector2, time: Double): DroneDynamics =
    new DroneDynamics(owner, maximumSpeed, weight, radius, initialPos, time)

  private[core] def constructStorage(owner: DroneImpl, startingResources: Int = 0): Option[DroneStorageModule] =
    if (storageModules > 0) Some(
      new DroneStorageModule(0 until storageModules, owner, startingResources)
    )
    else None

  private[core] def constructMissilesBatteries(owner: DroneImpl): Option[DroneMissileBatteryModule] =
    if (missileBatteries > 0) Some(
      new DroneMissileBatteryModule(storageModules until (storageModules + missileBatteries), owner)
    )
    else None

  private[core] def constructRefineries(owner: DroneImpl): Option[DroneRefineryModule] =
    if (refineries > 0) {
      val startIndex = storageModules + missileBatteries
      Some(new DroneRefineryModule(startIndex until startIndex + refineries, owner))
    } else None

  private[core] def constructManipulatorModules(owner: DroneImpl): Option[DroneManipulatorModule] =
    if (constructors > 0) {
      val startIndex = storageModules + missileBatteries + refineries
      Some(new DroneManipulatorModule(startIndex until startIndex + constructors, owner))
    } else None

  private[core] def constructEngineModules(owner: DroneImpl): Option[DroneEnginesModule] =
    if (engines > 0) {
      val startIndex = storageModules + missileBatteries + refineries + constructors
      Some(new DroneEnginesModule(startIndex until startIndex + engines, owner))
    } else None

  private[core] def constructShieldGenerators(owner: DroneImpl): Option[DroneShieldGeneratorModule] =
    if (shieldGenerators > 0) {
      val startIndex = storageModules + missileBatteries + refineries +
        constructors + engines
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
