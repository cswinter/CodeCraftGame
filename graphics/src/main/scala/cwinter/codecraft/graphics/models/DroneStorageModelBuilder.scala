package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model._
import cwinter.codecraft.graphics.models.DroneColors._
import cwinter.codecraft.util.maths.{ColorRGB, Geometry, VertexXY}
import cwinter.codecraft.util.modules.ModulePosition
import cwinter.codecraft.worldstate.{EmptyStorage, MineralStorage, EnergyStorage, StorageModuleContents}


case class DroneStorageModelBuilder(positions: Seq[VertexXY], moduleContents: StorageModuleContents, size: Int, tMerge: Option[Float])(implicit rs: RenderStack)
  extends ModelBuilder[DroneStorageModelBuilder, Unit] {

  val scale = math.sqrt(size).toFloat
  val radius = 8 * scale
  val outlineWidth = 1 * scale
  val center = positions.reduce(_ + _) / size


  def signature = this

  protected def buildModel: Model[Unit] = {
    tMerge match {
      case None => assembledModel
      case Some(x) =>
        val scale = 1 - x + x * math.sqrt(size).toFloat
        val xPos = if (x > 0.5f) 2 * (x - 0.5f) else 0
        val xFrac = if (x < 0.8f) x / 0.8f else 1

        val o0 = (center - positions.head).direction
        val components =
          for {
            (pos, i) <- positions.zipWithIndex
            position = (1 - xPos) * pos + xPos * center
            orientation = o0 + 2 * math.Pi.toFloat * i / size
            c <- partialModel(position, scale, orientation, xFrac)
          } yield c

        new StaticCompositeModel(components)
    }
  }


  private def partialModel(position: VertexXY, scale: Float, orientation: Float, progress: Float): Seq[Model[Unit]] = {
    val radius = 8 * scale
    val outlineWidth = 1 * scale
    val fraction = 1 - progress + progress / size
    val body =
      PartialPolygon(
        material = rs.MaterialXYRGB,
        n = 20,
        colorMidpoint = Seq.fill(20)(ColorBackplane),
        colorOutside = Seq.fill(20)(ColorBackplane),
        radius = radius - outlineWidth,
        position = position,
        zPos = 1,
        fraction = fraction,
        orientation = orientation
      ).getModel

    val hull =
      PartialPolygonRing(
        material = rs.MaterialXYRGB,
        n = 20,
        colorInside = Seq.fill(20)(ColorHull),
        colorOutside = Seq.fill(20)(ColorHull),
        innerRadius = radius - outlineWidth,
        outerRadius = radius,
        position = position,
        zPos = 1,
        fraction = fraction,
        orientation = orientation
      ).getModel

    Seq(body, hull)
  }

  private def assembledModel: Model[Unit] = {
    val body =
      Polygon(
        material = rs.MaterialXYRGB,
        n = 20,
        colorMidpoint = ColorBackplane,
        colorOutside = ColorBackplane,
        radius = radius - outlineWidth,
        position = center,
        zPos = 1
      ).getModel

    val hull =
      PolygonRing(
        material = rs.MaterialXYRGB,
        n = 20,
        colorInside = ColorHull,
        colorOutside = ColorHull,
        innerRadius = radius - outlineWidth,
        outerRadius = radius,
        position = center,
        zPos = 1
      ).getModel

    val contents = moduleContents match {
      case EnergyStorage(filledSlots) =>
        for {
          i <- 0 until 7
          if filledSlots.contains(i)
          position = ModulePosition.energyPosition(i) + center
        } yield EnergyGlobeModelFactory.build(position).getModel
      case MineralStorage =>
        Seq(
          Polygon(
            rs.MaterialXYRGB,
            n = 5,
            colorMidpoint = ColorRGB(0.1f, 0.75f, 0.1f),
            colorOutside = ColorRGB(0.0f, 0.3f, 0.0f),
            radius = 6.5f * scale,
            zPos = 2,
            position = center
          ).getModel
        )
      case EmptyStorage => Seq()
    }

    new StaticCompositeModel(body +: hull +: contents)
  }
}
