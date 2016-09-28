package cwinter.codecraft.scalajs

import cwinter.codecraft.core.api.{DroneControllerBase, TheGameMaster}
import cwinter.codecraft.core.game
import cwinter.codecraft.core.game.DroneWorldSimulator
import org.scalajs.dom
import org.scalajs.dom.{document, html}

import scala.scalajs.js.annotation.JSExport

@JSExport
object Main {
  @JSExport
  def webgl(canvas: html.Canvas): Unit = {
    val multiplayer = true
    new game.Settings(recordReplays = false).setAsDefault()
    TheGameMaster.canvas = canvas
    TheGameMaster.outputFPS = true

    if (multiplayer) {
      import scala.concurrent.ExecutionContext.Implicits.global
      val simulator = TheGameMaster.prepareMultiplayerGame("localhost", TheGameMaster.replicatorAI())
      simulator.onSuccess {
        case s: DroneWorldSimulator => TheGameMaster.run(s)
      }
    } else {
      run(TheGameMaster.replicatorAI(), TheGameMaster.replicatorAI())
    }

    document.getElementById("btn-gameplay").asInstanceOf[html.Button].onclick = (e: dom.Event) => {
      TheGameMaster.stop()
      run(TheGameMaster.replicatorAI(), TheGameMaster.replicatorAI())
    } /*
    document.getElementById("btn-physics").asInstanceOf[html.Button].onclick = (e: dom.Event) => {
      reset()
      TheObjectManager.main(Array())
    }
    document.getElementById("btn-graphics").asInstanceOf[html.Button].onclick = (e: dom.Event) => {
      reset()
      BlogpostDemo.main(Array())
    }*/
  }

  def run(m1: DroneControllerBase, m2: DroneControllerBase): Unit = {
    val simulator = new DroneWorldSimulator(
      TheGameMaster.defaultMap.createGameConfig(Seq(m1, m2), tickPeriod = 1))
    TheGameMaster.run(simulator)
  }
}
