package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.model._
import RobotColors._
import Geometry._


class RobotThrusterTrailsModelFactory(
  val sideLength: Float,
  val radiusHull: Float,
  val sides: Int
)(implicit rs: RenderStack) {
  def buildModel(positions: Seq[(Float, Float, Float)]): Model[Unit] = {
    val n = positions.length


    val trailPositions =
      for (((x, y, a), t) <- positions.zipWithIndex.reverse)
      yield {
        val drift = -VertexXY(a) * (n - t - 1) * 2.0f
        val offset = VertexXY(x, y) + drift
        (computeThrusterPos(1, a) + offset, computeThrusterPos(-1, a) + offset)
      }

    val (trail1, trail2) = trailPositions.unzip
    val colorsInside = trail1.indices.map(
      index => {
        val x = index / n.toFloat
        ColorRGBA(x * ColorThrusters + (1 - x) * White, 1 - x)
      })
    val colorsOutside = trail1.indices.map(index => ColorRGBA(ColorThrusters, 0))

    new StaticCompositeModel(Seq(
      RichQuadStrip(
        rs.TranslucentAdditive,
        trail1,
        colorsInside,
        colorsOutside,
        sideLength * 0.5f
      ).noCaching.getModel,
      RichQuadStrip(
        rs.TranslucentAdditive,
        trail2,
        colorsInside,
        colorsOutside,
        sideLength * 0.5f
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
