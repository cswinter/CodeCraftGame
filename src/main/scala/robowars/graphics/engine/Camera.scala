package robowars.graphics.engine

import robowars.graphics.matrices._


class Camera {
  private[this] var screenWidth: Int = 0
  private[this] var screenHeight: Int = 0
  private[this] var _projection: Matrix4x4 = IdentityMatrix4x4
  


  def projection = _projection

  private def recomputeProjection() = {
    _projection = new OrthographicProjectionMatrix4x4(screenWidth, screenHeight)
  }


  def screenDims = (screenWidth, screenHeight)

  def screenDims_=(dims: (Int, Int)) = {
    screenWidth = dims._1
    screenHeight = dims._2

    recomputeProjection()
  }

  def screenWidth_=(width: Int) = {
    screenWidth = width

    recomputeProjection()
  }

  def screenHeight_=(height: Int) = {
    screenHeight = height

    recomputeProjection()
  }

}
