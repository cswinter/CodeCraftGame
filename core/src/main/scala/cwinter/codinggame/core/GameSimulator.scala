package cwinter.codinggame.core

import cwinter.codinggame.collisions.VisionTracker
import cwinter.codinggame.core.api.{DroneController, DroneSpec}
import cwinter.codinggame.core.objects.drone._
import cwinter.codinggame.core.errors.Errors
import cwinter.codinggame.core.objects._
import cwinter.codinggame.physics.PhysicsEngine
import cwinter.codinggame.util.maths.{Rng, Vector2}
import cwinter.codinggame.util.modules.ModulePosition
import cwinter.codinggame.worldstate._


class GameSimulator(
  val map: WorldMap,
  mothershipController1: DroneController,
  mothershipController2: DroneController,
  eventGenerator: Int => Seq[SimulatorEvent]
) extends GameWorld {
  final val SightRadius = 500
  final val MaxDroneRadius = 60

  private val visibleObjects = collection.mutable.Set.empty[WorldObject]
  private val dynamicObjects = collection.mutable.Set.empty[WorldObject]
  private val drones = collection.mutable.Set.empty[Drone]
  private var deadDrones = List.empty[Drone]

  private val visionTracker = new VisionTracker[WorldObject](
    map.size.xMin.toInt, map.size.xMax.toInt,
    map.size.yMin.toInt, map.size.yMax.toInt,
    SightRadius
  )

  private val physicsEngine = new PhysicsEngine[ConstantVelocityDynamics](
    map.size, MaxDroneRadius
  )

  map.minerals.foreach(spawnMineral)
  // TODO: check map bounds
  val mothership1 = mothership(BluePlayer, mothershipController1, map.spawns(0))
  val mothership2 = mothership(OrangePlayer, mothershipController2, map.spawns(1))
  spawnDrone(mothership1)
  spawnDrone(mothership2)



  override def worldState: Iterable[WorldObjectDescriptor] = visibleObjects.flatMap(_.descriptor)

  private def spawnMineral(mineralCrystal: MineralCrystal): Unit = {
    visibleObjects.add(mineralCrystal)
    visionTracker.insert(mineralCrystal)
  }


  private def mothership(player: Player, controller: DroneController, pos: Vector2): Drone = {
    val spec = new DroneSpec(
      size = 7,
      missileBatteries = 2,
      manipulators = 2,
      refineries = 3,
      storageModules = 3
    )
    new Drone(spec, controller, player, pos, 0, 21)
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

  override def timestep = physicsEngine.timestep
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

