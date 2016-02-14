package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model._
import cwinter.codecraft.util.maths.{Vector2, ColorRGB, ColorRGBA, VertexXY}


private[graphics] case class DroneManipulatorModelBuilder(
  colors: DroneColors,
  playerColor: ColorRGB,
  position: VertexXY,
  constructionPosition: Option[Vector2],
  active: Boolean
)(implicit rs: RenderStack) extends ModelBuilder[DroneManipulatorModelBuilder, Unit] {
  override def signature: DroneManipulatorModelBuilder = this

  override protected def buildModel: Model[Unit] = {
    val module = Polygon(
      rs.GaussianGlow,
      20,
      ColorRGBA(0.5f * playerColor + 0.5f * colors.White, 1),
      ColorRGBA(colors.White, 0),
      radius = 8,
      position = position,
      zPos = 1,
      orientation = 0,
      colorEdges = true
    ).getModel

    constructionPosition match {
      case None => module
      case Some(pos) => new StaticCompositeModel(Seq(
        module,
        constructionBeamModel(pos)
      ))
    }
  }

  private def constructionBeamModel(mineralPosition: Vector2): Model[Unit] = {
    val dist = position.toVector2 - mineralPosition
    val angle =
      if (dist.x == 0 && dist.y == 0) 0
      else (position.toVector2 - mineralPosition).orientation.toFloat
    val radius = dist.length.toFloat
    val focusColor =
      if (active) ColorRGBA(0.5f * playerColor + 0.5f * colors.White, 0.9f)
      else ColorRGBA(playerColor, 0.7f)

    val n = 5
    val n2 = n - 1
    val midpoint = (n2 - 1) / 2f
    val range = (n2 + 1) / 2f
    PartialPolygon(
      rs.TranslucentAdditive,
      n,
      Seq.fill(n)(focusColor),
      ColorRGBA(0, 0, 0, 0) +: Seq.tabulate(n2)(i => {
        val color = 1 - Math.abs(i - midpoint) / range
        ColorRGBA(playerColor * color, 0)
      }).flatMap(x => Seq(x, x)) :+ ColorRGBA(0, 0, 0, 0),
      radius,
      position,
      0,
      angle,
      fraction = 0.05f
    ).getModel
  }
}

