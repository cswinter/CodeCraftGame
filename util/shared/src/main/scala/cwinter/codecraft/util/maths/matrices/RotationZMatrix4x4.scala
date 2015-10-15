package cwinter.codecraft.util.maths.matrices

private[codecraft] class RotationZMatrix4x4(angle: Double) extends Matrix4x4({
  val cos = math.cos(angle).toFloat
  val sin = math.sin(angle).toFloat
  Array[Float](
    cos, -sin, 0, 0,
    sin,  cos, 0, 0,
      0,    0, 1, 0,
      0,    0, 0, 1
  )
})
