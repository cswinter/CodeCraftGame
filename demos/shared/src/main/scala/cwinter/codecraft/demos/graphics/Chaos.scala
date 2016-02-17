package cwinter.codecraft.demos.graphics

import cwinter.codecraft.graphics.worldstate._
import cwinter.codecraft.util.maths.{ColorRGB, Float0To1}

private[graphics] object Chaos {
  val sightRadius = Some(250)
  val north = (math.Pi / 2).toFloat
  val customDrones = Seq(
    new MockDrone(
      xPos = 200,
      yPos = 0,
      orientation = north,
      modules = Seq(
        StorageModuleDescriptor(0, EmptyStorage),
        StorageModuleDescriptor(1, EmptyStorage),
        StorageModuleDescriptor(2, EmptyStorage),
        EnginesDescriptor(3)
      ),
      size = 5,
      sightRadius = sightRadius
    )
  )

  def droneConstruction(time: Int): ModelDescriptor = {
    ModelDescriptor(
      PositionDescriptor(
        x = 100,
        y = 100,
        orientation = 2
      ),
      DroneDescriptor(
        positions = Seq(),
        modules = Seq(
          EnginesDescriptor(0),
          MissileBatteryDescriptor(1, 0),
          StorageModuleDescriptor(2, EmptyStorage),
          ShieldGeneratorDescriptor(3),
          StorageModuleDescriptor(4, EmptyStorage),
          MissileBatteryDescriptor(5, 3),
          MissileBatteryDescriptor(6, 3)
        ),
        hullState = Seq[Byte](2, 2, 2, 2, 2),
        shieldState = Some(1),
        size = 6,
        playerColor = ColorRGB(0, 0, 1),
        constructionState = Some(Float0To1(math.max(time / 5000f, 1))),
        None, None
      )
    )
  }


  def main(args: Array[String]): Unit = {
    val s = new GraphicsSimulator(
      customDrones,
      t => Seq(droneConstruction(t)),
      t => Seq(),
      sightRadius = sightRadius,
      nRandomDrones = 10,
      nRandomMinerals = 20,
      spawnProjectiles = true
    )
    s.start()
  }
}
