package cwinter.codecraft.core.api

import cwinter.codecraft.util.maths.ColorRGB

/** Base trait for all objects representing a player. */
trait Player {
  /** The unique identifier for the player. */
  def id: Int
  private[codecraft] def char: Char
  private[codecraft] def color: ColorRGB
  private[codecraft] def name: String
  private[codecraft] def isObserver: Boolean
}

private[codecraft] object Player {
  def fromID(id: Int): Player = id match {
    case 0 => BluePlayer
    case 1 => RedPlayer
    case 2 => OrangePlayer
    case _ => Observer(id)
  }
}

/** The blue player. */
object BluePlayer extends Player {
  private[codecraft] val color: ColorRGB = ColorRGB(0, 0, 1)
  private[codecraft] val char = 'B'
  private[codecraft] val name = "Blue Player"
  private[codecraft] val isObserver: Boolean = false

  /** Returns `0`. */
  val id = 0
}

/** The red player. */
object RedPlayer extends Player {
  private[codecraft] val color: ColorRGB = ColorRGB(1, 0, 0)
  private[codecraft] val char = 'R'
  private[codecraft] val name = "Red Player"
  private[codecraft] val isObserver: Boolean = false

  /** Returns `1`. */
  val id = 1
}

/** The orange player. */
object OrangePlayer extends Player {
  private[codecraft] val color: ColorRGB = ColorRGB(1, 0.25f, 0)
  private[codecraft] val char = 'O'
  private[codecraft] val name = "Orange Player"
  private[codecraft] val isObserver: Boolean = false

  /** Returns `2`. */
  val id = 2
}

/** Observer */
case class Observer(val id: Int) extends Player {
  private[codecraft] val color: ColorRGB = ColorRGB(1, 0.25f, 0)
  private[codecraft] val char = 'O'
  private[codecraft] val name = "Orange Player"
  private[codecraft] val isObserver: Boolean = false
}

