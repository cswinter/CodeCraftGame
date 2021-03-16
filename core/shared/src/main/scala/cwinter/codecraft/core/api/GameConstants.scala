package cwinter.codecraft.core.api

object GameConstants {

  /** The largest distance at which two drones can see each other. */
  final val DroneVisionRange = 750

  /** The largest distance at which homing missiles can be fired at another drone. */
  final val MissileLockOnRange = 300

  /** The largest distance at which long range homing missiles can be fired at another drone. */
  final val LongRangeMissileLockOnRange = 600

  /** The number of timesteps that have to elapse before homing missiles can be fired again. */
  final val MissileCooldown = 30

  /** The number of timesteps that have to elapse before long range homing missiles can be fired again. */
  final val LongRangeMissileChargeup = 70

  /** The number of timesteps it takes for a missile to disappear after being fired. */
  final val MissileLifetime = 60

  /** The number of timesteps it takes for a missile to disappear after being fired. */
  final val LongRangeMissileLifetime = 70

  /** The speed of missiles measured in units distance per timestep. */
  final val MissileSpeed = 17

  /** The acceleration of missiles measured in units distance per timestep per timestep. */
  final val LongRangeMissileAcceleration = 0.5

  /** The number of timesteps it takes for shield hitpoints to increase by one (per shield generator module).
    * Cooldown resets whenever drone takes damage.
    * */
  final val ShieldRegenerationInterval = 35

  /** The amount of hitpoints provided per shield generator module. */
  final val ShieldMaximumHitpoints = 4

  /** The largest distance at which minerals can be harvested. */
  final val HarvestingRange = 70

  /** The number of timesteps it takes to build a drone is `DroneConstructionTime` *
    *  (number of modules of the drone being built) / (number constructor modules of the building drone).
    */
  final val DroneConstructionTime = 30

  /** The amount of resources required per module to build a drone */
  final val ModuleResourceCost = 3

  /** The number of timesteps it takes to harvest 1 resource from a mineral crystal. */
  final val HarvestingInterval = 60
}
