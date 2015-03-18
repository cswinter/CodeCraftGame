package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.materials.Material
import robowars.graphics.matrices.Matrix2x2
import robowars.graphics.model._
import RobotColors._


case class RobotLaserWeaponModelBuilder(slot: Int, n: Int, inradius: Float)(implicit rs: RenderStack)
  extends ModelBuilder[RobotLaserWeaponModelBuilder, Unit] {
  override def signature: RobotLaserWeaponModelBuilder = this

  override protected def buildModel: Model[Unit] = {
    new StaticCompositeModel(Seq(
      /=\(slot, n, inradius).getModel
    ))
  }
}


case class /=\(slot: Int, n: Int, inradius: Float)(implicit rs: RenderStack)
  extends PrimitiveModelBuilder[/=\, ColorRGB, Unit] {
  val shape = this

  override val material: Material[VertexXYZ, ColorRGB, Unit] = rs.MaterialXYRGB

  override protected def computeVertexData(): Seq[(VertexXYZ, ColorRGB)] = {
    val vertexPos = new collection.mutable.ArrayBuffer[VertexXY]()


    // center quad
    vertexPos += VertexXY(0f, -1f)
    vertexPos += VertexXY(2f, -1f)
    vertexPos += VertexXY(2f, 1f)

    vertexPos += VertexXY(0f, -1f)
    vertexPos += VertexXY(2f, 1f)
    vertexPos += VertexXY(0f, 1f)

    // top triangle
    vertexPos += VertexXY(0, -3)
    vertexPos += VertexXY(2, -1)
    vertexPos += VertexXY(0, -1)

    // bottom triangle
    vertexPos += VertexXY(0, 1)
    vertexPos += VertexXY(2, 1)
    vertexPos += VertexXY(0, 3)


    // center quad
    val d = 0.6f
    vertexPos += VertexXY(0f + d, -1f + d)
    vertexPos += VertexXY(2f - d, -1f + d)
    vertexPos += VertexXY(2f - d,  1f - d)

    vertexPos += VertexXY(0f + d, -1f + d)
    vertexPos += VertexXY(2f - d,  1f - d)
    vertexPos += VertexXY(0f + d,  1f - d)

    // top triangle
    vertexPos += VertexXY(0 + d, -3 + d + d)
    vertexPos += VertexXY(2 - d, -1 - d + d)
    vertexPos += VertexXY(0 + d, -1 - d + d)

    // bottom triangle
    vertexPos += VertexXY(0 + d, 1 + d - d)
    vertexPos += VertexXY(2 - d, 1 + d - d)
    vertexPos += VertexXY(0 + d, 3 - d - d)


    val scale = 5f
    val angle = 2 * math.Pi.toFloat * slot / n + math.Pi.toFloat
    val rotation = Matrix2x2.rotation(angle)
    val posData =
      for ((pos, i) <- vertexPos.toArray.zipWithIndex) yield {
        rotation * (scale * pos + VertexXY(inradius, 0))
      }.zPos(if (i < 4 * 3) -1 else -0.5f)

    val colData =
      Seq.fill(4 * 3)(ColorBackplane) ++
      Seq.fill(4 * 3)(ColorRGB(1, 1, 1))

    posData zip colData
  }
}

