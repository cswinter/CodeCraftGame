package cwinter.codecraft.graphics.engine

import cwinter.codecraft.graphics.materials.{Intensity, Material}
import cwinter.codecraft.util.maths._


trait RenderStack {
  val SimpleMaterial: Material[VertexXY, EmptyVertex.type, Unit]
  val MaterialXYZRGB: Material[VertexXYZ, ColorRGB, Unit]
  val BloomShader: Material[VertexXYZ, ColorRGB, Unit]
  val GaussianGlow: Material[VertexXYZ, ColorRGBA, Unit]
  val GaussianGlowPIntensity: Material[VertexXYZ, ColorRGBA, Intensity]
  val TranslucentAdditive: Material[VertexXYZ, ColorRGBA, Unit]
  val TranslucentProportional: Material[VertexXYZ, ColorRGBA, Unit]
  val TranslucentAdditivePIntensity: Material[VertexXYZ, ColorRGBA, Intensity]

  lazy val materials = List(
    SimpleMaterial, MaterialXYZRGB, BloomShader, GaussianGlow, GaussianGlowPIntensity,
    TranslucentAdditive, TranslucentProportional, TranslucentAdditivePIntensity)
}

