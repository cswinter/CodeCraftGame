package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model.{EmptyModel, Model, StaticCompositeModel}
import cwinter.codecraft.graphics.primitives.RichQuadStrip
import cwinter.codecraft.util.maths.Geometry._
import cwinter.codecraft.util.maths.{ColorRGB, ColorRGBA, VertexXY}


private[graphics] class DroneThrusterTrailsModelFactory(
  val sideLength: Float,
  val radiusHull: Float,
  val sides: Int,
  val playerColor: ColorRGB
)(implicit rs: RenderStack) {
  def buildModel(positions: Seq[(Float, Float, Float)]): Model[Unit] = {
    val n = positions.length

    if (n <= 1) {
      return new EmptyModel()
    }


    val trailPositions =
      for (((x, y, a), t) <- positions.zipWithIndex.reverse)
      yield {
        val drift = -VertexXY(a) * (n - t - 1) * 2.0f
        val offset = VertexXY(x, y) + drift
        (computeThrusterPos(1, a) + offset, computeThrusterPos(-1, a) + offset)
      }

    val (trail1, trail2) = trailPositions.unzip
    val colors = trailPositions.indices.map(index => ColorRGBA(playerColor, (n - index) / n.toFloat))

    new StaticCompositeModel(Seq(
      RichQuadStrip(
        rs.TranslucentAdditive,
        trail1,
        colors,
        colors,
        sideLength * 0.4f
      ).noCaching.getModel,
      RichQuadStrip(
        rs.TranslucentAdditive,
        trail2,
        colors,
        colors,
        sideLength * 0.4f
      ).noCaching.getModel
    )).identityModelview
  }

  def computeThrusterPos(side: Int, angle: Float = 0): VertexXY = {
    val perp = outerModulePerpendicular(0, angle)
    outerModulePosition(0, angle) + side * sideLength * 0.3f * perp
  }

  def outerModulePosition(n: Int, orientationOffset: Float = 0): VertexXY = {
    val r = inradius(radiusHull, sides)
    r * outerModuleNormal(n, orientationOffset)
  }

  def outerModuleNormal(n: Int, orientationOffset: Float = 0): VertexXY = {
    val angle = math.Pi + (2 * n * math.Pi / sides) + orientationOffset
    VertexXY(angle)
  }

  def outerModulePerpendicular(n: Int, orientationOffset: Float = 0): VertexXY = {
    outerModuleNormal(n, orientationOffset).perpendicular
  }
}
