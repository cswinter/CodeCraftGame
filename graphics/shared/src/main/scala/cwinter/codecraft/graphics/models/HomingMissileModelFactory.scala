package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model.{EmptyModel, QuadStrip}
import cwinter.codecraft.graphics.worldstate.Player
import cwinter.codecraft.util.maths.{ColorRGB, ColorRGBA, VertexXY}


object HomingMissileModelFactory {
  def build(positions: Seq[(Float, Float)], nMaxPos: Int, player: Player)(implicit rs: RenderStack) = {
    if (positions.length < 2) EmptyModel
    else {
      val midpoints = positions.map { case (x, y) => VertexXY(x, y)}
      val n = nMaxPos // positions.length
      val colorHead = ColorRGB(1, 1, 1)
      val colorTail = player.color
      val colors = positions.zipWithIndex.map {
        case (_, index) =>
          val x = index / (n - 1).toFloat
          val z = x * x
          ColorRGBA(z * colorHead + (1 - z) * colorTail, x)
      }



      QuadStrip(
        rs.TranslucentAdditive,
        midpoints,
        colors,
        2,
        zPos = 3
      ).noCaching.getModel
    }
  }
}