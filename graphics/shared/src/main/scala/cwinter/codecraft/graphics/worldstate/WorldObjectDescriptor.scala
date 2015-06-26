package cwinter.codecraft.graphics.worldstate

import cwinter.codecraft.util.maths
import cwinter.codecraft.util.maths.{ColorRGB, Float0To1, VertexXY}



sealed trait WorldObjectDescriptor {
  val identifier: Int
  val xPos: Float
  val yPos: Float
  val orientation: Float
}

case class DroneDescriptor(
  identifier: Int,
  xPos: Float,
  yPos: Float,
  orientation: Float,
  positions: Seq[(Float, Float, Float)],
  modules: Seq[DroneModuleDescriptor],
  hullState: Seq[Byte],
  shieldState: Option[Float],
  size: Int,

  player: Player,
  constructionState: Option[Float0To1] = None,

  sightRadius: Option[Int] = None,
  inSight: Option[Iterable[(Float, Float)]] = None
) extends WorldObjectDescriptor {
  assert(hullState.size == size - 1)
  assert(!xPos.toDouble.isNaN)
  assert(!yPos.toDouble.isNaN)
  assert(!orientation.toDouble.isNaN)
}


sealed trait DroneModuleDescriptor

case class StorageModuleDescriptor(positions: Seq[Int], contents: StorageModuleContents, mergingProgress: Option[Float] = None) extends DroneModuleDescriptor {
  for (x <- mergingProgress) {
    assert(x >= 0)
    assert(x <= 1)
  }
}

sealed trait StorageModuleContents
case object EmptyStorage extends StorageModuleContents
case object MineralStorage extends StorageModuleContents
case class EnergyStorage(filledPositions: Set[Int] = Set(0, 1, 2, 3, 4, 5, 6)) extends StorageModuleContents

case class EnginesDescriptor(position: Int) extends DroneModuleDescriptor
case class ProcessingModuleDescriptor(positions: Seq[Int], mergingProgress: Option[Int] = None) extends DroneModuleDescriptor
case class ShieldGeneratorDescriptor(position: Int) extends DroneModuleDescriptor
case class MissileBatteryDescriptor(position: Int, n: Int = 3) extends DroneModuleDescriptor
case class ManipulatorDescriptor(position: Int) extends DroneModuleDescriptor

case class EnergyGlobeDescriptor(
  xPos: Float,
  yPos: Float,
  fade: Float = 1
) extends WorldObjectDescriptor {
  assert(fade >= 0)
  assert(fade <= 1)
  val identifier: Int = 0
  val orientation: Float = 0
}

case class ManipulatorArm(player: Player, x1: Float, y1: Float, x2: Float, y2: Float)
  extends WorldObjectDescriptor {
  val identifier: Int = 0
  val xPos: Float = 0
  val yPos: Float = 0
  val orientation: Float = 0
}

case class MineralDescriptor(
  identifier: Int,
  xPos: Float,
  yPos: Float,
  orientation: Float,

  size: Int,
  harvested: Boolean = false,
  harvestingProgress: Option[Float0To1] = None
) extends WorldObjectDescriptor


case class LightFlashDescriptor(
  identifier: Int,
  xPos: Float,
  yPos: Float,
  stage: Float
) extends WorldObjectDescriptor {
  val orientation = 0.0f
}


case class HomingMissileDescriptor(
  identifier: Int,
  positions: Seq[(Float, Float)],
  maxPos: Int,
  player: Player
) extends WorldObjectDescriptor {
  val orientation = 0.0f
  val xPos = 0.0f
  val yPos = 0.0f
}

case class TestingObject(time: Int) extends WorldObjectDescriptor {
  val identifier = -1
  val xPos = 0f
  val yPos = 0f
  val orientation = 0f
}

case class DrawCircle(
  identifier: Int,
  xPos: Float,
  yPos: Float,
  radius: Float
) extends WorldObjectDescriptor {
  val orientation = 0.0f
}

case class DrawCircleOutline(
  xPos: Float,
  yPos: Float,
  radius: Float,
  color: ColorRGB = ColorRGB(1, 1, 1)
) extends WorldObjectDescriptor {
  val identifier = -1
  val orientation = 0.0f
}

case class DrawRectangle(
  identifier: Int,
  bounds: maths.Rectangle
) extends WorldObjectDescriptor {
  val orientation = 0.0f
  val xPos: Float = 0
  val yPos: Float = 0
}
