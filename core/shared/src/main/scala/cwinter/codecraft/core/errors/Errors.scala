package cwinter.codecraft.core.errors

import cwinter.codecraft.core.api.CodeCraftException
import cwinter.codecraft.graphics.engine.Debug
import cwinter.codecraft.util.maths.{ColorRGB, Vector2}

private[codecraft] class Errors(debug: Debug) {
  private[this] var errorMessages = List.empty[ErrorMessageObject]

  def error(exception: CodeCraftException, position: Vector2): Nothing = {
    println(exception)
    exception.printStackTrace()
    addMessage(exception.getMessage, position, Error)
    throw exception
  }

  def warn(message: String, position: Vector2): Unit = {
    addMessage(message, position, Warning)
  }

  def inform(message: String, position: Vector2): Unit = {
    addMessage(message, position, Information)
  }

  def addMessage(message: String, position: Vector2, errorLevel: ErrorLevel): Unit = {
    errorMessages ::= new ErrorMessageObject(message, errorLevel, position)
  }

  def updateMessages(): Unit = {
    errorMessages =
      for (
        m <- errorMessages
        if !m.hasFaded
      ) yield {
        m.update()
        debug.drawText(m.model)
        m
      }
  }
}


private[codecraft] sealed trait ErrorLevel {
  val color: ColorRGB
  val messagePrefix: String
}

private[codecraft] case object Error extends ErrorLevel {
  val color = ColorRGB(1, 0, 0)
  val messagePrefix = "Error: "
}

private[codecraft] case object Warning extends ErrorLevel {
  val color = ColorRGB(1, 0.5f, 0)
  val messagePrefix = "Warning: "
}

private[codecraft] case object Information extends ErrorLevel {
  val color = ColorRGB(0, 0, 1)
  val messagePrefix = "Note: "
}

