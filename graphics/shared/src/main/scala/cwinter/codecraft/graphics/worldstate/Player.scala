package cwinter.codecraft.graphics.worldstate

import cwinter.codecraft.util.maths.ColorRGB

trait Player {
  def color: ColorRGB
  def id: Int
  private[codecraft] def char: Char
}

object Player {
  def fromID(id: Int): Player = id match {
    case 0 => BluePlayer
    case 1 => RedPlayer
    case 2 => OrangePlayer
  }
}


object BluePlayer extends Player {
  def color: ColorRGB = ColorRGB(0, 0, 1)
  def id: Int = 0
  private[codecraft] def char = 'B'
}

object RedPlayer extends Player {
  def color: ColorRGB = ColorRGB(1, 0, 0)
  def id: Int = 1
  private[codecraft] def char = 'R'
}

object OrangePlayer extends Player {
  def color: ColorRGB = ColorRGB(1, 0.25f, 0)
  def id: Int = 2
  private[codecraft] def char = 'O'
}



