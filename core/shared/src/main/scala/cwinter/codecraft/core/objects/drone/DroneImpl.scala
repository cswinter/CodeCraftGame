package cwinter.codecraft.core.objects.drone

import java.nio.ByteBuffer

import boopickle.Default._
import cwinter.codecraft.core.api.GameConstants.HarvestingRange
import cwinter.codecraft.core.api._
import cwinter.codecraft.core.game._
import cwinter.codecraft.core.objects._
import cwinter.codecraft.core.replay._
import cwinter.codecraft.util.maths._

private[core] final class DroneImpl(
  val spec: DroneSpec,
  val controller: DroneControllerBase,
  val context: DroneContext,
  initialPos: Vector2,
  time: Double,
  protected val startingResources: Int = 0
) extends WorldObject
    with DroneVisionTracker
    with DroneGraphicsHandler
    with DroneMessageDisplay
    with DroneMovementDetector
    with DroneHull
    with DroneEventQueue
    with DroneModules {

  val id = context.idGenerator.getAndIncrement()
  val priority = context.rng.int()
  private[this] var _constructionProgress: Option[Int] = None
  private[this] var handles = Map.empty[Player, EnemyDrone]
  val dynamics: DroneDynamics = spec.constructDynamics(this, initialPos, time)

  def initialise(time: Double): Unit = {
    dynamics.setTime(time)
    controller.initialise(this)
    invalidateModelCache()
  }

  override def update(): Seq[SimulatorEvent] = {
    log(Position(position, dynamics.orientation))
    for ((_, wrapper) <- handles) wrapper.recordPosition()
    val events = updateModules()
    dynamics.recomputeVelocity()
    updateCollisionMarkers()
    recordPosition()
    displayMessages()

    events
  }

  def updatePositionDependentState(): Unit = {
    recomputeHasMoved()
    for (event <- dynamics.checkArrivalConditions())
      enqueueEvent(event)
  }

  def collidedWith(other: DroneImpl): Unit = {
    log(Collision(position, other.id))
    addCollisionMarker(other.position)
  }

  @inline def !(command: DroneCommand) = executeCommand(command)

  def executeCommand(command: DroneCommand): Unit = {
    if (isDead) {
      warn(s"Command $command given to dead drone.")
      return
    }
    var redundant = false
    command match {
      case mc: MovementCommand =>
        redundant = giveMovementCommand(mc)
      case cd: ConstructDrone => startDroneConstruction(cd)
      case DepositMinerals(target) => redundant = depositMinerals(target)
      case FireMissiles(target) => fireWeapons(target)
      case FireLongRangeMissiles(target) => fireLongRangeWeapons(target)
      case HarvestMineral(mineral) => harvestResource(mineral)
    }
    if (!redundant) {
      context.replayRecorder.record(id, command)
      context.commandRecorder.foreach(_.record(id, command))
    }
    log(Command(command, redundant))
  }

  def applyState(state: DroneMovementMsg)(implicit context: SimulationContext): Unit = dynamics match {
    case syncable: SyncableDroneDynamics => syncable.synchronize(state)
    case _ => throw new AssertionError("Trying to apply state to locally computed drone.")
  }

  def applyHarvest(mineral: MineralCrystalImpl): Option[SimulatorEvent] = {
    assert(context.isMultiplayer && !context.isLocallyComputed,
           "Trying to apply harvest to locally computed drone.")
    assert(storage.nonEmpty, "Trying to apply harvest to drone without storage module.")
    storage.flatMap(_.performHarvest(mineral))
  }

  //+------------------------------+
  //+ Command implementations      +
  //+------------------------------+
  private def giveMovementCommand(value: MovementCommand): Boolean = {
    dynamics.setMovementCommand(value)
  }

  private def startDroneConstruction(command: ConstructDrone): Unit = {
    manipulator match {
      case Some(m) => m.startDroneConstruction(command)
      case None => warn("Drone construction requires a constructor module.")
    }
  }

  def immobile = droneModules.exists(_.exists(_.cancelMovement))

  private def fireWeapons(target: DroneImpl): Unit = {
    if (target == this) {
      warn("Drone tried to shoot itself!")
    } else {
      weapons match {
        case Some(w) => w.fire(target)
        case None => warn("Firing missiles requires a missile battery module.")
      }
    }
  }

  private def fireLongRangeWeapons(target: DroneImpl): Unit = {
    if (target == this) {
      warn("Drone tried to shoot itself!")
    } else {
      longRangeMissiles match {
        case Some(w) => w.fire(target)
        case None => warn("Firing long range missiles requires a long range missile module.")
      }
    }
  }
  private def harvestResource(mineralCrystal: MineralCrystalImpl): Unit = {
    storage match {
      case Some(s) => s.harvestMineral(mineralCrystal)
      case None => warn("Harvesting resources requires a storage module.")
    }
  }

  private def depositMinerals(other: DroneImpl): Boolean = {
    var succeeded = false
    if (other == this) {
      warn("Drone is trying to deposit minerals into itself!")
    } else if (other.storage.isEmpty) {
      warn("Trying to deposit minerals into a drone without a storage module.")
    } else if (storedResources == 0) {
      warn("Drone has no minerals to deposit.")
    } else if ((other.position - position).lengthSquared > (radius + other.radius + 20) * (radius + other.radius + 20)) {
      warn("Too far away to deposit minerals.")
    } else {
      for (s <- storage) s.depositResources(other.storage)
      succeeded = true
    }
    !succeeded
  }

  private[core] def log(datum: DebugLogDatum): Unit =
    for (log <- context.debugLog) log.record(context.simulator.timestep, id, datum)

  private[core] def log(msg: => String): Unit = log(UnstructuredEvent(msg))

  //+------------------------------+
  //| Drone properties             +
  //+------------------------------+
  override def position: Vector2 = dynamics.pos
  def missileCooldown: Int = weapons.map(_.cooldown).getOrElse(1)
  def longRangeMissileChargeup: Int = longRangeMissiles.map(_.chargeup).getOrElse(1)
  def isConstructing: Boolean = manipulator.exists(_.isConstructing)
  def isHarvesting: Boolean = storage.exists(_.isHarvesting)
  def isMoving: Boolean = dynamics.isMoving
  def storageCapacity = spec.storageModules
  def sides = spec.sides
  def radius = spec.radius
  def player = context.player
  def maxSpeed = spec.maxSpeed
  def constructionProgress: Option[Int] = _constructionProgress
  def constructionProgress_=(value: Option[Int]): Unit = {
    _constructionProgress = value
    invalidateModelCache()
  }
  def isStunned: Boolean = dynamics.isStunned

  def availableStorage: Int = {
    for (s <- storage) yield s.predictedAvailableStorage
  }.getOrElse(0)

  def storedResources: Int = {
    for (s <- storage) yield s.predictedStoredResources
  }.getOrElse(0)

  def isInHarvestingRange(mineral: MineralCrystalImpl): Boolean = {
    val maxDist = math.sqrt(mineral.size).toFloat * 3 + HarvestingRange
    (mineral.position - position).lengthSquared <= maxDist * maxDist
  }

  def wrapperFor(player: Player): Drone = {
    if (player == this.player) controller
    else {
      if (!handles.contains(player)) handles += player -> new EnemyDrone(this, player)
      handles(player)
    }
  }

  private[core] def deathEvents: Seq[SimulatorEvent] =
    if (isDead) {
      var events = List[SimulatorEvent](DroneKilled(this))
      for {
        m <- manipulator
        d <- m.droneInConstruction
      } events ::= DroneConstructionCancelled(d)
      events
    } else Seq.empty

  override def toString: String = id.toString
}

