package cwinter.codecraft.core.ai.replicator.combat

import cwinter.codecraft.core.api.Drone
import cwinter.codecraft.util.maths.Vector2


sealed trait ReplicatorCommand

case object Scout extends ReplicatorCommand
case class Attack(maxDist: Double, enemy: Drone, notFound: () => Unit) extends ReplicatorCommand
case class Search(position: Vector2, radius: Double) extends ReplicatorCommand
case class AttackMove(position: Vector2) extends ReplicatorCommand
case class Circle(position: Vector2, radius: Double) extends ReplicatorCommand
case class Observe(enemy: Drone, notFound: () => Unit) extends ReplicatorCommand

