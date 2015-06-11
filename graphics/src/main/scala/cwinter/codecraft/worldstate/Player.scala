package cwinter.codecraft.worldstate

import cwinter.codecraft.util.maths.ColorRGB

trait Player {
  def color: ColorRGB
  def id: Int
}


object BluePlayer extends Player {
  def color: ColorRGB = ColorRGB(0, 0, 1)
  def id: Int = 0
}

object RedPlayer extends Player {
  def color: ColorRGB = ColorRGB(1, 0, 0)
  def id: Int = 1
}

object OrangePlayer extends Player {
  def color: ColorRGB = ColorRGB(1, 0.25f, 0)
  def id: Int = 2
}



