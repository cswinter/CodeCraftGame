package cwinter.codecraft.graphics.engine

import cwinter.codecraft.graphics.materials.{Intensity, Material}
import cwinter.codecraft.util.PrecomputedHashcode
import cwinter.codecraft.util.maths._


private[graphics] trait RenderStack extends PrecomputedHashcode {
  self: Product =>
  val MaterialXYZRGB: Material[VertexXYZ, ColorRGB, Unit]
  val BloomShader: Material[VertexXYZ, ColorRGB, Unit]
  val GaussianGlow: Material[VertexXYZ, ColorRGBA, Unit]
  val GaussianGlowPIntensity: Material[VertexXYZ, ColorRGBA, Intensity]
  val TranslucentAdditive: Material[VertexXYZ, ColorRGBA, Unit]
  val TranslucentProportional: Material[VertexXYZ, ColorRGBA, Unit]
  val TranslucentAdditivePIntensity: Material[VertexXYZ, ColorRGBA, Intensity]

  lazy val materials = List(
    MaterialXYZRGB, BloomShader, GaussianGlow, GaussianGlowPIntensity,
    TranslucentAdditive, TranslucentProportional, TranslucentAdditivePIntensity)

  val modelviewTranspose: Boolean = false


  def postDraw(camera2D: Camera2D): Unit = ()
}

