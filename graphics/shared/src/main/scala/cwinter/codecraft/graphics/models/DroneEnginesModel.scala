package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model.{CompositeModelBuilder, Model, ModelBuilder, StaticCompositeModel}
import cwinter.codecraft.graphics.primitives.Polygon
import cwinter.codecraft.util.maths.{ColorRGB, Geometry, VertexXY}


private[graphics] case class DroneEnginesModel(
  position: VertexXY,
  colors: DroneColors,
  playerColor: ColorRGB,
  t: Int
)(implicit rs: RenderStack)
  extends CompositeModelBuilder[DroneEnginesModel, Unit] {

  def signature: DroneEnginesModel = this


  override protected def build: (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, Unit]]) = {
    val enginePositions = Geometry.polygonVertices2(3, radius = 5, orientation = 2 * math.Pi.toFloat * t / 100)
    val engines =
      for ((offset, i) <- enginePositions.zipWithIndex)
      yield Polygon(
        rs.MaterialXYZRGB,
        5,
        playerColor,
        colors.ColorHull,
        radius = 4,
        position = position + offset,
        orientation = -2 * math.Pi.toFloat * t / 125,
        zPos = 1
      )

    (engines, Seq.empty)
  }
}
