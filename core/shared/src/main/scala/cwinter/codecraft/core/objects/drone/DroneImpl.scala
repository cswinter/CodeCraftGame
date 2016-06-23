package cwinter.codecraft.core.objects.drone

import java.nio.ByteBuffer

import cwinter.codecraft.collisions.{ActiveVisionTracking, VisionTracking}
import cwinter.codecraft.core._
import cwinter.codecraft.core.api.GameConstants.HarvestingRange
import cwinter.codecraft.core.api._
import cwinter.codecraft.core.errors.Errors
import cwinter.codecraft.core.graphics.{DroneModelParameters, DroneModuleDescriptor, DroneModel, CollisionMarkerModel}
import cwinter.codecraft.core.objects._
import cwinter.codecraft.core.replay._
import cwinter.codecraft.graphics.engine.{TextModel, Debug, PositionDescriptor, ModelDescriptor}
import cwinter.codecraft.util.maths._
import boopickle.Default._


private[core] class DroneImpl(
  val spec: DroneSpec,
  val controller: DroneControllerBase,
  val context: DroneContext,
  initialPos: Vector2,
  time: Double,
  startingResources: Int = 0
) extends WorldObject with ActiveVisionTracking {
  require(context.worldConfig != null)

  def maxSpeed = spec.maxSpeed

  val id = context.idGenerator.getAndIncrement()
  val priority = context.rng.int()

  private[this] var _mineralsInSight = Set.empty[MineralCrystal]
  private[this] var _dronesInSight = Set.empty[Drone]
  private[this] var _enemiesInSight = Set.empty[Drone]
  private[this] var _alliesInSight = Set.empty[Drone]

  private[this] val eventQueue = collection.mutable.Queue[DroneEvent](Spawned)

  // TODO: move all this state into submodules?
  private[this] var hullState = List.fill[Byte](spec.sides - 1)(2)
  private[this] var _hasDied: Boolean = false
  private[this] var _oldPosition = Vector2.Null
  private[this] var _oldOrientation = 0.0
  private[this] var _hasMoved: Boolean = true
  private[this] var _constructionProgress: Option[Int] = None
  def constructionProgress: Option[Int] = _constructionProgress
  def constructionProgress_=(value: Option[Int]): Unit = {
    _constructionProgress = value
    mustUpdateModel()
  }

  private[this] var handles = Map.empty[Player, EnemyDrone]

  private[this] var _missileHits = List.empty[MissileHit]
  def popMissileHits(): Seq[MissileHit] = {
    val result = _missileHits
    _missileHits = List.empty[MissileHit]
    result
  }

  private[this] var _collisionMarkers = List.empty[(CollisionMarkerModel, Float)]
  private[this] var debugText = Option.empty[String]

  private[this] var cachedDescriptor: Option[DroneModel] = None

  final val CollisionMarkerLifetime = 50f
  final val MessageCooldown = 30
  private[this] var messageCooldown = 0
  private final val NJetPositions = 6
  private[this] val oldPositions = collection.mutable.Queue.empty[(Float, Float, Float)]


  // TODO: remove this once all logic is moved into modules
  private[this] var simulatorEvents = List.empty[SimulatorEvent]

  val dynamics: DroneDynamics = spec.constructDynamics(this, initialPos, time)
  private[this] val weapons = spec.constructMissilesBatteries(this)
  private[core] val storage = spec.constructStorage(this, startingResources)
  private[this] val manipulator = spec.constructManipulatorModules(this)
  private[this] val shieldGenerators = spec.constructShieldGenerators(this)
  private[this] val engines = spec.constructEngineModules(this)
  val droneModules = Seq(weapons, storage, manipulator, shieldGenerators, engines)


  def initialise(time: Double): Unit = {
    dynamics.setTime(time)
    controller.initialise(this)
  }

  var t = 0
  def processEvents(): Unit = {
    debugText = None
    controller.willProcessEvents()

    t += 1
    if (isDead) {
      controller.onDeath()
    } else {
      // process events
      eventQueue foreach {
        case Destroyed => // this should never be executed
        case MineralEntersSightRadius(mineral) =>
          controller.onMineralEntersVision(mineral.getHandle(player))
        case ArrivedAtPosition => controller.onArrivesAtPosition()
        case ArrivedAtDrone(drone) =>
          val droneHandle = if (drone.player == player) drone.controller else new EnemyDrone(drone, player)
          controller.onArrivesAtDrone(droneHandle)
        case ArrivedAtMineral(mineral) =>
          controller.onArrivesAtMineral(new MineralCrystal(mineral, player))
        case DroneEntersSightRadius(drone) => controller.onDroneEntersVision(
          if (drone.player == player) drone.controller
          else drone.wrapperFor(player)
        )
        case Spawned => /* this is handled by simulator to ensure onSpawn is called before any other events */
        case event => throw new Exception(s"Unhandled event! $event")
      }
      eventQueue.clear()
      controller.onTick()
    }
  }

  override def update(): Seq[SimulatorEvent] = {
    _hasMoved = _oldPosition != position || _oldOrientation != dynamics.orientation
    _oldPosition = position
    _oldOrientation = dynamics.orientation

    for ((_, wrapper) <- handles) wrapper.recordPosition()

    _collisionMarkers = for (
      (model, lifetime) <- _collisionMarkers
      if lifetime > 0
    ) yield (model, lifetime - 1)


    for (Some(m) <- droneModules) {
      val (events, resourceDepletions, resourceSpawns) = m.update(storedResources)
      simulatorEvents :::= events.toList
      for {
        s <- storage
        rd <- resourceDepletions
        pos = s.withdrawEnergyGlobe()
        if context.settings.allowEnergyGlobeAnimation
      } simulatorEvents ::= SpawnEnergyGlobeAnimation(new EnergyGlobeObject(this, pos, 30, rd))
      for (s <- storage; rs <- resourceSpawns) s.depositEnergyGlobe(rs)
    }

    dynamics.recomputeVelocity()

    messageCooldown -= 1

    oldPositions.enqueue((position.x, position.y, dynamics.orientation))
    if (oldPositions.length > NJetPositions) oldPositions.dequeue()

    val events = simulatorEvents
    simulatorEvents = List.empty[SimulatorEvent]
    events
  }

  def checkForArrival(): Unit =
    for (event <- dynamics.checkArrivalConditions())
      enqueueEvent(event)

  def enqueueEvent(event: DroneEvent): Unit = eventQueue.enqueue(event)

  // TODO: mb only record hits here and do all processing as part of update()
  // NOTE: death state probable must be determined before (or at very start of) next update()
  def missileHit(missile: HomingMissile): Unit = {
    def damageHull(hull: List[Byte]): List[Byte] = hull match {
      case h :: hs =>
        if (h > 0) (h - 1).toByte :: hs
        else h :: damageHull(hs)
      case Nil => Nil
    }


    val incomingDamage = 1
    val damage = shieldGenerators.map(_.absorbDamage(incomingDamage)).getOrElse(incomingDamage)

    for (_ <- 0 until damage)
      hullState = damageHull(hullState)

    if (hitpoints == 0) {
      dynamics.remove()
      _hasDied = true
      for (s <- storage) s.droneHasDied()
    }

    addCollisionMarker(missile.position)

    mustUpdateModel()

    // TODO: only do this in multiplayer games
    if (context.isLocallyComputed) {
      _missileHits ::= MissileHit(id, missile.position, missile.id)
    }
  }

  def collidedWith(other: DroneImpl): Unit = addCollisionMarker(other.position)

  private def addCollisionMarker(collisionPosition: Vector2): Unit =
    if (context.settings.allowCollisionAnimation) {
      val collisionAngle = (collisionPosition - position).orientation - dynamics.orientation
      _collisionMarkers ::= ((
        CollisionMarkerModel(radius.toFloat, collisionAngle.toFloat),
        CollisionMarkerLifetime))
    }

  @inline final def !(command: DroneCommand) = executeCommand(command)

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
      case DepositMinerals(target) => depositMinerals(target)
      case FireMissiles(target) => fireWeapons(target)
      case HarvestMineral(mineral) => harvestResource(mineral)
    }
    if (!redundant) {
      context.replayRecorder.record(id, command)
      context.commandRecorder.foreach(_.record(id, command))
    }
  }

  def applyState(state: DroneStateChangeMsg)(implicit context: SimulationContext): Unit = {
    assert(dynamics.isInstanceOf[RemoteDroneDynamics], "Trying to apply state to locally computed drone.")
    dynamics.asInstanceOf[RemoteDroneDynamics].synchronize(state)
  }

  override def objectEnteredVision(obj: VisionTracking): Unit = obj match {
    case mineral: MineralCrystalImpl =>
      _mineralsInSight += mineral.getHandle(player)
      eventQueue.enqueue(MineralEntersSightRadius(mineral))
    case drone: DroneImpl =>
      val wrapped = drone.wrapperFor(player)
      _dronesInSight += wrapped
      if (wrapped.isEnemy) _enemiesInSight += wrapped
      else _alliesInSight += wrapped
      eventQueue.enqueue(DroneEntersSightRadius(drone))
  }

  override def objectLeftVision(obj: VisionTracking): Unit = obj match {
    case mineral: MineralCrystalImpl =>
      _mineralsInSight -= mineral.getHandle(player)
    case drone: DroneImpl =>
      val wrapped = drone.wrapperFor(player)
      _dronesInSight -= wrapped
      if (wrapped.isEnemy) _enemiesInSight -= wrapped
      else _alliesInSight -= wrapped
  }

  override def objectRemoved(obj: VisionTracking): Unit = {
    val wasVisible =
      obj match {
        case mineral: MineralCrystalImpl => _mineralsInSight.contains(mineral.getHandle(player))
        case drone: DroneImpl => _dronesInSight.contains(drone.wrapperFor(player))
      }
    if (wasVisible) objectLeftVision(obj)
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

  private def harvestResource(mineralCrystal: MineralCrystalImpl): Unit = {
    storage match {
      case Some(s) => s.harvestMineral(mineralCrystal)
      case None => warn("Harvesting resources requires a storage module.")
    }
  }

  private def depositMinerals(other: DroneImpl): Unit = {
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
    }
  }


  //+------------------------------+
  //| Drone properties             +
  //+------------------------------+
  override def position: Vector2 = dynamics.pos
  def missileCooldown: Int = weapons.map(_.cooldown).getOrElse(1)
  def hitpoints: Int = hullState.map(_.toInt).sum + shieldGenerators.map(_.currHitpoints).getOrElse(0)
  def dronesInSight: Set[Drone] = if (isDead) Set.empty else _dronesInSight
  def enemiesInSight: Set[Drone] = if (isDead) Set.empty else _enemiesInSight
  def alliesInSight: Set[Drone] = if (isDead) Set.empty else _alliesInSight
  def isConstructing: Boolean = manipulator.exists(_.isConstructing)
  def isHarvesting: Boolean = storage.exists(_.isHarvesting)
  def isMoving: Boolean = dynamics.isMoving
  def storageCapacity = spec.storageModules
  def sides = spec.sides
  def radius = spec.radius
  def player = context.player

  def availableStorage: Int = {
    for (s <- storage) yield s.predictedAvailableStorage
  }.getOrElse(0)

  def storedResources: Int = {
    for (s <- storage) yield s.predictedStoredResources
  }.getOrElse(0)

  def isInHarvestingRange(mineral: MineralCrystalImpl): Boolean =
    (mineral.position - position).lengthSquared <= HarvestingRange * HarvestingRange

  def wrapperFor(player: Player): Drone = {
    if (player == this.player) controller
    else {
      if (!handles.contains(player)) handles += player -> new EnemyDrone(this, player)
      handles(player)
    }
  }

  private[drone] def mustUpdateModel(): Unit = {
    cachedDescriptor = None
  }

  override def descriptor: Seq[ModelDescriptor[_]] = {
    val positionDescr =
      PositionDescriptor(
        position.x,
        position.y,
        dynamics.orientation
      )
    val harvestBeams =
      for {
        s <- storage
        d <- s.beamDescriptor
      } yield ModelDescriptor(positionDescr, d)
    val constructionBeams =
      for {
        m <- manipulator
        d <- m.beamDescriptor
      } yield ModelDescriptor(positionDescr, d)

    Seq(
      ModelDescriptor(
        positionDescr,
        cachedDescriptor.getOrElse(recreateDescriptor()),
        modelParameters
      )
    ) ++ storage.toSeq.flatMap(_.energyGlobeAnimations) ++ harvestBeams.toSeq ++ constructionBeams.toSeq ++
      _collisionMarkers.map(cm => ModelDescriptor(positionDescr, cm._1, cm._2 / CollisionMarkerLifetime))
  }

  private def recreateDescriptor(): DroneModel = {
    val newDescriptor =
      DroneModel(
        spec.sides,
        moduleDescriptors,
        shieldGenerators.nonEmpty,
        hullState,
        constructionProgress.nonEmpty,
        if (spec.engines > 0 && context.settings.allowModuleAnimation && constructionProgress.isEmpty)
          context.simulator.timestep % 100
        else 0,
        player.color
      )
    cachedDescriptor = Some(newDescriptor)
    newDescriptor
  }

  private def modelParameters = DroneModelParameters(
    shieldGenerators.map(_.hitpointPercentage),
    constructionProgress.map(p => Float0To1(p / spec.buildTime.toFloat))
  )

  private def moduleDescriptors: Seq[DroneModuleDescriptor] = {
    for {
      Some(m) <- droneModules
      descr <- m.descriptors
    } yield descr
  }

  def error(message: String): Unit = {
    if (messageCooldown <= 0 && context.settings.allowMessages) {
      messageCooldown = MessageCooldown
      Errors.addMessage(message, position, errors.Error)
    }
  }

  def warn(message: String): Unit = {
    if (messageCooldown <= 0 && context.settings.allowMessages) {
      messageCooldown = MessageCooldown
      Errors.warn(message, position)
    }
  }

  def inform(message: String): Unit = {
    if (messageCooldown <= 0 && context.settings.allowMessages) {
      messageCooldown = MessageCooldown
      Errors.inform(message, position)
    }
  }

  def showText(message: String): Unit = {
    debugText = Some(debugText match {
      case Some(text) => s"$text;$message"
      case None => message
    })
  }

  def textModel: Option[TextModel] =
    for (message <- debugText)
      yield TextModel(message, position.x, position.y, ColorRGBA(ColorRGB(1, 1, 1) - context.player.color, 1))

  override def isDead = _hasDied

  private[core] def deathEvents: Seq[SimulatorEvent] =
    if (isDead) {
      var events = List[SimulatorEvent](DroneKilled(this))
      for {
        m <- manipulator
        d <- m.droneInConstruction
      } events ::= DroneConstructionCancelled(d)
      events
    } else Seq.empty



  def hasMoved = _hasMoved

  override def toString: String = id.toString
}


