package cwinter.codecraft.core.graphics

import cwinter.codecraft.graphics.engine.WorldObjectDescriptor
import cwinter.codecraft.graphics.model.{CompositeModelBuilder, ModelBuilder}
import cwinter.codecraft.graphics.primitives.PartialPolygon
import cwinter.codecraft.util.maths.{ColorRGB, ColorRGBA, Vector2, VertexXY}
import cwinter.codecraft.util.modules.ModulePosition

/**
  * Created by clemens on 4/21/16.
  */
private[codecraft] case class ConstructionBeamsModelBuilder(
  droneSize: Int,
  modules: Seq[(Int, Boolean)],
  constructionDisplacement: Vector2,
  playerColor: ColorRGB
) extends CompositeModelBuilder[ConstructionBeamsModelBuilder, Unit] with WorldObjectDescriptor[Unit] {

  override protected def buildSubcomponents: (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, Unit]]) = {
    val beams =
      for {
        (moduleIndex, active)<- modules
        modulePosition = ModulePosition(droneSize, moduleIndex)
      } yield constructionBeamModel(modulePosition, active)

    (beams, Seq.empty)
  }

  private def constructionBeamModel(position: VertexXY, active: Boolean): ModelBuilder[_, Unit] = {
    val displacement = position.toVector2 - constructionDisplacement
    val angle =
      if (displacement.x == 0 && displacement.y == 0) 0
      else displacement.orientation.toFloat
    val radius = displacement.length
    val focusColor =
      if (active) ColorRGBA(0.5f * playerColor + 0.5f * ColorRGB(1, 1, 1), 0.9f)
      else ColorRGBA(playerColor, 0.7f)

    val width = 50
    val alpha = math.Pi - 2 * math.atan2(radius, width / 2)

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
      radius.toFloat,
      position,
      0,
      angle,
      fraction = (alpha / (2 * math.Pi)).toFloat
    )
  }

  override def signature = this
  override protected def createModel(timestep: Int) = getModel

}
