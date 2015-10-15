package cwinter.codecraft.core.api

import cwinter.codecraft.util.maths.ColorRGB

trait Player {
  def id: Int
  private[codecraft] def char: Char
  private[codecraft] def color: ColorRGB
}

private[codecraft] object Player {
  def fromID(id: Int): Player = id match {
    case 0 => BluePlayer
    case 1 => RedPlayer
    case 2 => OrangePlayer
  }
}


object BluePlayer extends Player {
  private[codecraft] def color: ColorRGB = ColorRGB(0, 0, 1)
  private[codecraft] def char = 'B'
  val id = 0
}

object RedPlayer extends Player {
  private[codecraft] def color: ColorRGB = ColorRGB(1, 0, 0)
  private[codecraft] def char = 'R'
  val id = 1
}

object OrangePlayer extends Player {
  private[codecraft] def color: ColorRGB = ColorRGB(1, 0.25f, 0)
  private[codecraft] def char = 'O'
  val id = 2
}



