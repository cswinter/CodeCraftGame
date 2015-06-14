package cwinter.codecraft.core

import java.awt.event.KeyEvent

import cwinter.codecraft.collisions.VisionTracker
import cwinter.codecraft.core.api.{DroneControllerBase, DroneSpec}
import cwinter.codecraft.core.errors.Errors
import cwinter.codecraft.core.objects._
import cwinter.codecraft.core.objects.drone._
import cwinter.codecraft.core.replay._
import cwinter.codecraft.graphics.engine.Debug
import cwinter.codecraft.physics.PhysicsEngine
import cwinter.codecraft.util.maths.{ColorRGB, Rng, Vector2}
import cwinter.codecraft.util.modules.ModulePosition
import cwinter.codecraft.worldstate._


class DroneWorldSimulator(
  val map: WorldMap,
  mothershipController1: DroneControllerBase,
  mothershipController2: DroneControllerBase,
  eventGenerator: Int => Seq[SimulatorEvent],
  replayer: Option[Replayer] = None
) extends Simulator {
  final val MaxDroneRadius = 60

  private var showSightRadius = false
  private var showMissileRadius = false

  val replayRecorder =
    if (replayer.isEmpty) new FileReplayRecorder(System.getProperty("user.home") + "/.codecraft/replays")
    else NullReplayRecorder

  val worldConfig = WorldConfig(map.size)

  private val visibleObjects = collection.mutable.Set.empty[WorldObject]
  private val dynamicObjects = collection.mutable.Set.empty[WorldObject]
  private val drones = collection.mutable.Set.empty[Drone]
  private var deadDrones = List.empty[Drone]


  private val visionTracker = new VisionTracker[WorldObject](
    map.size.xMin.toInt, map.size.xMax.toInt,
    map.size.yMin.toInt, map.size.yMax.toInt,
    DroneSpec.SightRadius
  )

  private val physicsEngine = new PhysicsEngine[ConstantVelocityDynamics](
    map.size, MaxDroneRadius
  )

  Debug.drawAlways(DrawRectangle(0, map.size))

  replayRecorder.recordVersion()
  replayRecorder.recordRngSeed(Rng.seed)
  map.minerals.foreach(spawnMineral)
  // TODO: check map bounds
  val mothership1 = mothership(BluePlayer, mothershipController1, map.spawns(0))
  val mothership2 = mothership(OrangePlayer, mothershipController2, map.spawns(1))
  spawnDrone(mothership1)
  spawnDrone(mothership2)



  private def spawnMineral(mineralCrystal: MineralCrystal): Unit = {
    visibleObjects.add(mineralCrystal)
    visionTracker.insert(mineralCrystal)
  }


  private def mothership(player: Player, controller: DroneControllerBase, pos: Vector2): Drone = {
    val spec = new DroneSpec(
      missileBatteries = 2,
      constructors = 2,
      refineries = 3,
      storageModules = 3
    )
    replayRecorder.recordSpawn(spec, pos, player)
    new Drone(spec, controller, player, pos, 0, worldConfig, replayRecorder, 21)
  }

  private def spawnDrone(drone: Drone): Unit = {
    visibleObjects.add(drone)
    dynamicObjects.add(drone)
    drones.add(drone)
    visionTracker.insert(drone, generateEvents=true)
    physicsEngine.addObject(drone.dynamics)
    drone.initialise(physicsEngine.time)
  }

  private def droneKilled(drone: Drone): Unit = {
    visibleObjects.remove(drone)
    dynamicObjects.remove(drone)
    drones.remove(drone)
    visionTracker.remove(drone)
    physicsEngine.remove(drone.dynamics)

    deadDrones ::= drone
    drone.enqueueEvent(Destroyed)

    for {
      i <- 0 until ModulePosition.moduleCount(drone.size)
      pos = drone.position + Rng.double(0, drone.radius) * Rng.vector2()
    } spawnLightflash(pos)
  }

  private def spawnMissile(missile: HomingMissile): Unit = {
    visibleObjects.add(missile)
    dynamicObjects.add(missile)
    physicsEngine.addObject(missile.dynamics)
  }

  private def spawnLightflash(position: Vector2): Unit = {
    val lightFlash = new LightFlash(position)
    visibleObjects.add(lightFlash)
    dynamicObjects.add(lightFlash)
  }

  override def update(): Unit = {
    replayRecorder.newTimestep(timestep)

    if (showMissileRadius) {
      for (
        d <- drones
        if d.spec.missileBatteries > 0
      ) {
        Debug.draw(DrawCircleOutline(d.position.x.toFloat, d.position.y.toFloat, DroneConstants.MissileLockOnRadius, ColorRGB(1, 0, 0)))
      }
    }
    if (showSightRadius) {
      for (d <- drones) {
        Debug.draw(DrawCircleOutline(d.position.x.toFloat, d.position.y.toFloat, DroneSpec.SightRadius, ColorRGB(0, 1, 0)))
      }
    }

    if (replayer.exists(_.finished)) return

    // check win condition
    if (timestep % 30 == 0) {
      if (mothership1.hasDied) {
        for (drone <- drones) {
          if (drone.player == mothership2.player) {
            Errors.inform("Victory!", drone.position)
          }
        }
      } else if (mothership2.hasDied) {
        for (drone <- drones) {
          if (drone.player == mothership1.player) {
            Errors.inform("Victory!", drone.position)
          }
        }
      }
    }

    for (r <- replayer) {
      implicit val droneRegistry = drones.map(d => (d.id, d)).toMap
      implicit val mineralRegistry = map.minerals.map(m => (m.id, m)).toMap
      r.run(timestep)
    }

    // handle all drone events (execute user code)
    for (drone <- drones) {
      drone.processEvents()
    }

    for (drone <- deadDrones) {
      drone.processEvents()
    }
    deadDrones = List.empty[Drone]

    // execute game mechanics for all objects + collect resulting events
    val simulatorEvents = {
      for (obj <- dynamicObjects; event <- obj.update())
        yield event
    } ++ eventGenerator(timestep)


    simulatorEvents.foreach {
      case MineralCrystalHarvested(mineralCrystal) =>
        visibleObjects.remove(mineralCrystal)
        visionTracker.remove(mineralCrystal)
      case MineralCrystalDestroyed(mineralCrystal) =>
        visibleObjects.remove(mineralCrystal)
      case MineralCrystalActivated(mineralCrystal) =>
        visibleObjects.add(mineralCrystal)
    case MineralCrystalInactivated(mineralCrystal) =>
        visibleObjects.remove(mineralCrystal)
      case DroneConstructionStarted(drone) =>
        visibleObjects.add(drone)
      case DroneConstructionCancelled(drone) =>
        visibleObjects.remove(drone)
      case SpawnHomingMissile(player, position, target) =>
        // TODO: remove this check once boundary collisions are done properly
        if (map.size.contains(position)) {
        val newMissile = new HomingMissile(player, position, physicsEngine.time, target)
          spawnMissile(newMissile)
        }
      case HomingMissileFaded(missile) =>
        visibleObjects.remove(missile)
        dynamicObjects.remove(missile)
        physicsEngine.remove(missile.dynamics)
      case MissileExplodes(missile) =>
        spawnLightflash(missile.position)
      case LightFlashDestroyed(lightFlash) =>
        visibleObjects.remove(lightFlash)
        dynamicObjects.remove(lightFlash)
      case DroneKilled(drone) =>
        droneKilled(drone)
      case SpawnDrone(drone) =>
        spawnDrone(drone)
      case SpawnEnergyGlobeAnimation(energyGlobeObject) =>
        visibleObjects.add(energyGlobeObject)
        dynamicObjects.add(energyGlobeObject)
      case RemoveEnergyGlobeAnimation(energyGlobeObject) =>
        visibleObjects.remove(energyGlobeObject)
        dynamicObjects.remove(energyGlobeObject)
    }

    physicsEngine.update()

    // COLLECT ALL EVENTS FROM PHYSICS SIMULATION

    visionTracker.updateAll()
    for (drone <- drones) drone.objectsInSight = visionTracker.getVisible(drone)
    // SPAWN NEW OBJECTS HERE???
    for {
      (drone: Drone, events) <- visionTracker.collectEvents()
      event <- events
    } event match {
      case visionTracker.EnteredSightRadius(mineral: MineralCrystal) =>
        drone.enqueueEvent(MineralEntersSightRadius(mineral))
      case visionTracker.LeftSightRadius(obj) => // don't care (for now)
      case visionTracker.EnteredSightRadius(other: Drone) =>
        drone.enqueueEvent(DroneEntersSightRadius(other))
      case e => throw new Exception(s"AHHHH, AN UFO!!! RUN FOR YOUR LIFE!!! $e")
    }

    Errors.updateMessages()
  }


  override def computeWorldState: Iterable[WorldObjectDescriptor] = {
    visibleObjects.flatMap(_.descriptor)
  }


  override def initialCameraPos: Vector2 = map.spawns.head


  override def handleKeypress(keyEvent: KeyEvent): Unit = {
    keyEvent.getKeyChar match {
      case '1' => showSightRadius = !showSightRadius
      case '2' => showMissileRadius = !showMissileRadius
      case _ =>
    }
  }


  override def additionalInfoText: String =
    s"""${if (showSightRadius) "Hide" else "Show"} sight radius: 1
       |${if (showMissileRadius) "Hide" else "Show"} missile range: 2
       |${replayRecorder match { case frr: FileReplayRecorder => "Replay path: " + frr.filename case _ => ""}}
     """.stripMargin
}



