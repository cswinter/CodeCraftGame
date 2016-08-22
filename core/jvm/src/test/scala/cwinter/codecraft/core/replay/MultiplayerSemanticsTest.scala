package cwinter.codecraft.core.replay

import akka.actor.{ActorSystem, ActorRef}
import cwinter.codecraft.core.TestUtils
import cwinter.codecraft.core.ai.deterministic.DeterministicMothership
import cwinter.codecraft.core.api.TheGameMaster
import cwinter.codecraft.core.game.DroneWorldSimulator
import cwinter.codecraft.core.multiplayer.Server
import cwinter.codecraft.util.maths.GlobalRNG
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.duration._


class MultiplayerSemanticsTest extends FlatSpec {
  val timesteps = 7500
  val seed = 42
  val tickPeriod = 10
  val mapSeed: Option[Int] = Some(-1312233628)
  @volatile var serverRef = Option.empty[ActorRef]
  def ai() = new DeterministicMothership(seed)
  mapSeed.foreach(GlobalRNG.seed = _)

  s"A multiplayer game (map ${GlobalRNG.seed})" should "yield the same output as an identical singleplayer game" in {
    DroneWorldSimulator.enableDetailedLogging()
    try {
      TestUtils.runAndCompare(singleplayerGame(), multiplayerGame(), timesteps)
    } finally {
      serverRef.foreach(_ ! Server.Stop)
    }
  }

  def multiplayerGame(): DroneWorldSimulator = {
    new Thread {
      override def run(): Unit = {
        serverRef = Some(Server.start(seed, TheGameMaster.level1Map, displayGame = false))
        ActorSystem().awaitTermination()
      }
    }.start()
    Thread.sleep(2000, 0)

    def connectClient() = Await.result(
      TheGameMaster.prepareMultiplayerGame("localhost", ai()),
      5.seconds
    )
    val client1 = connectClient()
    client1.framerateTarget = 1001
    client1.run()
    connectClient()
  }

  def singleplayerGame(): DroneWorldSimulator = {
    val config = TheGameMaster.level1Map.createGameConfig(Seq(ai(), ai()), rngSeed = seed, tickPeriod = 10)
    new DroneWorldSimulator(config)
  }
}