import upickle.default._
private[core] sealed trait SerializableDroneCommand
@key("Construct") private[core] case class SerializableConstructDrone(spec: DroneSpec, position: Vector2) extends SerializableDroneCommand
@key("FireMissiles") private[core] case class SerializableFireMissiles(targetID: Int) extends SerializableDroneCommand
@key("Deposit") private[core] case class SerializableDepositMinerals(targetID: Int) extends SerializableDroneCommand
@key("Harvest") private[core] case class SerializableHarvestMineral(mineralID: Int) extends SerializableDroneCommand
@key("MoveToMineral") private[core] case class SerializableMoveToMineralCrystal(mineralCrystalID: Int) extends SerializableDroneCommand
@key("MoveToDrone") private[core] case class SerializableMoveToDrone(droneID: Int) extends SerializableDroneCommand


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
      else throw new Exception(f"Cannot find drone with id $droneID. Available IDs: ${context.droneRegistry.keys}")
    }
    implicit def mineralLookup(mineralID: Int): MineralCrystalImpl = {
      if (context.mineralRegistry.contains(mineralID)) context.mineralRegistry(mineralID)
      else throw new Exception(f"Cannot find mineral with id $mineralID. Available IDs: ${context.mineralRegistry.keys}")
    }
    serialized match {
      case SerializableConstructDrone(spec, position) => ConstructDrone(spec, new DummyDroneController, position)
      case SerializableFireMissiles(target) => FireMissiles(target)
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
  position: Vector2
) extends DroneCommand {
  def toSerializable = SerializableConstructDrone(spec, position)
}
private[core] case class FireMissiles(target: DroneImpl) extends DroneCommand {
  def toSerializable = SerializableFireMissiles(target.id)
}
private[core] case class DepositMinerals(target: DroneImpl) extends DroneCommand {
  def toSerializable = SerializableDepositMinerals(target.id)
}
private[core] case class HarvestMineral(mineral: MineralCrystalImpl) extends DroneCommand {
  def toSerializable = SerializableHarvestMineral(mineral.id)
}

