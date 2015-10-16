package cwinter.codecraft.core.api

import cwinter.codecraft.util.maths.ColorRGB

/**
 * Base trait for all objects representing a player.
 */
trait Player {
  /**
   * The unique identifier for the player.
   */
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

/**
 * The blue player.
 */
object BluePlayer extends Player {
  private[codecraft] def color: ColorRGB = ColorRGB(0, 0, 1)
  private[codecraft] def char = 'B'

  /**
   * Returns `0`.
   */
  val id = 0
}

/**
 * The red player.
 */
object RedPlayer extends Player {
  private[codecraft] def color: ColorRGB = ColorRGB(1, 0, 0)
  private[codecraft] def char = 'R'

  /**
   * Returns `1`.
   */
  val id = 1
}

/**
 * The orange player.
 */
object OrangePlayer extends Player {
  private[codecraft] def color: ColorRGB = ColorRGB(1, 0.25f, 0)
  private[codecraft] def char = 'O'

  /**
   * Returns `2`.
   */
  val id = 2
}