import upickle.default._
private[core] sealed trait SerializableDroneCommand
@key("Construct") private[core] case class SerializableConstructDrone(spec: DroneSpec,
                                                                      position: Vector2,
                                                                      resourceCost: Int)
    extends SerializableDroneCommand
@key("FireMissiles") private[core] case class SerializableFireMissiles(targetID: Int)
    extends SerializableDroneCommand
@key("FireLongRangeMissiles") private[core] case class SerializableFireLongRangeMissiles(targetID: Int)
    extends SerializableDroneCommand
@key("Deposit") private[core] case class SerializableDepositMinerals(targetID: Int)
    extends SerializableDroneCommand
@key("Harvest") private[core] case class SerializableHarvestMineral(mineralID: Int)
    extends SerializableDroneCommand
@key("MoveToMineral") private[core] case class SerializableMoveToMineralCrystal(mineralCrystalID: Int)
    extends SerializableDroneCommand
@key("MoveToDrone") private[core] case class SerializableMoveToDrone(droneID: Int)
    extends SerializableDroneCommand

private[core] sealed trait DroneCommand {
  def toSerializable: SerializableDroneCommand
}
private[core] object DroneCommand {
  def apply(
    serialized: SerializableDroneCommand
  )(implicit context: SimulationContext): DroneCommand = {
    import scala.language.implicitConversions
    implicit def droneLookup(droneID: Int): DroneImpl = {
      if (context.droneRegistry.contains(droneID)) context.droneRegistry(droneID)
      else
        throw new Exception(
          f"Cannot find drone with id $droneID. Available IDs: ${context.droneRegistry.keys}")
    }
    implicit def mineralLookup(mineralID: Int): MineralCrystalImpl = {
      if (context.mineralRegistry.contains(mineralID)) context.mineralRegistry(mineralID)
      else
        throw new Exception(
          f"Cannot find mineral with id $mineralID. Available IDs: ${context.mineralRegistry.keys}")
    }
    serialized match {
      case SerializableConstructDrone(spec, position, resourceCost) =>
        ConstructDrone(spec, new DummyDroneController, position, resourceCost)
      case SerializableFireMissiles(target) => FireMissiles(target)
      case SerializableFireLongRangeMissiles(target) => FireLongRangeMissiles(target)
      case SerializableDepositMinerals(target) => DepositMinerals(target)
      case SerializableHarvestMineral(mineral) => HarvestMineral(mineral)
      case SerializableMoveToMineralCrystal(mineral) => MoveToMineralCrystal(mineral)
      case SerializableMoveToDrone(drone) => MoveToDrone(drone)
      case move: MoveInDirection => move
      case move: MoveToPosition => move
      case HoldPosition => HoldPosition
    }
  }
}

