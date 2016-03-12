package cwinter.codecraft.graphics.engine

import javax.media.opengl.GL2

import cwinter.codecraft.graphics.materials._


private[graphics] case class JVMGL2RenderStack(implicit gl: GL2) extends RenderStack {

  // materials
  val MaterialXYZRGB = new MaterialXYZRGB110
  val BloomShader = new MaterialXYZRGB110 // FIXME
  val GaussianGlow = new TranslucentAdditive110 // FIXME
  val GaussianGlowPIntensity = new GaussianGlowPIntensity110
  val TranslucentAdditive = new TranslucentAdditive110
  val TranslucentProportional = new TranslucentAdditive110 // FIXME
  val TranslucentAdditivePIntensity = new TranslucentAdditivePIntensity110
}

