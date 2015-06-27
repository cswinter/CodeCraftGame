package cwinter.codecraft.core.api

import cwinter.codecraft.core.DroneWorldSimulator
import cwinter.codecraft.graphics.worldstate.WorldObjectDescriptor
import cwinter.codecraft.util.maths.Rectangle
import org.scalajs.dom

object TheGameMaster extends GameMasterLike {
  override def run(simulator: DroneWorldSimulator): Unit = {
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
  
  
  private[codecraft] var render: (Seq[WorldObjectDescriptor], Rectangle) => Unit = null
}

