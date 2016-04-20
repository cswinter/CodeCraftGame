package cwinter.codecraft.graphics.engine

import cwinter.codecraft.graphics.models.{MineralModelBuilder, DroneModelBuilder}
import cwinter.codecraft.graphics.worldstate._
import cwinter.codecraft.util.maths.{Vector2, Rectangle, ColorRGB}


private[codecraft] object AsciiVisualizer {
  final val ratio = 75
  final val margin = ratio * 2

  def show(worldState: Iterable[ModelDescriptor[_]], bounds: Rectangle): String = {
    def gridpoint(pos: Vector2): (Int, Int) = (
      ((pos.x - bounds.xMin + margin) / ratio).toInt,
      ((pos.y - bounds.yMin + margin) / ratio).toInt
      )

    val width = ((bounds.width + 2 * margin) / ratio).toInt
    val height = ((bounds.height + 2 * margin) / ratio).toInt
    val image = Array.fill[Char](height, width)(' ')

    for (obj <- worldState) obj.objectDescriptor match {
      case d: DroneModelBuilder =>
        val (x, y) = gridpoint(Vector2(obj.position.x, obj.position.y))
        image(y)(x) = toChar(d.playerColor)
      case m: MineralModelBuilder =>
        val (x, y) = gridpoint(Vector2(obj.position.x, obj.position.y))
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
