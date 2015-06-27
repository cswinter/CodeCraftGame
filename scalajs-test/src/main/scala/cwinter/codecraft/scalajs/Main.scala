package cwinter.codecraft.scalajs

import cwinter.codecraft.core.api.TheGameMaster

import scala.scalajs.js.annotation.JSExport


@JSExport
object Main {
  @JSExport
  def main(): Unit = {
    TheGameMaster.runL1vL2()
  }
}
