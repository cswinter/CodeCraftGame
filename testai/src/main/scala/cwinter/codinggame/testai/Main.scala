package cwinter.codinggame.testai

import cwinter.codinggame.core.{DroneController, TheGameMaster}
import cwinter.codinggame.maths.{Rng, Vector2}

object Main {
  def main(args: Array[String]): Unit = {
    TheGameMaster.startGame(new Mothership)
  }
}


class Mothership extends DroneController {
  // abstract methods for event handling
  override def onSpawn(): Unit = {
    moveInDirection(Vector2(0, 1))
  }

  override def onTick(): Unit = {
    if (Rng.bernoulli(0.01)) {
      moveInDirection(Vector2(Rng.double(0, 100)))
    }
  }
}
