package robowars.graphics.matrices

class OrthographicProjectionMatrix4x4(width: Int, height: Int) extends Matrix4x4(
  Array[Float](
    2.0f / width,             0,    0, 0,
               0, 2.0f / height,    0, 0,
               0,             0, 0.1f, 0,
               0,             0,    0, 1
  )
)