private[core] case class ConstructDrone(
  spec: DroneSpec,
  controller: DroneControllerBase,
  position: Vector2,
  resourceCost: Int
) extends DroneCommand {
  def toSerializable = SerializableConstructDrone(spec, position, resourceCost)
}
private[core] case class FireMissiles(target: DroneImpl) extends DroneCommand {
  def toSerializable = SerializableFireMissiles(target.id)
}
private[core] case class FireLongRangeMissiles(target: DroneImpl) extends DroneCommand {
  def toSerializable = SerializableFireLongRangeMissiles(target.id)
}
private[core] case class DepositMinerals(target: DroneImpl) extends DroneCommand {
  def toSerializable = SerializableDepositMinerals(target.id)
}
private[core] case class HarvestMineral(mineral: MineralCrystalImpl) extends DroneCommand {
  def toSerializable = SerializableHarvestMineral(mineral.id)
}

private[core] sealed trait MovementCommand extends DroneCommand
@key("MoveInDirec") private[core] case class MoveInDirection(direction: Float)
    extends MovementCommand
    with SerializableDroneCommand {
  def toSerializable = this
}
@key("MoveToPos") private[core] case class MoveToPosition(position: Vector2)
    extends MovementCommand
    with SerializableDroneCommand {
  def toSerializable = this
}
private[core] case class MoveToMineralCrystal(mineralCrystal: MineralCrystalImpl) extends MovementCommand {
  def toSerializable = SerializableMoveToMineralCrystal(mineralCrystal.id)
}
private[core] case class MoveToDrone(drone: DroneImpl) extends MovementCommand {
  def toSerializable = SerializableMoveToDrone(drone.id)
}
@key("Stop") private[core] case object HoldPosition extends MovementCommand with SerializableDroneCommand {
  def toSerializable = this
}

