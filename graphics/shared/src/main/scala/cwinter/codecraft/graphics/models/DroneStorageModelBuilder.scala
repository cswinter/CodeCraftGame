package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model._
import cwinter.codecraft.graphics.worldstate.{EmptyStorage, EnergyStorage, MineralStorage, StorageModuleContents}
import cwinter.codecraft.util.maths.{ColorRGB, ColorRGBA, Vector2, VertexXY}
import cwinter.codecraft.util.modules.ModulePosition


private[graphics] case class DroneStorageModelBuilder(
  position: VertexXY,
  colors: DroneColors,
  moduleContents: StorageModuleContents,
  mineralPosition: Option[Vector2]
)(implicit rs: RenderStack) extends ModelBuilder[DroneStorageModelBuilder, Unit] {

  val radius = 8
  val outlineWidth = 1


  def signature = this

  protected def buildModel: Model[Unit] = {
    import colors._
    val body =
      Polygon(
        material = rs.MaterialXYZRGB,
        n = 20,
        colorMidpoint = ColorBackplane,
        colorOutside = ColorBackplane,
        radius = radius - outlineWidth,
        position = position,
        zPos = 1
      ).getModel

    val hull =
      PolygonRing(
        material = rs.MaterialXYZRGB,
        n = 20,
        colorInside = ColorHull,
        colorOutside = ColorHull,
        innerRadius = radius - outlineWidth,
        outerRadius = radius,
        position = position,
        zPos = 1
      ).getModel

    val contents = moduleContents match {
      case EnergyStorage(filledSlots) =>
        for {
          i <- 0 until 7
          if filledSlots.contains(i)
          globePos = ModulePosition.energyPosition(i) + position
        } yield EnergyGlobeModelFactory.build(globePos).getModel
      case MineralStorage =>
        Seq(
          Polygon(
            rs.MaterialXYZRGB,
            n = 5,
            colorMidpoint = ColorRGB(0.1f, 0.75f, 0.1f),
            colorOutside = ColorRGB(0.0f, 0.3f, 0.0f),
            radius = 6.5f,
            zPos = 2,
            position = position
          ).getModel
        )
      case EmptyStorage => Seq()
    }


    val beamModel = mineralPosition.map(buildBeamModel)

    new StaticCompositeModel((body +: hull +: contents) ++ beamModel.toSeq)
  }

  private def buildBeamModel(mineralPosition: Vector2): Model[Unit] = {
    val dist = position.toVector2 - mineralPosition
    val angle =
      if (dist.x == 0 && dist.y == 0) 0
      else (position.toVector2 - mineralPosition).orientation.toFloat

    val n = 5
    val n2 = n - 1
    val midpoint = (n2 - 1) / 2f
    val range = (n2 + 1) / 2f
    PartialPolygon(
      rs.TranslucentAdditive,
      n,
      Seq.fill(n)(ColorRGBA(0.5f, 1f, 0.5f, 0.7f)),
      ColorRGBA(0, 0, 0, 0) +: Seq.tabulate(n2)(i => {
        val color = 1 - Math.abs(i - midpoint) / range
        ColorRGBA(color / 2, color, color / 2, 0)
      }).flatMap(x => Seq(x, x)) :+ ColorRGBA(0, 0, 0, 0),
      75,
      position,
      0,
      angle,
      fraction = 0.03f
    ).getModel
  }
}

