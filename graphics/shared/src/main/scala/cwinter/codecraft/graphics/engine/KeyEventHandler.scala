package cwinter.codecraft.graphics.engine

import cwinter.codecraft.graphics.worldstate.Simulator

class KeyEventHandler(
  val gameWorld: Simulator,
  val camera: Camera2D
) {
  final val moveSpeed = 100
  final val zoomSpeed = 0.2f

  def keypress(key: Key): Unit = key match {
    case LeftArrow | Letter('a') => camera.x -= moveSpeed * camera.zoomFactor
    case RightArrow | Letter('d') => camera.x += moveSpeed * camera.zoomFactor
    case UpArrow | Letter('w') => camera.y += moveSpeed * camera.zoomFactor
    case DownArrow | Letter('s') => camera.y -= moveSpeed * camera.zoomFactor
    case PageUp | Letter('q') => camera.zoom -= zoomSpeed
    case PageDown | Letter('e') => camera.zoom += zoomSpeed
    case Letter('r') =>
      if (gameWorld.framerateTarget >= 30) gameWorld.framerateTarget -= 10
      else if (gameWorld.framerateTarget >= 15) gameWorld.framerateTarget -= 5
      else if (gameWorld.framerateTarget > 1) gameWorld.framerateTarget -= 1
    case Letter('f') =>
      if (gameWorld.framerateTarget < 10) gameWorld.framerateTarget += 1
      else if (gameWorld.framerateTarget < 30) gameWorld.framerateTarget += 5
      else gameWorld.framerateTarget += 10
    case Letter(' ') => gameWorld.togglePause()
    case Letter('p') =>
      if (gameWorld.framerateTarget == 5) gameWorld.framerateTarget = 30
      else gameWorld.framerateTarget = 5
    case Letter(x) => gameWorld.handleKeypress(x)
    case _ =>
  }
}

sealed trait Key
case class Letter(char: Char) extends Key
case object LeftArrow extends Key
case object RightArrow extends Key
case object UpArrow extends Key
case object DownArrow extends Key
case object PageUp extends Key
case object PageDown extends Key

