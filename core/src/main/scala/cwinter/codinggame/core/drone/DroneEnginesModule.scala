package cwinter.codinggame.core.drone

import cwinter.codinggame.core.{SimulatorEvent, SpawnLaserMissile}
import cwinter.worldstate

class DroneEnginesModule(positions: Seq[Int], owner: Drone)
  extends DroneModule(positions, owner) {

  override def update(availableResources: Int): (Seq[SimulatorEvent], Int) = NoEffects

  override def descriptors: Seq[worldstate.DroneModule] = positions.map(worldstate.Engines)
}

