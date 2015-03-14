package robowars.graphics.model

import robowars.graphics.materials.Material

import scala.reflect.ClassTag


trait PrimitiveModelBuilder[TShape, TColor <: Vertex, TParams] <: ModelBuilder[TShape, TParams] {
  val material: Material[VertexXYZ, TColor, TParams]
  val shape: TShape
  def signature = shape

  protected def buildModel: Model[TParams] = {
    val vbo = material.createVBO(computeVertexData())
    new StaticModel(vbo, material)
  }

  protected def computeVertexData(): Seq[(VertexXYZ, TColor)]

}



case class Polygon[TColor <: Vertex : ClassTag, TParams](
  material: Material[VertexXYZ, TColor, TParams],
  n: Int,
  colorMidpoint: TColor,
  colorOutside: TColor,
  radius: Float = 1,
  position: VertexXY = NullVectorXY,
  zPos: Float = 0,
  orientation: Float = 0
) extends PrimitiveModelBuilder[Polygon[TColor, TParams], TColor, TParams] {
  val shape = this

  protected override def computeVertexData(): Seq[(VertexXYZ, TColor)] = {
    val vertices = Geometry.polygonVertices2(n, orientation, radius, position)
    val vertexPos = new Array[VertexXYZ](3 * n)
    for (i <- 0 until n) {
      val v1 = if (i == 0) vertices(n - 1) else vertices(i - 1)
      val v2 = vertices(i)

      vertexPos(3 * i + 0) = position.zPos(zPos)
      vertexPos(3 * i + 1) = v1.zPos(zPos)
      vertexPos(3 * i + 2) = v2.zPos(zPos)
    }


    val colors = new Array[TColor](vertexPos.length)
    for (i <- 0 until n) {
      colors(3 * i + 1) = colorOutside
      colors(3 * i + 2) = colorOutside
      colors(3 * i) = colorMidpoint
    }

    vertexPos zip colors
  }
}