import upickle.default._

private[core] case class SerializableSpawn(spec: DroneSpec,
                                           position: Vector2,
                                           playerID: Int,
                                           resources: Int,
                                           name: Option[String]) {
  def deserialize: Spawn =
    Spawn(spec, position, Player.fromID(playerID), resources, name)
}
private[core] object SerializableSpawn {
  def apply(spawn: Spawn): SerializableSpawn =
    SerializableSpawn(spawn.droneSpec, spawn.position, spawn.player.id, spawn.resources, spawn.name)
}

private[core] sealed trait MultiplayerMessage

private[core] sealed trait ServerMessage extends MultiplayerMessage
private[core] sealed trait ClientMessage extends MultiplayerMessage

private[core] case object Register extends ClientMessage

private[core] case class RTT(sent: Long, msg: String) extends ServerMessage with ClientMessage

private[core] case class CommandsMessage(
  commands: Seq[(Int, SerializableDroneCommand)]
) extends ServerMessage
    with ClientMessage

private[core] case class WorldStateMessage(
  missileHits: Seq[MissileHit],
  stateChanges: Seq[DroneMovementMsg],
  mineralHarvests: Seq[MineralHarvest],
  droneSpawns: Seq[DroneSpawned]
) extends ServerMessage
private[core] case class MineralHarvest(droneID: Int, mineralID: Int)

private[core] sealed trait InitialServerResponse extends ServerMessage

private[core] case object ServerBusy extends InitialServerResponse

private[core] case class InitialSync(
  worldSize: Rectangle,
  minerals: Seq[MineralSpawn],
  initialDrones: Seq[SerializableSpawn],
  localPlayerIDs: Set[Int],
  remotePlayerIDs: Set[Int],
  tickPeriod: Int,
  rngSeed: Int,
  winConditions: Seq[WinCondition]
) extends InitialServerResponse {
  def gameConfig(droneControllers: Seq[DroneControllerBase]): GameConfig = {
    val controllersIter = droneControllers.iterator
    GameConfig(
      worldSize,
      minerals,
      initialDrones.map { spawn =>
        if (localPlayerIDs.contains(spawn.playerID)) (spawn.deserialize, controllersIter.next())
        else (spawn.deserialize, new DummyDroneController)
      },
      winConditions,
      tickPeriod,
      rngSeed
    )
  }

  def localPlayers: Set[Player] = localPlayerIDs.map(Player.fromID)
  def remotePlayers: Set[Player] = remotePlayerIDs.map(Player.fromID)
}

private[core] case class GameClosed(reason: GameClosed.Reason) extends ServerMessage

private[core] object GameClosed {
  sealed trait Reason
  case class PlayerWon(winnerID: Int) extends Reason
  case class PlayerDisconnected(playerID: Int) extends Reason
  case object PlayerTimedOut extends Reason
  case class ProtocolViolation(playerID: Int) extends Reason
  case object Timeout extends Reason
  case object ServerStopped extends Reason
  case class Crash(exceptionMsg: String) extends Reason
}

private[core] object ServerMessage {
  def parse(json: String): ServerMessage = read[ServerMessage](json)
  def parseBytes(bytes: ByteBuffer): ServerMessage = Unpickle[ServerMessage].fromBytes(bytes)
  def serializeBinary(msg: ServerMessage): ByteBuffer = Pickle.intoBytes(msg)
}

private[core] object ClientMessage {
  def parse(json: String): ClientMessage = read[ClientMessage](json)
  def parseBytes(bytes: ByteBuffer): ClientMessage = Unpickle[ClientMessage].fromBytes(bytes)
  def serializeBinary(msg: ClientMessage): ByteBuffer = Pickle.intoBytes(msg)
}

private[core] object DroneOrdering extends Ordering[DroneImpl] {
  override def compare(d1: DroneImpl, d2: DroneImpl): Int =
    if (d1.priority == d2.priority) d1.id - d2.id
    else d1.id - d2.id
}
