package cwinter.codecraft.scalajs

import cwinter.codecraft.core.api.TheGameMaster
import cwinter.codecraft.graphics.engine.AsciiVisualizer
import cwinter.codecraft.graphics.materials.Material
import cwinter.codecraft.graphics.worldstate.WorldObjectDescriptor
import cwinter.codecraft.util.maths.matrices.IdentityMatrix4x4
import cwinter.codecraft.util.maths.{EmptyVertex, Rectangle, VertexXYZ}
import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.raw.{WebGLRenderingContext => GL}

import scala.scalajs.js.annotation.JSExport


@JSExport
object Main {
  @JSExport
  def main(target: html.Pre): Unit = {
    println(target)
    TheGameMaster.render = render(target)
    TheGameMaster.runL3vL3()
  }

  @JSExport
  def webgl(canvas: html.Canvas): Unit = {
    val gl = initGL(canvas)
    object SimpleMaterial extends Material[VertexXYZ, EmptyVertex.type, Unit](
      gl = gl,
      vsID = "shader-vs",
      fsID = "shader-fs",
      attributeNamePos = "aVertexPosition",
      None)

    val vertexData = Seq(
      (VertexXYZ(0, 1, 0), EmptyVertex),
      (VertexXYZ(-1, -1, 0), EmptyVertex),
      (VertexXYZ(1, -1, 0), EmptyVertex)
    )
    val vbo = SimpleMaterial.createVBO(vertexData)

    SimpleMaterial.beforeDraw(IdentityMatrix4x4)
    SimpleMaterial.draw(vbo, IdentityMatrix4x4)
    SimpleMaterial
  }

  def initGL(canvas: html.Canvas): GL = {
    val gl = canvas.getContext("experimental-webgl").asInstanceOf[GL]
    if (gl == null) {
      dom.alert("Could not initialise WebGL. INSERT LINK TO FIX HERE.")
    }
    gl.viewport(0, 0, canvas.width, canvas.height)
    gl
  }

  def render(target: html.Pre)(objects: Seq[WorldObjectDescriptor], mapSize: Rectangle): Unit = {
    target.innerHTML = AsciiVisualizer.show(objects, mapSize)
  }
}

