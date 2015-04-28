package cwinter.codinggame.core

import cwinter.codinggame.util.maths.Vector2
import cwinter.worldstate.WorldObjectDescriptor

class LaserMissile extends WorldObject {
  val dynamics: DroneDynamics = null


  override def position: Vector2 = dynamics.pos
  override private[core] def descriptor: WorldObjectDescriptor = null
}
