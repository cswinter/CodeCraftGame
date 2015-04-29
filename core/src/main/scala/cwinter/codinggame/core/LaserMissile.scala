package cwinter.codinggame.core

import cwinter.codinggame.util.maths.Vector2
import cwinter.worldstate.{BluePlayer, LaserMissileDescriptor, WorldObjectDescriptor}

class LaserMissile(initialPos: Vector2, time: Double, target: Drone) extends WorldObject {
  val dynamics: MissileDynamics = new MissileDynamics(150, target.dynamics, initialPos, time)
  val previousPositions = collection.mutable.Queue(initialPos)
  val positions = 15
  var lifetime = 90

  def update(): Seq[SimulatorEvent] = {
    dynamics.update()

    lifetime -= 1

    previousPositions.enqueue(position)
    if (previousPositions.length > positions) previousPositions.dequeue()

    if (dynamics.removed) lifetime = 0

    if (lifetime == 0) {
      dynamics.remove()
      Seq(LaserMissileDestroyed(this))
    } else Seq.empty[SimulatorEvent]
  }

  override def position: Vector2 = dynamics.pos
  override private[core] def descriptor: WorldObjectDescriptor = {
    LaserMissileDescriptor(id, previousPositions.map{case Vector2(x, y) => (x.toFloat, y.toFloat)}, BluePlayer)
  }

  override private[core] def hasDied = lifetime <= 0
}

