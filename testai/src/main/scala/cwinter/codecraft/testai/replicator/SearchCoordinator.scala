package cwinter.codecraft.testai.replicator

import cwinter.codecraft.core.api.DroneSpec
import cwinter.codecraft.util.maths.{Vector2, Rectangle}


class SearchCoordinator(worldSize: Rectangle) {
  var searchTokens: Set[SearchToken] = genSearchTokens

  private def genSearchTokens: Set[SearchToken] = {
    val width = math.ceil(worldSize.width / DroneSpec.SightRadius).toInt
    val height = math.ceil(worldSize.height / DroneSpec.SightRadius).toInt
    val xOffset = (worldSize.width / DroneSpec.SightRadius / 2).toInt
    val yOffset = (worldSize.height / DroneSpec.SightRadius / 2).toInt
    val tokens = Seq.tabulate(width, height){
      (x, y) => SearchToken(x - xOffset, y - yOffset)
    }
    for (ts <- tokens; t <- ts) yield t
  }.toSet


  def getSearchToken(pos: Vector2): Option[SearchToken] = {
    if (searchTokens.isEmpty) {
      None
    } else {
      val closest = searchTokens.minBy(t => (t.pos - pos).lengthSquared)
      searchTokens -= closest
      Some(closest)
    }
  }

  def returnSearchToken(searchToken: SearchToken): Unit = {
    searchTokens += searchToken
  }
}

