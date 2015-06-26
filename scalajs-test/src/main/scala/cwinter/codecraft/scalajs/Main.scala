package cwinter.codecraft.scalajs

import scala.scalajs.js.JSApp

import cwinter.codecraft.core.api.TheGameMaster


object Main extends JSApp {
  def main(): Unit = {
    TheGameMaster.runL1vL2()
  }
}
