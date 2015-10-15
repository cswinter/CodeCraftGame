package cwinter.codecraft.graphics.engine


import org.scalajs.dom.raw.{WebGLRenderingContext => GL}

import cwinter.codecraft.graphics.materials._
import cwinter.codecraft.util.maths._


private[graphics] class JSRenderStack(implicit gl: GL) extends RenderStack {
  override val TranslucentAdditive: Material[VertexXYZ, ColorRGBA, Unit] = new TranslucentAdditive
  override val MaterialXYZRGB: Material[VertexXYZ, ColorRGB, Unit] = new MaterialXYZRGB
  override val GaussianGlow: Material[VertexXYZ, ColorRGBA, Unit] = new GaussianGlow
  override val TranslucentProportional: Material[VertexXYZ, ColorRGBA, Unit] = new TranslucentAdditive // FIXME
  override val GaussianGlowPIntensity: Material[VertexXYZ, ColorRGBA, Intensity] = new GaussianGlowPIntensity
  override val BloomShader: Material[VertexXYZ, ColorRGB, Unit] =  new MaterialBrightenedXYZRGB // FIXME
  override val TranslucentAdditivePIntensity: Material[VertexXYZ, ColorRGBA, Intensity] = new TranslucentAdditivePIntensity

  override val modelviewTranspose = true
}

