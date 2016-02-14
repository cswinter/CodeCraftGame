package cwinter.codecraft.demos.graphics

import cwinter.codecraft.graphics.worldstate._
import cwinter.codecraft.util.maths.{ColorRGB, VertexXY, Geometry}

private[codecraft] object BlogpostDemo {
  def main(args: Array[String]): Unit = {
    val s = new GraphicsSimulator(
      customObjects = modules,
      customChangingObjects = generateObjects,
      spawnedObjects = spawnObjects
    )
    s.start()
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

      ModelDescriptor(
        PositionDescriptor(
          x = position.x,
          y = position.y,
          orientation = orientation
        ),
        DroneDescriptor(
          positions = Seq(),
          modules = Seq(),
          hullState = Seq.fill[Byte](size - 1)(2),
          Some(1),
          size = size,
          playerColor = ColorRGB(0, 0, 1)
        )
      )
    }
  }

  def minerals = {
    Seq(
      ModelDescriptor(PositionDescriptor(100, -500, 0), MineralDescriptor(1)),
      ModelDescriptor(PositionDescriptor(130, -520, 0), MineralDescriptor(1)),
      ModelDescriptor(PositionDescriptor(80, -525, 0), MineralDescriptor(1)),
      ModelDescriptor(PositionDescriptor(140, -490, 0), MineralDescriptor(1)),
      ModelDescriptor(PositionDescriptor(95, -550, 0), MineralDescriptor(1)),
      ModelDescriptor(PositionDescriptor(105, -530, 0), MineralDescriptor(2)),
      ModelDescriptor(PositionDescriptor(150, -550, 0), MineralDescriptor(2)),
      ModelDescriptor(PositionDescriptor(180, -500, 0), MineralDescriptor(4))
    )
  }

  def modules = Seq[DroneModuleDescriptor](
    StorageModuleDescriptor(0, EmptyStorage),
    StorageModuleDescriptor(0, EnergyStorage(Set(0, 3, 4, 5))),
    StorageModuleDescriptor(0, MineralStorage),
    EnginesDescriptor(0),
    ShieldGeneratorDescriptor(0),
    ProcessingModuleDescriptor(Seq(0)),
    MissileBatteryDescriptor(0)
  ).zipWithIndex.map {
    case (module, i) =>
      new MockDrone(
        xPos = -250,
        yPos = 100 * i - 200,
        orientation = 0,
        size = 3,
        modules = Seq[DroneModuleDescriptor](module),
        sightRadius = None,
        undamaged = true,
        dontMove = true
      )
  }

  val largeDrone = ModelDescriptor(
    PositionDescriptor(170, 300, 0),
    DroneDescriptor(
      positions = Seq(),
      modules = Seq(
        MissileBatteryDescriptor(0),
        EnginesDescriptor(1),
        StorageModuleDescriptor(2, EnergyStorage((0 to 6).toSet)),
        ShieldGeneratorDescriptor(3),
        MissileBatteryDescriptor(4),
        MissileBatteryDescriptor(5),
        MissileBatteryDescriptor(6)
      ),
      hullState = Seq.fill[Byte](5)(2),
      Some(1),
      6,
      ColorRGB(0, 0, 1)
    )
  )

  val profilePic = ModelDescriptor(
    PositionDescriptor(-400, 0, 1),
    DroneDescriptor(
      positions = Seq(),
      modules = Seq(
        EnginesDescriptor(0),
        MissileBatteryDescriptor(3),
        ShieldGeneratorDescriptor(2),
        StorageModuleDescriptor(1, EnergyStorage())
      ),
      hullState = Seq.fill[Byte](4)(2),
      Some(1),
      5, ColorRGB(0, 0, 1)
    )
  )

  def generateObjects(t: Int): Seq[ModelDescriptor] = {
    hulls ++ minerals :+ largeDrone :+ profilePic
  }

  def spawnObjects(t: Int): Seq[MockObject] = {
    if (t % 100 < 40 && t % 5 == 1) {
      Seq(new MockLaserMissile(100, -300, 0, 25))
    } else {
      Seq()
    }
  }
}

