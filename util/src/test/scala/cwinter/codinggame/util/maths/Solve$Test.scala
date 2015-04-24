package cwinter.codinggame.util.maths

import org.scalatest.FlatSpec


class Solve$Test extends FlatSpec {
  "Solve.quadratic" should "solve x**2 - 1 = 0" in {
    assertResult(Some(1))(Solve.quadratic(1, 0, -1))
  }

  it should "find no (positive) solution for x**2 + 1 = 0" in {
    assertResult(None)(Solve.quadratic(1, 0, 1))
  }

  it should "solve x**2 - 2*x + 1 = 0" in {
    assertResult(Some(1))(Solve.quadratic(1, -2, 1))
  }

  it should "solve x**2 - 6*x + 9" in {
    assertResult(Some(3))(Solve.quadratic(1, -6, 9))
  }
}
