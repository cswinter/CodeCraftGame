package cwinter.codecraft.testai

import cwinter.codecraft.core.api._

object RunTestAI {
  def main(args: Array[String]): Unit = {
    TheGameMaster.startGame(TheGameMaster.replicatorAI(), TheGameMaster.destroyerAI())
  }
}

