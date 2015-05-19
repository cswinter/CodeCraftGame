package cwinter.codinggame.core.api

import cwinter.codinggame.core.objects.drone._
import cwinter.codinggame.util.maths.{Geometry, Vector2}
import cwinter.codinggame.util.modules.ModulePosition

case class DroneSpec(
  size: Int,
  storageModules: Int = 0,
  missileBatteries: Int = 0,
  processingModules: Int = 0,
  manipulatorModules: Int = 0,
  engineModules: Int = 0,
  shieldGenerators: Int = 0,
  isMothership: Boolean = false // TODO: remove or only allow this to be set by core
) {
  require(storageModules >= 0)
  require(missileBatteries >= 0)
  require(processingModules >= 0)
  require(manipulatorModules >= 0)
  require(engineModules >= 0)
  require(shieldGenerators >= 0)

  val moduleCount =
    storageModules + missileBatteries + processingModules +
      manipulatorModules + engineModules + shieldGenerators

  require(ModulePosition.moduleCount(size) == moduleCount)



  import DroneSpec._

  def resourceCost: Int = ModulePosition.moduleCount(size) * ResourceCost
  def buildTime: Int = ConstructionPeriod * (size - 1)
  def resourceDepletionPeriod: Int = buildTime / resourceCost // TODO: this is not always accurate bc integer division
  def weight = if (isMothership) 10000 else size + moduleCount
  def maximumSpeed: Double = 1000 * (1 + engineModules)  / weight
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

  private[core] def constructProcessingModules(owner: Drone): Option[DroneProcessingModule] =
    if (processingModules > 0) {
      val startIndex = storageModules + missileBatteries
      Some(new DroneProcessingModule(startIndex until startIndex + processingModules, owner))
    } else None

  private[core] def constructManipulatorModules(owner: Drone): Option[DroneManipulatorModule] =
    if (manipulatorModules > 0) {
      val startIndex = storageModules + missileBatteries + processingModules
      Some(new DroneManipulatorModule(startIndex until startIndex + manipulatorModules, owner))
    } else None

  private[core] def constructEngineModules(owner: Drone): Option[DroneEnginesModule] =
    if (engineModules > 0) {
      val startIndex = storageModules + missileBatteries + processingModules + manipulatorModules
      Some(new DroneEnginesModule(startIndex until startIndex + engineModules, owner))
    } else None

  private[core] def constructShieldGenerators(owner: Drone): Option[DroneShieldGeneratorModule] =
    if (shieldGenerators > 0) {
      val startIndex = storageModules + missileBatteries + processingModules +
        manipulatorModules + engineModules
      Some(new DroneShieldGeneratorModule(startIndex until startIndex + shieldGenerators, owner))
    } else None
}


object DroneSpec {
  // constants for drone construction
  final val ConstructionPeriod = 50
  final val ResourceCost = 2
  final val SideLength = 40
}
