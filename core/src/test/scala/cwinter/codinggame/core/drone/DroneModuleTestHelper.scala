package cwinter.codinggame.core.drone

import cwinter.codinggame.core.SimulatorEvent

object DroneModuleTestHelper {
  def multipleUpdates(module: DroneModule, count: Int): (Seq[SimulatorEvent], Int) = {
    val allUpdates = for {
      _ <- 0 until count
    } yield module.update(0)

    allUpdates.foldLeft((Seq.empty[SimulatorEvent], 0)){
      case ((es1, r1), (es2, r2)) => (es1 ++ es2, r1 + r2)
    }
  }
}
