package cwinter.codinggame.demos.graphics

import cwinter.codinggame.util.maths.{VertexXY, Geometry}
import cwinter.worldstate._

object BlogpostDemo {
  def main(args: Array[String]): Unit = {
    val s = new GraphicsSimulator(
      customObjects = modules,
      customChangingObjects = generateObjects,
      spawnedObjects = spawnObjects
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

  def minerals = {
    Seq(
      MineralDescriptor(0, 100, -500, 1, 1),
      MineralDescriptor(1, 130, -520, 2, 1),
      MineralDescriptor(2, 80, -525, 3, 1),
      MineralDescriptor(3, 140, -490, 5, 1),
      MineralDescriptor(4, 95, -550, 4, 1),
      MineralDescriptor(5, 105, -530, 9, 2),
      MineralDescriptor(6, 150, -550, 7, 2),
      MineralDescriptor(7, 180, -500, 8, 4)
    )
  }

  def modules = Seq[DroneModule](
    StorageModule(Seq(0), 0),
    StorageModule(Seq(0), 7),
    StorageModule(Seq(0), -1),
    Engines(0),
    ShieldGenerator(0),
    ProcessingModule(Seq(0)),
    Lasers(0)
  ).zipWithIndex.map {
    case (module, i) =>
      new MockRobot(
        xPos = -250,
        yPos = 100 * i - 200,
        orientation = 0,
        size = 3,
        modules = Seq[DroneModule](module),
        sightRadius = None,
        undamaged = true,
        dontMove = true
      )
  }

  def generateObjects(t: Int): Seq[WorldObjectDescriptor] = {
    hulls ++ minerals
  }

  def spawnObjects(t: Int): Seq[MockObject] = {
    if (t % 50 == 1) {
      Seq(new MockLaserMissile(100, -300, 0, 25))
    } else {
      Seq()
    }
  }
}
