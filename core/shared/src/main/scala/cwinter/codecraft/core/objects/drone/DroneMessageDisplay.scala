package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.errors.Error
import cwinter.codecraft.graphics.engine.TextModel
import cwinter.codecraft.util.maths.{ColorRGB, ColorRGBA}


private[core] trait DroneMessageDisplay { self: DroneImpl =>
  import DroneMessageDisplay.MessageCooldown
  private[this] var messageCooldown = 0
  private[this] var debugMessage = Option.empty[String]
  private[this] var debugTexts = List.empty[TextModel]

  def error(message: String): Unit = {
    if (messageCooldown <= 0 && context.settings.allowMessages) {
      messageCooldown = MessageCooldown
      context.errors.addMessage(message, position, Error)
    }
  }

  def warn(message: String): Unit = {
    if (messageCooldown <= 0 && context.settings.allowMessages) {
      messageCooldown = MessageCooldown
      context.errors.warn(message, position)
    }
  }

  def inform(message: String): Unit = {
    if (messageCooldown <= 0 && context.settings.allowMessages) {
      messageCooldown = MessageCooldown
      context.errors.inform(message, position)
    }
  }

  def showText(message: String): Unit = {
    debugMessage = Some(debugMessage match {
      case Some(text) => s"$text;$message"
      case None => message
    })
  }

  def showText(message: String, xPos: Float, yPos: Float): Unit =
    debugTexts ::= TextModel(message, xPos, yPos, ColorRGBA(context.player.color, 1f))


  protected def resetMessageDisplay(): Unit = {
    debugMessage = None
    debugTexts = List.empty[TextModel]
  }

  protected def displayMessages(): Unit = {
    messageCooldown -= 1
    for (text <- debugTexts) context.debug.drawText(text)
    for (message <- debugMessage)
      yield context.debug.drawText(
        message, position.x, position.y, ColorRGBA(ColorRGB(1, 1, 1) - context.player.color, 1))
  }
}

private[core] object DroneMessageDisplay {
  val MessageCooldown = 30
}
