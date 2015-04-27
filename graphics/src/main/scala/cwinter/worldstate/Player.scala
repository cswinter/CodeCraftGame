package cwinter.worldstate

import cwinter.codinggame.util.maths.ColorRGB

trait Player {
  def color: ColorRGB
}


object BluePlayer extends Player {
  def color: ColorRGB = ColorRGB(0, 0, 1)
}

object RedPlayer extends Player {
  def color: ColorRGB = ColorRGB(1, 0, 0)
}