private[core] sealed trait MovementCommand extends DroneCommand
@key("MoveInDirec") private[core] case class MoveInDirection(direction: Float) extends MovementCommand with SerializableDroneCommand {
  def toSerializable = this
}
@key("MoveToPos") private[core] case class MoveToPosition(position: Vector2) extends MovementCommand with SerializableDroneCommand {
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

private[core] case class SerializableSpawn(spec: DroneSpec, position: Vector2, playerID: Int, resources: Int, name: Option[String]) {
  def deserialize: Spawn =
    Spawn(spec, position, Player.fromID(playerID), resources, name)
}
private[core] object SerializableSpawn {
  def apply(spawn: Spawn): SerializableSpawn =
    SerializableSpawn(spawn.droneSpec, spawn.position, spawn.player.id, spawn.resources, spawn.name)
}

private[core] sealed trait MultiplayerMessage

private[core] case class CommandsMessage(
  commands: Seq[(Int, SerializableDroneCommand)]
) extends MultiplayerMessage

private[core] case class WorldStateMessage(
  missileHits: Seq[MissileHit],
  stateChanges: Seq[DroneStateChangeMsg]
) extends MultiplayerMessage

private[core] case class InitialSync(
  worldSize: Rectangle,
  minerals: Seq[MineralSpawn],
  initialDrones: Seq[SerializableSpawn],
  localPlayerIDs: Set[Int],
  remotePlayerIDs: Set[Int],
  rngSeed: Int
) extends MultiplayerMessage {
  def worldMap: WorldMap = new WorldMap(
    minerals,
    worldSize,
    initialDrones.map(_.deserialize),
    None
  )

  def localPlayers: Set[Player] = localPlayerIDs.map(Player.fromID)
  def remotePlayers: Set[Player] = remotePlayerIDs.map(Player.fromID)
}

private[core] case object Register extends MultiplayerMessage

private[core] case class RTT(sent: Long, msg: String) extends MultiplayerMessage


private[core] object MultiplayerMessage {
  def parse(json: String): MultiplayerMessage =
    read[MultiplayerMessage](json)

  def parseBytes(bytes: ByteBuffer): MultiplayerMessage = {
    Unpickle[MultiplayerMessage].fromBytes(bytes)
  }

  def serializeBinary(msg: MultiplayerMessage): ByteBuffer = Pickle.intoBytes(msg)

  def serialize(commands: Seq[(Int, SerializableDroneCommand)]): String =
    write(CommandsMessage(commands))

  def serializeBinary(commands: Seq[(Int, SerializableDroneCommand)]): ByteBuffer =
    Pickle.intoBytes[MultiplayerMessage](CommandsMessage(commands))

  def serialize(
    worldSize: Rectangle,
    minerals: Seq[MineralSpawn],
    initialDrones: Seq[Spawn],
    localPlayers: Set[Player],
    remotePlayers: Set[Player],
    rngSeed: Int
  ): String = write(InitialSync(
    worldSize,
    minerals,
    initialDrones.map(x => SerializableSpawn(x)),
    localPlayers.map(_.id),
    remotePlayers.map(_.id),
    rngSeed
  ))

  def serializeBinary(
    worldSize: Rectangle,
    minerals: Seq[MineralSpawn],
    initialDrones: Seq[Spawn],
    localPlayers: Set[Player],
    remotePlayers: Set[Player],
    rngSeed: Int
  ): ByteBuffer = Pickle.intoBytes[MultiplayerMessage](InitialSync(
    worldSize,
    minerals,
    initialDrones.map(x => SerializableSpawn(x)),
    localPlayers.map(_.id),
    remotePlayers.map(_.id),
    rngSeed
  ))

  def register: String = write(Register)

  def registerBinary: ByteBuffer = Pickle.intoBytes[MultiplayerMessage](Register)
}

private[core] object DroneOrdering extends Ordering[DroneImpl] {
  override def compare(d1: DroneImpl, d2: DroneImpl): Int =
    if (d1.priority == d2.priority) d1.id - d2.id
    else d1.id - d2.id
}

