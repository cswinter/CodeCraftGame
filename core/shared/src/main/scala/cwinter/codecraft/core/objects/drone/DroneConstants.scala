package cwinter.codecraft.core.objects.drone


// TODO: aggregate all constants
private[core] object DroneConstants {
  // For some reason, Scala seems to inline these constants (maybe bug in Scala.js?)
  // With incremental compilation, this means changes won't take effect until the files
  // that use these constants happen to be changed (if only some files are changed, different
  // parts of the code have different values for the constants. fml)
  // Hence, these fields not defined as final.
  val MissileLockOnRadius: Int = 300
  val HarvestingRange: Int = 70
}

