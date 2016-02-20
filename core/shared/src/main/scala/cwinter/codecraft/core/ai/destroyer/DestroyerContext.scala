package cwinter.codecraft.core.ai.destroyer

import cwinter.codecraft.core.ai.shared.{Mission, BattleCoordinator, BasicHarvestCoordinator, SharedContext}
import cwinter.codecraft.core.api.Drone
import cwinter.codecraft.util.maths.{Vector2, Rectangle}


class DestroyerContext extends SharedContext[DestroyerCommand] {
  val harvestCoordinator = new BasicHarvestCoordinator
  override val battleCoordinator = new DestroyerBattleCoordinator(this)

  private var _mothership: Mothership = null
  def mothership: Mothership = _mothership


  def initialise(worldSize: Rectangle, mothership: Mothership): Unit = {
    initialise(worldSize)
    _mothership = mothership
  }
}

class DestroyerBattleCoordinator(context: DestroyerContext) extends BattleCoordinator[DestroyerCommand] {
  private var harvesters = Set.empty[Harvester]
  private var protectHarvesters = Option.empty[ProtectHarvesters]


  override def foundCapitalShip(drone: Drone): Unit = {
    if (!enemyCapitalShips.contains(drone)) {
      val newMission = new AssaultCapitalShip(drone)
      addMission(newMission)
    }
    super.foundCapitalShip(drone)
  }

  def requestBigDaddy(): Unit = {
    protectHarvesters match {
      case Some(protect) if !protect.hasExpired =>
        protect.refresh()
      case _ =>
        val newMission = new ProtectHarvesters(context.mothership, harvesters)
        addMission(newMission)
        protectHarvesters = Some(newMission)
    }
  }

  def harvesterOnline(harvester: Harvester): Unit = {
    harvesters += harvester
  }

  def harvesterOffline(harvester: Harvester): Unit = {
    harvesters -= harvester
  }

  def needScouting = enemyCapitalShips.isEmpty
}


sealed trait DestroyerCommand

case class Attack(
  enemy: Drone,
  notFound: () => Unit,
  metResistance: Int => Unit
) extends DestroyerCommand
case class MoveTo(
  position: Vector2
) extends DestroyerCommand

class AssaultCapitalShip(enemy: Drone) extends Mission[DestroyerCommand] {
  private var enemyStrength = enemy.spec.maxHitpoints * enemy.spec.missileBatteries
  def minRequired =
    math.max(1, math.ceil(math.sqrt(enemyStrength /  (22 * 2.0))).toInt)
  def maxRequired = (minRequired * 1.5).toInt
  val basePriority = 10 - enemy.spec.missileBatteries + enemy.spec.constructors
  private var assemble = minRequired > 1

  def priority = basePriority + priorityBoost

  def priorityBoost =
    if (minRequired > 1 && nAssigned == minRequired) 5
    else 0

  def locationPreference = Some(enemy.lastKnownPosition)

  def missionInstructions: DestroyerCommand =
    if (nAssigned <= 1 || !assemble) Attack(enemy, notFound, metResistance)
    else MoveTo(midpoint)

  private def allClose: Boolean = nAssigned == 1 || {
    for {
      pair <- assigned.sliding(2)
      e1 = pair.head
      e2 = pair.tail.head
    } yield (e1.position - e2.position).lengthSquared <= 150 * 150
  }.forall(close => close)

  private def midpoint: Vector2 =
    assigned.foldLeft(Vector2.Null)(_ + _.position) / nAssigned

  def notFound(): Unit = {
    deactivate()
  }

  def metResistance(strength: Int): Unit = {
    if (strength > enemyStrength) {
      println(s"Adjusting enemy strength ($enemyStrength -> $strength)")
      println(s"New status: $nAssigned/$minRequired ($maxRequired)")
      enemyStrength = strength
      if (minRequired >= nAssigned * 2) {
        println("Disbanding...")
        disband()
      }
    }
  }

  override def update(): Unit = {
    if (isDeactivated && enemy.isVisible) reactivate()
    if (assemble && allClose) assemble = false
    if (minRequired > 1 && nAssigned == 0) assemble = true
    if (minRequired > nAssigned)
      for (drone <- assigned)
        if ((drone.position - enemy.lastKnownPosition).lengthSquared > 1000 * 1000)
          drone.abortMission()
  }

  def hasExpired = enemy.isDead
}

class ProtectHarvesters(
  mothership: Mothership,
  harvesters: => Set[Harvester]
) extends Mission[DestroyerCommand] {
  val minRequired: Int = 1
  val maxRequired: Int = 1
  val priority: Int = 8

  var timeout = 1500
  def hasExpired: Boolean = timeout < 0 || harvesters.isEmpty

  override def update(): Unit = timeout -= 1

  def refresh() = timeout = 1500

  def locationPreference: Option[Vector2] = harvesters.headOption.map(_.position)

  private def furthestHarvester: Option[Harvester] = {
    if (harvesters.isEmpty) None
    else {
      val mothershipPos =
        if (mothership.isDead) Vector2.Null
        else mothership.position
      Some(harvesters.maxBy(h => (h.position - mothershipPos).lengthSquared))
    }
  }

  def missionInstructions: DestroyerCommand = {
    val enemies = harvesters.flatMap(_.enemies).filter(_.spec.missileBatteries > 0)
    if (enemies.nonEmpty) {
      val targetPos = furthestHarvester.map(_.position).getOrElse(Vector2.Null)
      val closestEnemy = enemies.minBy(e => (targetPos - e.position).lengthSquared)
      MoveTo(closestEnemy.lastKnownPosition)
    } else {
      furthestHarvester match {
        case Some(harvester) =>
          MoveTo(harvester.position + Vector2(harvester.orientation) * 150)
        case None =>
          MoveTo(Vector2.Null)
      }
    }
  }

  override def candidateFilter(drone: Drone): Boolean =
    (drone.position - locationPreference.getOrElse(Vector2.Null)).lengthSquared <= 1500 * 1500
}

