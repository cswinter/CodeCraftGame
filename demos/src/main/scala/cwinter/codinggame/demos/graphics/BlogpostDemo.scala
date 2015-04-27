package cwinter.codinggame.demos.graphics

import cwinter.codinggame.util.maths.{VertexXY, Geometry}
import cwinter.worldstate._

object BlogpostDemo {
  def main(args: Array[String]): Unit = {
    val s = new GraphicsSimulator(
      customObjects = modules,
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

  def modules = Seq[DroneModule](
    StorageModule(Seq(0), 0),
    StorageModule(Seq(0), 7),
    StorageModule(Seq(0), -1),
    Engines(0),
    ShieldGenerator(0),
    ProcessingModule(Seq(0))
  ).zipWithIndex.map {
    case (module, i) =>
      new MockRobot(
        xPos = -250,
        yPos = 100 * i - 200,
        orientation = 0,
        size = 3,
        modules = Seq[DroneModule](module),
        sightRadius = None,
        dontMove = true
      )
  }

  def generateObjects(t: Int): Seq[WorldObjectDescriptor] = {
    hulls
  }
}
