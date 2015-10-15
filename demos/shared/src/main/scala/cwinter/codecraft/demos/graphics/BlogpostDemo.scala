package cwinter.codecraft.demos.graphics

import cwinter.codecraft.core.api
import cwinter.codecraft.graphics.worldstate._
import cwinter.codecraft.util.maths.{VertexXY, Geometry}

private[graphics] object BlogpostDemo {
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

      DroneDescriptor(
        identifier = i,
        xPos = position.x,
        yPos = position.y,
        orientation = orientation,
        positions = Seq(),
        modules = Seq(),
        hullState = Seq.fill[Byte](size - 1)(2),
        Some(1),
        size = size,
        player = api.BluePlayer
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

  def modules = Seq[DroneModuleDescriptor](
    StorageModuleDescriptor(Seq(0), EmptyStorage),
    StorageModuleDescriptor(Seq(0), EnergyStorage(Set(0, 3, 4, 5))),
    StorageModuleDescriptor(Seq(0), MineralStorage),
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

  val largeDrone = DroneDescriptor(
    identifier = 1, xPos = 170, yPos = 300, orientation = 0,
    positions = Seq(),
    modules = Seq(
      MissileBatteryDescriptor(0),
      EnginesDescriptor(1),
      StorageModuleDescriptor(Seq(2), EnergyStorage((0 to 6).toSet)),
      ShieldGeneratorDescriptor(3),
      MissileBatteryDescriptor(4),
      MissileBatteryDescriptor(5),
      MissileBatteryDescriptor(6)
    ),
    hullState = Seq.fill[Byte](5)(2),
    Some(1),
    6,
    api.BluePlayer
  )

  val profilePic = DroneDescriptor(
    identifier = 9000, xPos = -400, yPos = 0, orientation = 1,
    positions = Seq(),
    modules = Seq(
      EnginesDescriptor(0),
      MissileBatteryDescriptor(3),
      ShieldGeneratorDescriptor(2),
      StorageModuleDescriptor(Seq(1), EnergyStorage())
    ),
    hullState = Seq.fill[Byte](4)(2),
    Some(1),
    5, api.BluePlayer
  )

  def generateObjects(t: Int): Seq[WorldObjectDescriptor] = {
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
