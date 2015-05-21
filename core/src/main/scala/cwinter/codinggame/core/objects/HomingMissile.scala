package cwinter.codinggame.core.objects

import cwinter.codinggame.core._
import cwinter.codinggame.core.objects.drone.Drone
import cwinter.codinggame.util.maths.Vector2
import cwinter.codinggame.worldstate.{HomingMissileDescriptor, Player, WorldObjectDescriptor}

class HomingMissile(val player: Player, initialPos: Vector2, time: Double, target: Drone) extends WorldObject {
  final val MaxLifetime = 50
  val dynamics: MissileDynamics = new MissileDynamics(500, target.dynamics, player.id, initialPos, time)
  val previousPositions = collection.mutable.Queue(initialPos)
  val positions = 7
  var lifetime = MaxLifetime
  var fading: Boolean = false

  def update(): Seq[SimulatorEvent] = {
    if (fading) {
      previousPositions.dequeue()
      if (previousPositions.isEmpty) {
        Seq(HomingMissileFaded(this))
      } else {
        Seq.empty[SimulatorEvent]
      }
    } else {
      dynamics.update()

      lifetime -= 1

      previousPositions.enqueue(position)
      if (previousPositions.length > positions) previousPositions.dequeue()
      while (previousPositions.length > lifetime + 1) previousPositions.dequeue()

      if (dynamics.removed) {
        fading = true
        Seq(MissileExplodes(this))
      } else if (lifetime == 0) {
        dynamics.remove()
        Seq(HomingMissileFaded(this))
      } else Seq.empty[SimulatorEvent]
    }
  }

  override def position: Vector2 = dynamics.pos
  override private[core] def descriptor: Seq[WorldObjectDescriptor] = Seq(
    HomingMissileDescriptor(
      id,
      previousPositions.map{case Vector2(x, y) => (x.toFloat, y.toFloat)},
      math.min(MaxLifetime - lifetime, positions), player)
  )

  override private[core] def hasDied = lifetime <= 0
}
