package cwinter.codecraft.core.ai.shared

import cwinter.codecraft.core.api.GameConstants.DroneVisionRange
import cwinter.codecraft.util.maths.{Rectangle, Vector2}

import scala.collection.mutable


class SearchCoordinator(worldSize: Rectangle) {
  private var searchTokens: Set[SearchToken] = genSearchTokens
  private val dangerousSearchTokens = mutable.Queue.empty[SearchToken]
  private var cooldown = 300

  private def genSearchTokens: Set[SearchToken] = {
    val width = math.ceil(worldSize.width / DroneVisionRange).toInt
    val height = math.ceil(worldSize.height / DroneVisionRange).toInt
    val xOffset = (worldSize.width / DroneVisionRange / 2).toInt
    val yOffset = (worldSize.height / DroneVisionRange / 2).toInt
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

  def dangerous(searchToken: SearchToken): Unit = {
    dangerousSearchTokens.enqueue(searchToken)
  }

  def returnSearchToken(searchToken: SearchToken): Unit = {
    searchTokens += searchToken
  }

  def update(): Unit = {
    if (dangerousSearchTokens.nonEmpty) {
      cooldown -= 1
      if (cooldown <= 0) {
        cooldown = 300
        searchTokens += dangerousSearchTokens.dequeue()
      }
    }
  }
}

