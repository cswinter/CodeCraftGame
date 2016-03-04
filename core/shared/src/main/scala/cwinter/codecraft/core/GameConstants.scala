package cwinter.codecraft.core


object GameConstants {
  /**
    * The largest distance at which two drones can see each other.
    */
  final val DroneVisionRange = 500

  /**
    * The largest distance at which homing missiles can be fired at another drone.
    */
  final val MissileLockOnRange = 300

  /**
    * The number of timesteps that have to elapse before homing missiles can be fired again.
    */
  final val MissileCooldown = 30

  /**
    * The number of timesteps it takes for a missile to disappear after being fired.
    */
  final val MissileLifetime = 50

  /**
    * The speed of missiles measured in units distance per timestep.
    */
  final val MissileSpeed = 17

  /**
    * The number of timesteps it takes for shield hitpoints to increase by one (per shield generator module).
    */
  final val ShieldRegenerationInterval = 100

  /**
    * The amount of hitpoints provided per shield generator module.
    */
  final val ShieldMaximumHitpoints = 7

  /**
    * The largest distance at which minerals can be harvested.
    */
  final val HarvestingRange = 70

  /**
    * The number of timesteps * number of constructor modules / resource cost it takes to build a drone.
    */
  final val DroneConstructionTime = 100

  /**
    * The amount of resources required to build a
    */
  final val ModuleResourceCost = 5

  /**
    * The number of timesteps it takes to harvest 1 resource from a mineral crystal.
    */
  final val HarvestingInterval = 60
}

