package cwinter.codinggame.physics

import cwinter.codinggame.maths.{Solve, Vector2}


trait Dynamics[Command] {
  def calculateMovement(pos: Vector2, command: Command, timestep: Float): Vector2
  def calculateCollisionTime(pos1: Vector2, pos2: Vector2, cmd1: Command, cmd2: Command, timestep: Float): Option[Float]
  def calculateWallCollisionTime(pos: Vector2, cmd: Command, timestep: Float): Option[Float]

  // def calculateBoundingBox(pos: Vector2, command: Command, timestep: Float): Vector2
}


case class Velocity(vec: Vector2) extends AnyVal

object Velocity {
  implicit def VelocityIsVector2(vector2: Vector2): Velocity = Velocity(vector2)
  implicit def Vector2IsVelocity(velocity: Velocity): Vector2 = velocity.vec
}

object ConstantVelocity extends Dynamics[Velocity] {
  def calculateMovement(pos: Vector2, command: Velocity, timestep: Float): Vector2 =
    pos + timestep * command.vec

  def calculateCollisionTime(pos1: Vector2, pos2: Vector2, cmd1: Velocity, cmd2: Velocity, timestep: Float): Option[Float] = {
    // need to calculate the intersection (if any), of two circles moving at constant speed
    // this is equivalent to a stationary circle with combined radius and a moving point

    // transform to frame of reference of object 1
    val position = pos2 - pos1
    val velocity = cmd2 - cmd1
    val radius = 50 + 50

    if (velocity.x == 0 && velocity.y == 0) return None

    val a = velocity dot velocity
    val b = 2 * velocity dot position
    val c = (position dot position) - radius * radius

    for (
      t <- Solve.quadratic(a, b, c)
      if t <= timestep
    ) yield t
  }

  def calculateWallCollisionTime(pos: Vector2, v: Velocity, timestep: Float): Option[Float] = {
    val ctX =
      if (v.x > 0) Some((750 - pos.x) / v.x)
      else if (v.x < 0) Some((-750 - pos.x) / v.x)
      else None

    val ctY =
      if (v.y > 0) Some((750 - pos.y) / v.y)
      else if (v.y < 0) Some((-750 - pos.y) / v.y)
      else None

    val x = (ctX, ctY) match {
      case (Some(t1), Some(t2)) => Some(math.min(t1, t2))
      case (Some(t1), None) => Some(t1)
      case (None, Some(t2)) => Some(t2)
      case (None, None) => None
    }

    x.filter(_ < timestep)
  }
}


