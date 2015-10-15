package cwinter.codecraft.util.maths.matrices

private[codecraft] class DilationXYMatrix4x4(x: Float, y: Float) extends Matrix4x4(
  Array[Float](
    x, 0, 0, 0,
    0, y, 0, 0,
    0, 0, 1, 0,
    0, 0, 0, 1
  )
) {
  def this(xy: Float) = this(xy, xy)
}
