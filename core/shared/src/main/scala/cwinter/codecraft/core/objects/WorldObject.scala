package cwinter.codecraft.core.objects

import cwinter.codecraft.collisions.Positionable
import cwinter.codecraft.core.SimulatorEvent
import cwinter.codecraft.graphics.worldstate.WorldObjectDescriptor
import cwinter.codecraft.util.maths.Vector2


private[core] trait WorldObject {
  def position: Vector2

  def update(): Seq[SimulatorEvent]
  private[core] def descriptor: Seq[WorldObjectDescriptor]
  private[core] val id: Int
  private[core] def isDead: Boolean
}


class Counter {
  private[this] var count: Int = -1

  def getAndIncrement(): Int = {
    count += 1
    count
  }
}


private[core] object WorldObject {
  implicit object WorldObjectIsPositionable extends Positionable[WorldObject] {
    override def position(t: WorldObject): Vector2 = t.position
  }
}