sealed trait SimulatorEvent
case class MineralCrystalHarvested(mineralCrystal: MineralCrystal) extends SimulatorEvent
case class DroneConstructionStarted(drone: Drone) extends SimulatorEvent
case class SpawnDrone(drone: Drone) extends SimulatorEvent
case class MineralCrystalActivated(mineralCrystal: MineralCrystal) extends SimulatorEvent
case class MineralCrystalInactivated(mineralCrystal: MineralCrystal) extends SimulatorEvent
case class MineralCrystalDestroyed(mineralCrystal: MineralCrystal) extends SimulatorEvent
case class SpawnHomingMissile(player: Player, position: Vector2, target: Drone) extends SimulatorEvent
case class HomingMissileFaded(missile: HomingMissile) extends SimulatorEvent
case class MissileExplodes(homingMissile: HomingMissile) extends SimulatorEvent
case class LightFlashDestroyed(lightFlash: LightFlash) extends SimulatorEvent
case class DroneKilled(drone: Drone) extends SimulatorEvent
case class DroneConstructionCancelled(drone: Drone) extends SimulatorEvent
case class SpawnEnergyGlobeAnimation(energyGlobeObject: EnergyGlobeObject) extends SimulatorEvent
case class RemoveEnergyGlobeAnimation(energyGlobeObject: EnergyGlobeObject) extends SimulatorEvent

