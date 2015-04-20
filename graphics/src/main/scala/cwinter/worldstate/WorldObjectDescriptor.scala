package cwinter.worldstate


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
  modules: Seq[DroneModule],
  hullState: Seq[Byte],
  size: Int,

  constructionState: Int = -1,
  processingModuleMergers: Seq[Int] = Seq(),
  storageModuleMergers: Seq[Int] = Seq(),

  sightRadius: Option[Int],
  inSight: Option[Iterable[(Float, Float)]]
) extends WorldObjectDescriptor {
  assert(hullState.size == size - 1)
}


sealed trait DroneModule

case class StorageModule(positions: Seq[Int], resourceCount: Int, mergingProgress: Int = 0) extends DroneModule {
  assert(resourceCount >= -1)
  assert(resourceCount <= 7)
}
case class Engines(position: Int, t: Int = 0) extends DroneModule
case class ProcessingModule(positions: Seq[Int], t: Int = 0, mergingProgress: Int = 0) extends DroneModule
case class ShieldGenerator(position: Int) extends DroneModule
case class Lasers(position: Int, n: Int = 3) extends DroneModule


case class MineralDescriptor(
  identifier: Int,
  xPos: Float,
  yPos: Float,
  orientation: Float,

  size: Int
) extends WorldObjectDescriptor


case class LightFlashDescriptor(
  identifier: Int,
  xPos: Float,
  yPos: Float,
  stage: Float
) extends WorldObjectDescriptor {
  val orientation = 0.0f
}


case class LaserMissileDescriptor(
  identifier: Int,
  positions: Seq[(Float, Float)]
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

case class Circle(
  identifier: Int,
  xPos: Float,
  yPos: Float,
  radius: Float
) extends WorldObjectDescriptor {
  val orientation = 0.0f
}

case class Rectangle(
  identifier: Int,
  bounds: cwinter.codinggame.maths.Rectangle
) extends WorldObjectDescriptor {
  val orientation = 0.0f
  val xPos: Float = 0
  val yPos: Float = 0
}
