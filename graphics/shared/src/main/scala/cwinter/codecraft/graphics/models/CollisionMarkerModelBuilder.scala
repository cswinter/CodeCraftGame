package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.materials.Intensity
import cwinter.codecraft.graphics.model._
import cwinter.codecraft.graphics.primitives.PartialPolygonRing
import cwinter.codecraft.graphics.worldstate.CollisionMarker
import cwinter.codecraft.util.maths.{ColorRGBA, NullVectorXY}


private[graphics] case class CollisionMarkerModelBuilder(
  signature: CollisionMarker
)(implicit rs: RenderStack) extends CompositeModelBuilder[CollisionMarker, Float] {
  import signature._
  val colorGradient = IndexedSeq.tabulate(15)(i => 1 - math.abs(i.toFloat - 7f) / 7f)

  override protected def buildSubcomponents: (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, Float]]) = {
    val marker =
      PartialPolygonRing(
        position = NullVectorXY,
        orientation = orientation,
        zPos = 2,
        material = rs.TranslucentAdditivePIntensity,
        n = 14,
        colorInside = Seq.tabulate(15)(i => ColorRGBA(DefaultDroneColors.ColorThrusters, colorGradient(i) * 0f)),
        colorOutside = Seq.tabulate(15)(i => ColorRGBA(DefaultDroneColors.White, colorGradient(i) * 0.7f)),
        outerRadius = radius,
        innerRadius = radius - 10,
        fraction = 0.2f
      ).wireParameters[Float](Intensity)

    (Seq.empty, Seq(marker))
  }
}

