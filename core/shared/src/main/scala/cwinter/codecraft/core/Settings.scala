package cwinter.codecraft.core


/** Aggregates various display settings.
  * @param allowMessages Drones are allowed to display warnings or user generated messages.
  * @param showSightRadius If true, the sight radius for all drones is shown.
  * @param showMissileRadius If true, the missile radius for all drones is shown.
  * @param allowEnergyGlobeAnimation Energy globes move between drones and modules, rather then teleporting instantly.
  * @param allowMissileAnimation Homing missiles have a trail that shows their path.
  * @param allowModuleAnimation Animate modules such as engines.
  */
class Settings(
  var allowMessages: Boolean = true,
  var showSightRadius: Boolean = false,
  var showMissileRadius: Boolean = false,
  var allowEnergyGlobeAnimation: Boolean = true,
  var allowMissileAnimation: Boolean = true,
  var allowModuleAnimation: Boolean = true
) {
  /** Sets all graphics options to the fastest setting. */
  def setFastestGraphics(): Unit = {
    allowEnergyGlobeAnimation = false
    allowMissileAnimation = false
    allowModuleAnimation = false
  }

  /** Sets all graphics options to their best setting */
  def setBestGraphics(): Unit = {
    allowEnergyGlobeAnimation = true
    allowMissileAnimation = true
    allowModuleAnimation = true
  }
}

