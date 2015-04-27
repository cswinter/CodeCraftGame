package cwinter.codinggame.demos.graphics

import cwinter.codinggame.util.maths.{VertexXY, Geometry}
import cwinter.worldstate.{BluePlayer, DroneDescriptor, WorldObjectDescriptor}

object BlogpostDemo {
  def main(args: Array[String]): Unit = {
    val s = new GraphicsSimulator(
      customObjects = Seq.empty,
      customChangingObjects = generateObjects
    )
    s.run()
  }


  val sideLength = 40
  def radius(size: Int): Double = {
    val radiusBody = 0.5f * sideLength / math.sin(math.Pi / size).toFloat
    radiusBody + Geometry.circumradius(4, size)
  }

  def hulls = {
    var xPos = 0.0f
    Seq.tabulate(5) { i =>
      val size = i + 3
      val r = radius(size).toFloat
      val position = VertexXY(xPos, Geometry.inradius(r, size))
      val orientation = math.Pi.toFloat / 2
      xPos += 2.3f * r

      DroneDescriptor(
        identifier = i,
        xPos = position.x,
        yPos = position.y,
        orientation = orientation,
        positions = Seq(),
        modules = Seq(),
        hullState = Seq.fill[Byte](size - 1)(2),
        size = size,
        player = BluePlayer
      )
    }
  }

  def generateObjects(t: Int): Seq[WorldObjectDescriptor] = {
    hulls
  }
}
