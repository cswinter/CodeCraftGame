package cwinter.codecraft.core.replay

import akka.actor.{ActorRef, ActorSystem}
import cwinter.codecraft.core.TestUtils
import cwinter.codecraft.core.ai.deterministic.DeterministicMothership
import cwinter.codecraft.core.api.TheGameMaster
import cwinter.codecraft.core.game.{Settings, DroneWorldSimulator}
import cwinter.codecraft.core.multiplayer.Server
import cwinter.codecraft.util.maths.GlobalRNG
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class MultiplayerSemanticsTest extends FlatSpec {
  val timesteps = 7500
  val seed = 42
  val tickPeriod = 10
  val mapSeed: Option[Int] = None
  @volatile var serverRef = Option.empty[ActorRef]
  def ai() = new DeterministicMothership(seed)
  mapSeed.foreach(GlobalRNG.seed = _)
  val map = TheGameMaster.level1Map

  s"A multiplayer game (map ${GlobalRNG.seed})" should "yield the same output as an identical singleplayer game" in {
    Settings(allowFramePrecomputation = false).setAsDefault()
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
        serverRef = Some(Server.start(seed, map, displayGame = false, winConditions = Seq.empty))
        ActorSystem().awaitTermination()
      }
    }.start()
    Thread.sleep(2000, 0)

    def connectClient() = TheGameMaster.prepareMultiplayerGame("localhost", ai())

    val c1fut = connectClient()
    val c2fut = connectClient()
    for (client1 <- c1fut) {
      client1.framerateTarget = 1001
      client1.run()
    }

    Await.result(c2fut, 5.seconds)
  }

  def singleplayerGame(): DroneWorldSimulator = {
    val config = map.createGameConfig(Seq(ai(), ai()), rngSeed = seed, tickPeriod = tickPeriod)
    new DroneWorldSimulator(config)
  }
}
