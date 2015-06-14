package cwinter.codecraft.core.ai.basicplus

import cwinter.codecraft.core.api._
import cwinter.codecraft.util.maths.Vector2


case class SearchToken(x: Int, y: Int) {
  val pos: Vector2 = Vector2((x + 0.5) * DroneSpec.SightRadius, (y + 0.5) * DroneSpec.SightRadius)
}
