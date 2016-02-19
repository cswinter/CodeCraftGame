package cwinter.codecraft.testai

import cwinter.codecraft.core.api._

object RunTestAI {
  def main(args: Array[String]): Unit = {
    TheGameMaster.startGame(TheGameMaster.destroyerAI(), TheGameMaster.level2AI())
  }
}

