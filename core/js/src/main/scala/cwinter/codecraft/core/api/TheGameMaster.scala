package cwinter.codecraft.core.api

import cwinter.codecraft.core.DroneWorldSimulator
import cwinter.codecraft.graphics.engine.Renderer
import cwinter.codecraft.graphics.worldstate.WorldObjectDescriptor
import cwinter.codecraft.util.maths.Rectangle
import org.scalajs.dom
import org.scalajs.dom.html

object TheGameMaster extends GameMasterLike {
  var canvas: html.Canvas = null


  def runWithAscii(simulator: DroneWorldSimulator): Unit = {
    println("Starting simulator...")
    dom.setInterval(() => {
      if (render != null) {
        render(simulator.worldState, simulator.map.size)
      }
      println("t = " + simulator.timestep)
      println("object count: " + simulator.worldState.length)
      simulator.run(1)
    }, 30)
    println("Success")
  }

  def run(simulator: DroneWorldSimulator): Unit = {
    require(canvas != null, "Must first set TheGameMaster.canvas variable to the webgl canvas element.")
    val renderer = new Renderer(canvas, simulator, simulator.map.spawns.head)
    dom.setInterval(() => {
      renderer.render()
      simulator.run(1)
    }, 30)
  }
  
  
  private[codecraft] var render: (Seq[WorldObjectDescriptor], Rectangle) => Unit = null
}

