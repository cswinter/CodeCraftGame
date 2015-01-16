package robowars.graphics.matrices

class OrthographicProjectionMatrix4x4(right: Float, left: Float, top: Float, bottom: Float,
                                      near: Float = 0.0f, far: Float = 1.0f) extends Matrix4x4({
  println(s"right=$right, left=$left, top=$top, bottom=$bottom")
  val rml = right - left
  val rpl = right + left
  val tmb = top - bottom
  val tpb = top + bottom
  val fmn = far - near
  val fpn = far + near
  Array[Float](
    2 / rml,       0,      0, -rpl / rml,
          0, 2 / tmb,      0, -tpb / tmb,
          0,       0, -2/fmn,  fpn / fmn,
          0,       0,      0,          1
  )}
)
