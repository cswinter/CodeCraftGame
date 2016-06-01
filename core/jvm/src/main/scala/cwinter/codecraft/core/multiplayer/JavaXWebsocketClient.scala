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
  private[this] var userSession: Session = null
  private[this] var messageHandler: (WebsocketClient, ByteBuffer) => Unit = null
  private[this] var parts: List[Array[Byte]] = List.empty


  def onMessage(handler: (WebsocketClient, ByteBuffer) => Unit): WebsocketClient = {
    val container = ContainerProvider.getWebSocketContainer
    container.connectToServer(this, new URI(uri))
    messageHandler = handler
    this
  }

  def sendMessage(message: ByteBuffer): Unit = {
    assert(messageHandler != null, "You must assign a message handler using `onMessage` before calling sendMessage.")
    this.userSession.getAsyncRemote.sendBinary(message)
  }

  @OnOpen
  def onOpen(userSession: Session): Unit =
    this.userSession = userSession

  @OnClose
  def onClose(userSession: Session, reason: CloseReason): Unit =
    this.userSession = null

  @OnMessage
  def _onMessage(message: String): Unit =
    println(s"Websocket recieved unexpected string message $message.")

  @OnMessage
  def _onMessage(message: Array[Byte], last: Boolean, session: Session): Unit = {
    parts ::= message
    if (last) {
      messageHandler(this, combineParts(parts))
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
    userSession.close()
    _closed = true
  }
}

