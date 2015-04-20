package cwinter.graphics.matrices


class TranslationMatrix4x4(x: Float, y: Float, z: Float) extends Matrix4x4(
  Array[Float](
    1, 0, 0, x,
    0, 1, 0, y,
    0, 0, 1, z,
    0, 0, 1, 1
  )
)