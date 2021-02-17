package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.game.{SpawnEnergyGlobeAnimation, SimulatorEvent}
import cwinter.codecraft.core.objects.EnergyGlobeObject

private[core] trait DroneModules { self: DroneImpl =>
  protected val weapons = spec.constructMissilesBatteries(this)
  protected[core] val storage = spec.constructStorage(this, startingResources)
  protected val manipulator = spec.constructManipulatorModules(this)
  protected val shieldGenerators = spec.constructShieldGenerators(this)
  protected val engines = spec.constructEngineModules(this)
  val droneModules = Seq(weapons, manipulator, storage, shieldGenerators, engines)

  def updateModules(): Seq[SimulatorEvent] = {
    var simulatorEvents = List.empty[SimulatorEvent]
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
    simulatorEvents
  }
}
