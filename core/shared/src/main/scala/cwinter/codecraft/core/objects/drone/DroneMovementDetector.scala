package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.util.maths.Vector2


private[core] trait DroneMovementDetector { self: DroneImpl =>
  private[this] var _oldPosition = Vector2.Null
  private[this] var _oldOrientation = 0.0
  private[this] var _hasMoved: Boolean = true


  def recomputeHasMoved(): Unit = {
    _hasMoved = _oldPosition != position || _oldOrientation != dynamics.orientation
    _oldPosition = position
    _oldOrientation = dynamics.orientation
  }

  def hasMoved = _hasMoved
}

