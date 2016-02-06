package cwinter.codecraft.testai.replicator

import cwinter.codecraft.core.api.DroneSpec
import cwinter.codecraft.util.maths.Vector2


case class SearchToken(x: Int, y: Int) {
  val pos: Vector2 = Vector2((x + 0.5) * DroneSpec.SightRadius, (y + 0.5) * DroneSpec.SightRadius)
}
