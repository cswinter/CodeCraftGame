package cwinter.codecraft.core.game

import cwinter.codecraft.core.api.DroneControllerBase
import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.util.maths.Rectangle

/** Aggregates all pieces of information required to start a game. */
private[core] case class GameConfig(
  worldSize: Rectangle,
  minerals: Seq[MineralSpawn],
  drones: Seq[(Spawn, DroneControllerBase)],
  winConditions: Seq[WinCondition],
  tickPeriod: Int,
  rngSeed: Int
) {
  def instantiateMinerals(): Seq[MineralCrystalImpl] =
    for ((MineralSpawn(size, position), id) <- minerals.zipWithIndex)
      yield new MineralCrystalImpl(size, id, position)
}
