package robowars.graphics.matrices

class OrthographicProjectionMatrix4x4(width: Int, height: Int,
                                      near: Float = 0.0f, far: Float = 1.0f) extends Matrix4x4(
  Array[Float](
    2.0f / width,             0,                 0, -1,
               0, 2.0f / height,                 0, -1,
               0,             0, -2 / (far - near), (far + near) / (far - near),
               0,             0,                 0, 1
  )
)
