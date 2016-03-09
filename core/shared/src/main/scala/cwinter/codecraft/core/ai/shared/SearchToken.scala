package cwinter.codecraft.core.ai.shared

import cwinter.codecraft.core.api.GameConstants.DroneVisionRange
import cwinter.codecraft.util.maths.Vector2


private[codecraft] case class SearchToken(x: Int, y: Int) {
  val pos: Vector2 = Vector2((x + 0.5) * DroneVisionRange, (y + 0.5) * DroneVisionRange)
}
