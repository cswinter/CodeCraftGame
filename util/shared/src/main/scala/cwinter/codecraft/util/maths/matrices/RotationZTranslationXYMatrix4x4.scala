package cwinter.codecraft.util.maths.matrices

class RotationZTranslationXYMatrix4x4(angle: Double, x: Float, y: Float) extends Matrix4x4({
  val cos = math.cos(angle).toFloat
  val sin = math.sin(angle).toFloat
  Array[Float](
    cos, -sin, 0, x,
    sin,  cos, 0, y,
     0 ,   0 , 1, 0,
     0 ,   0 , 0, 1
  )
})

