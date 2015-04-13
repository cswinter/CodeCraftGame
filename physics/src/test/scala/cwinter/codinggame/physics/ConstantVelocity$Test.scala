package cwinter.codinggame.physics

import cwinter.codinggame.maths.Vector2
import org.scalatest.FlatSpec


class ConstantVelocity$Test extends FlatSpec {
  "calculateCollisionTime" should "asdf" in {
    val p1 = Vector2(-100, 0)
    val v1 = Vector2(50, 0)
    val p2 = Vector2(100, 0)
    val v2 = Vector2(-50, 0)
    val obj1 = new ConstantVelocityObject(p1, v1)
    val obj2 = new ConstantVelocityObject(p2, v2)
    assertResult(Some(1))(obj1.computeCollisionTime(obj2, 2))
  }
}
