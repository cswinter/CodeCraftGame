package cwinter.codecraft.graphics.engine

import cwinter.codecraft.util.maths.matrices.Matrix4x4


private[codecraft] case class PositionDescriptor(
  x: Float,
  y: Float,
  orientation: Float = 0
) {
  assert(!x.toDouble.isNaN)
  assert(!y.toDouble.isNaN)
  assert(!orientation.toDouble.isNaN)

  private[this] var _cachedModelviewMatrix: Option[Matrix4x4] = None
  private[graphics] def cachedModelviewMatrix_=(value: Matrix4x4): Unit =
    _cachedModelviewMatrix = Some(value)
  private[graphics] def cachedModelviewMatrix = _cachedModelviewMatrix
}

private[codecraft] object NullPositionDescriptor extends PositionDescriptor(0, 0, 0)


