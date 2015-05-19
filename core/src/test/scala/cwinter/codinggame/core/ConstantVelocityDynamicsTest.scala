package cwinter.codinggame.core

import cwinter.codinggame.core.objects.ConstantVelocityDynamics
import cwinter.codinggame.util.maths.{Rectangle, Vector2}
import org.scalatest.FlatSpec

class ConstantVelocityDynamicsTest extends FlatSpec {
  object ConstVelTestDynamics extends ConstantVelocityDynamics(1, 0, true, Vector2.NullVector, 0) {
    override def update(): Unit = ()
    override def handleWallCollision(areaBounds: Rectangle): Unit = ()
    override def handleObjectCollision(other: ConstantVelocityDynamics): Unit = ()
    def setVelocity(v: Vector2): Unit = velocity = v
  }

  "ConstantVelocityDynamics" should "move with linear velocity" in {
    ConstVelTestDynamics.setVelocity(Vector2(1, 1))
    ConstVelTestDynamics.updatePosition(10)
    assert(ConstVelTestDynamics.pos == Vector2(10, 10))
  }
}
