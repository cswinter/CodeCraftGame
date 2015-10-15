package cwinter.codecraft.util.maths.matrices

private[codecraft] class TranslationXYMatrix4x4(x: Float, y: Float) extends Matrix4x4(
  Array[Float](
    1, 0, 0, x,
    0, 1, 0, y,
    0, 0, 1, 0,
    0, 0, 0, 1
  )
)
