package cwinter.codinggame.physics

import cwinter.codinggame.maths.{Vector2, Rng}
import robowars.worldstate.{Circle, WorldObject}


abstract class MovingObject[TCommand](
  val dynamics: Dynamics[TCommand],
  protected var pos: Vector2,
  protected[physics] var command: TCommand
) {
  val id = UID()
  private var currentTime = 0.0

  def update(time: Double): Unit = {
    setCommand()
    pos = dynamics.calculateMovement(pos, command, time - currentTime)
    currentTime = time
  }

  def setCommand(): Unit

  def state: WorldObject = Circle(id, pos.x.toFloat, pos.y.toFloat, 50)

  def collisionTime(other: MovingObject[TCommand], time: Double): Option[Double] = {
    dynamics.calculateCollisionTime(pos, other.pos, command, other.command, time - currentTime)
  }

  def wallCollisionTime(time: Double): Option[Double] = {
    dynamics.calculateWallCollisionTime(pos, command, time - currentTime)
  }

  def collision(other: MovingObject[TCommand]): Unit
  def wallCollision(): Unit
}


class ConstantVelocityObject(_pos: Vector2, _command: Velocity)
  extends MovingObject[Velocity](ConstantVelocity, _pos, _command) {

  def setCommand(): Unit = {

    //if (!collision && Rng.bernoulli(0.01)) {
    //  command = Rng.vector2(200)
    //}
  }

  override def collision(other: MovingObject[Velocity]): Unit = {
    command = -command
    other.command = -other.command
  }

  override def wallCollision(): Unit = {
    // find closest wall
    val dx = math.min(math.abs(pos.x + 750), math.abs(pos.x - 750))
    val dy = math.min(math.abs(pos.y + 750), math.abs(pos.y - 750))
    if (dx < dy) {
      command = command.vec.copy(x = -command.x)
    } else {
      command = command.vec.copy(y = -command.y)
    }
  }
}


object MovingObject {
  def apply() = {
    new ConstantVelocityObject(Rng.vector2(-500, 500, -500, 500), Rng.vector2(200))
  }

  def apply(position: Vector2) = {
    new ConstantVelocityObject(position, Rng.vector2(200))
  }
}


object UID {
  private[this] var _count = -1
  def apply(): Int = {
    _count += 1
    _count
  }
}
