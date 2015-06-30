package cwinter.codecraft.util.maths.matrices

class DilationMatrix4x4(x: Float, y: Float, z: Float) extends Matrix4x4(
  Array[Float](
    x, 0, 0, 0,
    0, y, 0, 0,
    0, 0, z, 0,
    0, 0, 0, 1
  )
)
