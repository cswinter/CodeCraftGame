package cwinter.codecraft.core.ai.replicator.combat

import cwinter.codecraft.core.api.Drone
import cwinter.codecraft.util.maths.Vector2


sealed trait MissionInstructions

case object Scout extends MissionInstructions
case class Attack(enemy: Drone, feedback: AssaultCapitalShip) extends MissionInstructions
case class Search(position: Vector2, radius: Double) extends MissionInstructions
case class AttackMove(position: Vector2) extends MissionInstructions
case class Circle(position: Vector2, radius: Double) extends MissionInstructions

