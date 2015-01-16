package robowars.worldstate


trait WorldObject {
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

  size: Int
) extends WorldObject


case class MineralObject(
  identifier: Int,
  xPos: Float,
  yPos: Float,
  orientation: Float,

  size: Int
) extends WorldObject
