package cwinter.codecraft.core


/** Aggregates various display settings.
  * @param allowMessages Whether drones are allowed to display warnings or user generated messages.
  * @param showSightRadius If true, the sight radius for all drones is shown.
  * @param showMissileRadius If true, the missile radius for all drones is shown.
  */
class Settings(
  var allowMessages: Boolean = true,
  var showSightRadius: Boolean = false,
  var showMissileRadius: Boolean = false
)

