package cwinter.codecraft.scalajs

import cwinter.codecraft.core.api.TheGameMaster
import cwinter.codecraft.graphics.engine.AsciiVisualizer
import cwinter.codecraft.graphics.worldstate.WorldObjectDescriptor
import cwinter.codecraft.util.maths.Rectangle
import org.scalajs.dom.html

import scala.scalajs.js.annotation.JSExport


@JSExport
object Main {
  @JSExport
  def main(target: html.Pre): Unit = {
    println(target)
    TheGameMaster.render = render(target)
    TheGameMaster.runL3vL3()
  }


  def render(target: html.Pre)(objects: Seq[WorldObjectDescriptor], mapSize: Rectangle): Unit = {
    target.innerHTML = AsciiVisualizer.show(objects, mapSize)
  }
}
