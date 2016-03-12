package cwinter.codecraft.graphics.engine


import org.scalajs.dom.raw.{WebGLRenderingContext => GL}

import cwinter.codecraft.graphics.materials._
import cwinter.codecraft.util.maths._


private[graphics] case class JSRenderStack(implicit gl: GL) extends RenderStack {
  override val TranslucentAdditive = new TranslucentAdditive
  override val MaterialXYZRGB = new MaterialXYZRGB
  override val GaussianGlow = new GaussianGlow
  override val TranslucentProportional = new TranslucentAdditive // FIXME
  override val GaussianGlowPIntensity = new GaussianGlowPIntensity
  override val BloomShader =  new MaterialBrightenedXYZRGB // FIXME
  override val TranslucentAdditivePIntensity = new TranslucentAdditivePIntensity

  override val modelviewTranspose = true
}

