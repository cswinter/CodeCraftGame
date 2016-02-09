package cwinter.codecraft.testai

import cwinter.codecraft.core.api._
import cwinter.codecraft.testai.replicator.Replicator

object RunTestAI {
  def main(args: Array[String]): Unit = {
    TheGameMaster.startGame(TheGameMaster.replicatorAI(), TheGameMaster.replicatorAI())
  }
}

