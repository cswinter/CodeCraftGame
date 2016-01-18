package cwinter.codecraft.core.multiplayer

import java.net.URI
import javax.websocket._


// The implementation of the websocket API appears to make use of reflection in a very brittle fashion.
// Things that may cause a crash:
// - this class has any abstract member functions
// - any of the (annotated) api functions are private
@ClientEndpoint
class WebsocketClient(uri: String) {
  private[this] var _closed = false
  def isClosed: Boolean = _closed
  private[this] var userSession: Session = null
  private[this] var messageHandler: (WebsocketClient, String) => Unit = null


  def onMessage(handler: (WebsocketClient, String) => Unit): WebsocketClient = {
    val container = ContainerProvider.getWebSocketContainer
    container.connectToServer(this, new URI(uri))
    messageHandler = handler
    this
  }

  def sendMessage(message: String): Unit = {
    assert(messageHandler != null, "You must assign a message handler using `onMessage` before calling sendMessage.")
    this.userSession.getAsyncRemote.sendText(message)
  }

  @OnOpen
  def onOpen(userSession: Session): Unit =
    this.userSession = userSession

  @OnClose
  def onClose(userSession: Session, reason: CloseReason): Unit =
    this.userSession = null

  @OnMessage
  def _onMessage(message: String): Unit =
    messageHandler(this, message)

  def close(): Unit = {
    userSession.close()
    _closed = true
  }
}

