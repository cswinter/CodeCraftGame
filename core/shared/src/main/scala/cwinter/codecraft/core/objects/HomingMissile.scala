package cwinter.codecraft.core.objects

import cwinter.codecraft.core.api.GameConstants.{
  MissileLifetime,
  MissileSpeed,
  LongRangeMissileLifetime,
  LongRangeMissileAcceleration
}
import cwinter.codecraft.core.api.Player
import cwinter.codecraft.core.game.{HomingMissileFaded, MissileExplodes, SimulatorEvent}
import cwinter.codecraft.core.graphics.{BasicHomingMissileModel, HomingMissileModel}
import cwinter.codecraft.core.objects.drone.DroneImpl
import cwinter.codecraft.graphics.engine.{ModelDescriptor, NullPositionDescriptor}
import cwinter.codecraft.util.maths.Vector2

private[core] class HomingMissile(
  val player: Player,
  initialPos: Vector2,
  val id: Int,
  time: Double,
  target: DroneImpl,
  longRange: Boolean = false
) extends WorldObject {
  val dynamics: MissileDynamics =
    new MissileDynamics(if (longRange) 0.0 else MissileSpeed,
                        target.dynamics,
                        player.id,
                        this,
                        initialPos,
                        time,
                        acceleration = if (longRange) LongRangeMissileAcceleration else 0.0)
  val previousPositions = collection.mutable.Queue(initialPos)
  val positions = 7
  var lifetime = MissileLifetime
  var fading: Boolean = false

  def update(): Seq[SimulatorEvent] = {
    if (fading) fade()
    else updatePosition()
  }

  private def fade() = {
    previousPositions.dequeue()
    if (previousPositions.isEmpty) Seq(HomingMissileFaded(this))
    else Seq.empty[SimulatorEvent]
  }

  private def updatePosition() = {
    dynamics.recomputeVelocity()
    lifetime -= 1

    if (isAnimated) recordPosition()

    checkForRemoval
  }

  private def recordPosition() = {
    previousPositions.enqueue(position)
    if (previousPositions.length > positions) previousPositions.dequeue()
    while (previousPositions.length > lifetime + 1) previousPositions.dequeue()
  }

  private def checkForRemoval = {
    if (dynamics.removed) {
      fading = true
      Seq(MissileExplodes(this))
    } else if (lifetime == 0) {
      dynamics.remove()
      Seq(HomingMissileFaded(this))
    } else Seq.empty[SimulatorEvent]
  }

  def isAnimated = target.context.settings.allowMissileAnimation

  override def position: Vector2 = dynamics.pos
  override private[core] def descriptor: Seq[ModelDescriptor[_]] = Seq(
    ModelDescriptor(
      NullPositionDescriptor,
      modelDescriptor
    )
  )

  private def modelDescriptor =
    if (isAnimated) fancyModelDescriptor
    else basicModelDescriptor

  private def fancyModelDescriptor =
    HomingMissileModel(
      previousPositions.map { case Vector2(x, y) => (x.toFloat, y.toFloat) },
      math.min(MissileLifetime - lifetime, positions),
      player.color,
      if (longRange) 5.0f else 3.0f
    )

  private def basicModelDescriptor =
    BasicHomingMissileModel(position.x, position.y, player.color, if (longRange) 8.0f else 5.0f)

  override private[core] def isDead = lifetime <= 0
}
