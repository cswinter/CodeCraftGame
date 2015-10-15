package cwinter.codecraft.graphics.engine

import cwinter.codecraft.graphics.worldstate.{HomingMissileDescriptor, MineralDescriptor, DroneDescriptor, WorldObjectDescriptor}
import cwinter.codecraft.util.maths.{Vector2, Rectangle, ColorRGB}

private[graphics] object AsciiVisualizer {
  final val ratio = 75
  final val margin = ratio * 2

  def show(worldState: Iterable[WorldObjectDescriptor], bounds: Rectangle): String = {
    def gridpoint(pos: Vector2): (Int, Int) = (
      ((pos.x - bounds.xMin + margin) / ratio).toInt,
      ((pos.y - bounds.yMin + margin) / ratio).toInt
      )

    val width = ((bounds.width + 2 * margin) / ratio).toInt
    val height = ((bounds.height + 2 * margin) / ratio).toInt
    val image = Array.fill[Char](height, width)(' ')

    for (obj <- worldState) obj match {
      case d: DroneDescriptor =>
        val (x, y) = gridpoint(Vector2(d.xPos, d.yPos))
        image(y)(x) = toChar(d.playerColor)
      case m: MineralDescriptor =>
        val (x, y) = gridpoint(Vector2(m.xPos, m.yPos))
        image(y)(x) = '*'
      case l: HomingMissileDescriptor =>
        val (x, y) = gridpoint(Vector2(l.positions.head._1, l.positions.head._2))
        image(y)(x) = '.'
      case _ =>
    }

    image.map(_.foldLeft("")(_ + _)).mkString("\n")
  }


  private def toChar(color: ColorRGB): Char = {
    if (color.b > 0) 'B'
    else if (color.r > 0 && color.g > 0) 'O'
    else 'R'
  }
}
