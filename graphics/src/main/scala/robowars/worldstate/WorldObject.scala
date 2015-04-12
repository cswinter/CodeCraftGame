package robowars.worldstate


sealed trait WorldObject {
  val identifier: Int
  val xPos: Float
  val yPos: Float
  val orientation: Float
}

case class RobotObject(
  identifier: Int,
  xPos: Float,
  yPos: Float,
  orientation: Float,
  positions: Seq[(Float, Float, Float)],
  modules: Seq[RobotModule],
  hullState: Seq[Byte],
  size: Int,

  constructionState: Int = -1,
  processingModuleMergers: Seq[Int] = Seq(),
  storageModuleMergers: Seq[Int] = Seq(),

  sightRadius: Option[Int],
  inSight: Option[Iterable[(Float, Float)]]
) extends WorldObject {
  assert(hullState.size == size - 1)
}


sealed trait RobotModule

case class StorageModule(positions: Seq[Int], resourceCount: Int, mergingProgress: Int = 0) extends RobotModule {
  assert(resourceCount >= -1)
  assert(resourceCount <= 7)
}
case class Engines(position: Int, t: Int = 0) extends RobotModule
case class ProcessingModule(positions: Seq[Int], t: Int = 0, mergingProgress: Int = 0) extends RobotModule
case class ShieldGenerator(position: Int) extends RobotModule
case class Lasers(position: Int, n: Int = 3) extends RobotModule


case class MineralObject(
  identifier: Int,
  xPos: Float,
  yPos: Float,
  orientation: Float,

  size: Int
) extends WorldObject


case class LightFlash(
  identifier: Int,
  xPos: Float,
  yPos: Float,
  stage: Float
) extends WorldObject {
  val orientation = 0.0f
}


case class LaserMissile(
  identifier: Int,
  positions: Seq[(Float, Float)]
) extends WorldObject {
  val orientation = 0.0f
  val xPos = 0.0f
  val yPos = 0.0f
}

case class TestingObject(time: Int) extends WorldObject {
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
) extends WorldObject {
  val orientation = 0.0f
}
