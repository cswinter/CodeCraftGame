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

      new MockDrone(
        position.x,
        position.y,
        orientation,
        size = size,
        modules = Seq(),
        undamaged = true
      ).state()
    }
  }

  def minerals =
    for ((size, x, y, orientation) <- Seq(
      (1, 100, -500, 0),
      (1, 130, -520, 0),
      (1, 80, -525, 0),
      (1, 140, -490, 0),
      (1, 95, -550, 0),
      (2, 105, -530, 0),
      (2, 150, -550, 0),
      (4, 180, -500, 0)
    )) yield ModelDescriptor(NullPositionDescriptor, MineralDescriptor(size, x, y, orientation))

  def modules = Seq[DroneModuleDescriptor](
    StorageModuleDescriptor(0, EmptyStorage),
    StorageModuleDescriptor(0, EnergyStorage(Set(0, 3, 4, 5))),
    StorageModuleDescriptor(0, MineralStorage),
    EnginesDescriptor(0),
    ShieldGeneratorDescriptor(0),
    MissileBatteryDescriptor(0)
  ).zipWithIndex.map {
    case (module, i) =>
      new MockDrone(
        xPos = -250,
        yPos = 100 * i - 200,
        orientation = 0,
        size = 3,
        modules = Seq[DroneModuleDescriptor](module),
        undamaged = true,
        dontMove = true
      )
  }

  val largeDrone = new MockDrone(
    170, 300, 0,
    size = 6,
    modules = Seq(
      MissileBatteryDescriptor(0),
      EnginesDescriptor(1),
      StorageModuleDescriptor(2, EnergyStorage((0 to 6).toSet)),
      ShieldGeneratorDescriptor(3),
      MissileBatteryDescriptor(4),
      MissileBatteryDescriptor(5),
      MissileBatteryDescriptor(6)
    ),
    true
  ).state()

  val profilePic = new MockDrone(
    -400, 0, 1,
    size = 5,
    modules = Seq(
      EnginesDescriptor(0),
      MissileBatteryDescriptor(3),
      ShieldGeneratorDescriptor(2),
      StorageModuleDescriptor(1, EnergyStorage())
    ),
    true
  ).state()

  def generateObjects(t: Int): Seq[ModelDescriptor[_]] = {
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

