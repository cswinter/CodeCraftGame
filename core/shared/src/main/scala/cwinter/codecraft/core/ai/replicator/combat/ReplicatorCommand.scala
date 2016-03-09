package cwinter.codecraft.core.ai.replicator.combat

import cwinter.codecraft.core.api.Drone
import cwinter.codecraft.util.maths.Vector2


private[codecraft] sealed trait ReplicatorCommand

private[codecraft] case object Scout extends ReplicatorCommand
private[codecraft] case class Attack(maxDist: Double, enemy: Drone, notFound: () => Unit) extends ReplicatorCommand
private[codecraft] case class Search(position: Vector2, radius: Double) extends ReplicatorCommand
private[codecraft] case class AttackMove(position: Vector2) extends ReplicatorCommand
private[codecraft] case class Circle(position: Vector2, radius: Double) extends ReplicatorCommand
private[codecraft] case class Observe(enemy: Drone, notFound: () => Unit) extends ReplicatorCommand

