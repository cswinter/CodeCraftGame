import org.scalatest.FlatSpec
import robowars.graphics.matrices._


class Matrix4x4Test extends FlatSpec {
  val A = new Matrix4x4(Array[Float](
    10.4f, 15.1f, -123.4f, 0,
    -5, -43.4f, 1234101f, 43,
     1, 0, 34.545f, 431,
    114.0f, 452.4f, -123, -1
  ))

  val B = new Matrix4x4(Array[Float](
    1, 4, 12, 94,
    -12, 43, 12, 10,
    -4, -5, -6, 12,
    12, 34, 51, 10
  ))


  "IdentityMatrix4x4" should "have value 1 on the diagonal and 0 otherwise" in {
    for (row <- 0 to 3; col <- 0 to 3)
      if (row == col) assert(IdentityMatrix4x4(row, col) === 1)
      else assert(IdentityMatrix4x4(row, col) === 0)
  }

  it should "respect the identity I * I = I" in {
    assert(IdentityMatrix4x4 * IdentityMatrix4x4 === IdentityMatrix4x4)
  }

  it should "respect the identity I * A = A" in {
    assert(IdentityMatrix4x4 * A === A)
  }

  it should "respect the identity A * I = A" in {
    assert(A * IdentityMatrix4x4 === A)
  }


  "robowars.graphics.matrices.Matrix4x4" should "respect the identity (B * B) * (B * B) === B * (B * (B * B)))" in {
    assert((B * B) * (B * B) === B * (B * (B * B)))
  }
}

