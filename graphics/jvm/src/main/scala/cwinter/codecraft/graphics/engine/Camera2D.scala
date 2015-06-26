package cwinter.codecraft.graphics.engine

import cwinter.codecraft.graphics.matrices.{IdentityMatrix4x4, OrthographicProjectionMatrix4x4, Matrix4x4}


class Camera2D {
  private[this] var _screenWidth: Int = 0
  private[this] var _screenHeight: Int = 0
  private[this] var _projection: Matrix4x4 = IdentityMatrix4x4
  private[this] var _x: Float = 0
  private[this] var _y: Float = 0
  private[this] var _zoom: Float = 0
  private[this] var _zoomFactor: Float = 1


  def projection = _projection

  private def recomputeProjection() = {
    _projection =
      new OrthographicProjectionMatrix4x4(
        x + screenWidth * 0.5f * zoomFactor,
        x - screenWidth * 0.5f * zoomFactor,
        y + screenHeight * 0.5f * zoomFactor,
        y - screenHeight * 0.5f * zoomFactor,
        -20,
        20
      )
  }


  def screenDims = (_screenWidth, _screenHeight)

  def screenDims_=(dims: (Int, Int)) = {
    _screenWidth = dims._1
    _screenHeight = dims._2

    recomputeProjection()
  }

  def screenWidth = _screenWidth

  def screenWidth_=(width: Int) = {
    _screenWidth = width

    recomputeProjection()
  }

  def screenHeight = _screenHeight

  def screenHeight_=(height: Int) = {
    _screenHeight = height

    recomputeProjection()
  }

  def position = (_x, _y)

  def position_=(xy: (Int, Int)) = {
    _x = xy._1
    _y = xy._2

    recomputeProjection()
  }

  def x = _x

  def x_=(x: Float) = {
    _x = x

    recomputeProjection()
  }

  def y = _y

  def y_=(y: Float) = {
    _y = y

    recomputeProjection()
  }

  def zoom = _zoom

  def zoom_=(zoom: Float) = {
    _zoom = zoom
    _zoomFactor = math.exp(zoom).toFloat

    recomputeProjection()
  }

  def zoomFactor: Float = _zoomFactor
}
