package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model._
import cwinter.codecraft.graphics.primitives.{Polygon, PolygonRing}
import cwinter.codecraft.graphics.worldstate.{EmptyStorage, EnergyStorage, MineralStorage, StorageModuleContents}
import cwinter.codecraft.util.maths.{ColorRGB, VertexXY}
import cwinter.codecraft.util.modules.ModulePosition


private[graphics] case class DroneStorageModelBuilder(
  position: VertexXY,
  colors: DroneColors,
  moduleContents: StorageModuleContents
)(implicit rs: RenderStack) extends CompositeModelBuilder[DroneStorageModelBuilder, Unit] {

  val radius = 8
  val outlineWidth = 1


  def signature = this

  override protected def buildSubcomponents: (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, Unit]]) = {
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
      )

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
      )

    val contents: Seq[ModelBuilder[_, Unit]] = moduleContents match {
      case EnergyStorage(filledSlots) =>
        for {
          i <- 0 until 7
          if filledSlots.contains(i)
          globePos = ModulePosition.energyPosition(i) + position
        } yield EnergyGlobeModelFactory.build(globePos)
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
          )
        )
      case EmptyStorage => Seq()
    }

    (body +: hull +: contents, Seq.empty)
  }
}

