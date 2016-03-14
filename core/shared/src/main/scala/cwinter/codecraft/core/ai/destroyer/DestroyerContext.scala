package cwinter.codecraft.core.ai.destroyer

import cwinter.codecraft.core.ai.shared.{Mission, BattleCoordinator, BasicHarvestCoordinator, SharedContext}
import cwinter.codecraft.core.api.Drone
import cwinter.codecraft.util.maths.{Vector2, Rectangle}


private[codecraft] class DestroyerContext extends SharedContext[DestroyerCommand] {
  val harvestCoordinator = new BasicHarvestCoordinator
  override val battleCoordinator = new DestroyerBattleCoordinator(this)

  private var _mothership: Mothership = null
  def mothership: Mothership = _mothership


  def initialise(worldSize: Rectangle, mothership: Mothership): Unit = {
    initialise(worldSize)
    _mothership = mothership
    battleCoordinator.addMission(new ProtectMothership(mothership))
  }
}

private[codecraft] class DestroyerBattleCoordinator(context: DestroyerContext) extends BattleCoordinator[DestroyerCommand] {
  private var harvesters = Set.empty[Harvester]
  private var protectHarvesters = Option.empty[ProtectHarvesters]
  private var protectMothership = Option.empty[ProtectMothership]
  private var assaultMissions = Set.empty[AssaultCapitalShip]


  override def foundCapitalShip(drone: Drone): Unit = {
    if (!enemyCapitalShips.contains(drone)) {
      val newMission = new AssaultCapitalShip(drone)
      assaultMissions += newMission
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

  def requestGuards(amount: Int): Unit = {
    protectMothership match {
      case Some(protect) => protect.refresh(amount)
      case None =>
        val newMission = new ProtectMothership(context.mothership, amount)
        addMission(newMission)
        protectMothership = Some(newMission)
    }
  }

  def harvesterOnline(harvester: Harvester): Unit = {
    harvesters += harvester
  }

  def harvesterOffline(harvester: Harvester): Unit = {
    harvesters -= harvester
  }

  def needScouting =
    enemyCapitalShips.isEmpty || assaultMissions.forall(m => m.isDeactivated || m.hasExpired)
}


private[codecraft] sealed trait DestroyerCommand

private[codecraft] case class Attack(
  enemy: Drone,
  notFound: () => Unit,
  metResistance: Int => Unit
) extends DestroyerCommand
private[codecraft] case class MoveTo(
  position: Vector2
) extends DestroyerCommand

private[codecraft] case class MoveClose(position: Vector2, dist: Double) extends DestroyerCommand

private[codecraft] class AssaultCapitalShip(enemy: Drone) extends Mission[DestroyerCommand] {
  private var enemyStrength = enemy.spec.maxHitpoints * enemy.spec.missileBatteries
  var minRequired = calcMinRequired
  def maxRequired = math.min((minRequired * 1.5).toInt, minRequired + 1)
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
    } yield (e1.position - e2.position).lengthSquared <= 130 * 130 * nAssigned
  }.forall(close => close)

  private def midpoint: Vector2 =
    assigned.foldLeft(Vector2.Null)(_ + _.position) / nAssigned

  def notFound(): Unit = {
    deactivate()
  }

  def metResistance(strength: Int): Unit = {
    if (strength > enemyStrength) {
      enemyStrength = strength
      if (minRequired >= nAssigned * 2) disband()
      minRequired = calcMinRequired
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

  def calcMinRequired: Int = math.ceil(math.sqrt(enemyStrength /  (23 * 2.0))).toInt

  def hasExpired = enemy.isDead
}

private[codecraft] class ProtectHarvesters(
  mothership: Mothership,
  harvesters: => Set[Harvester]
) extends Mission[DestroyerCommand] {
  val minRequired: Int = 1
  val maxRequired: Int = 1
  val priority: Int = 5

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


private[codecraft] class ProtectMothership(
  mothership: Mothership,
  val required: Int = 0
) extends Mission[DestroyerCommand] {
  val minRequired: Int = 0
  var maxRequired: Int = if (required == 0) Int.MaxValue else required
  val hasExpired = mothership.isDead
  val priority: Int = if (required == 0) 2 else 15

  var timeout = 3000
  override def update(): Unit = {
    timeout -= 1
    if (timeout == 0) {
      maxRequired -= 1
      reduceAssignedToMax()
      timeout = 3000
    }
  }
  def refresh(amount: Int) = {
    if (amount >= maxRequired) {
      maxRequired = amount
      timeout = 3000
    }
  }

  def locationPreference: Option[Vector2] = Some(mothership.position)

  def missionInstructions: DestroyerCommand = {
    val enemies = mothership.armedEnemies
    if (enemies.nonEmpty) MoveTo(mothership.closestEnemy.lastKnownPosition)
    else MoveClose(mothership.position, 350)
  }

  override def candidateFilter(drone: Drone): Boolean =
    (drone.position - locationPreference.getOrElse(Vector2.Null)).lengthSquared <= 1500 * 1500
}

