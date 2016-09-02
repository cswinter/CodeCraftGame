package cwinter.codecraft.core.multiplayer

import java.net.URI
import java.nio.ByteBuffer
import javax.websocket._

// The implementation of the websocket API appears to make use of reflection in a very brittle fashion.
// Things that may cause a crash:
// - this class has any abstract member functions
// - any of the (annotated) api functions are private
@ClientEndpoint
private[core] class JavaXWebsocketClient(uri: String) extends WebsocketClient {
  private[this] var _closed = false
  def isClosed: Boolean = _closed
  private[this] var userSession = Option.empty[Session]
  private[this] var parts: List[Array[Byte]] = List.empty

  def connect(): Unit = {
    val container = ContainerProvider.getWebSocketContainer
    container.connectToServer(this, new URI(uri))
  }

  def sendMessage(message: ByteBuffer): Unit = userSession match {
    case None =>
      throw new IllegalStateException(
        if (_closed) s"Trying to send message on websocket that has been closed."
        else s"Trying to send message on websocket that has not established a connection.")
    case Some(us) => us.getAsyncRemote.sendBinary(message)
  }

  @OnOpen
  def onOpen(userSession: Session): Unit = {
    this.userSession = Some(userSession)
    runOnOpenCallbacks()
  }

  @OnClose
  def onClose(userSession: Session, reason: CloseReason): Unit = {
    this.userSession = None
    _closed = true
  }

  @OnMessage
  def _onMessage(message: String): Unit =
    println(s"Websocket recieved unexpected string message $message.")

  @OnMessage
  def _onMessage(message: Array[Byte], last: Boolean, session: Session): Unit = {
    parts ::= message
    if (last) {
      runOnMessageCallbacks(combineParts(parts))
      parts = List.empty
    }
  }

  private def combineParts(parts: List[Array[Byte]]): ByteBuffer = parts match {
    case List(part) => ByteBuffer.wrap(part)
    case _ =>
      assert(parts.nonEmpty)
      val totalBytes = parts.foldLeft(0)(_ + _.length)
      val buffer = ByteBuffer.allocate(totalBytes)
      for (part <- parts.reverse) buffer.put(part)
      buffer
  }

  def close(): Unit = {
    userSession.foreach(_.close())
    _closed = true
  }
}
