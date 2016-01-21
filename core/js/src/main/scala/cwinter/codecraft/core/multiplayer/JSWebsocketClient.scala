package cwinter.codecraft.core.multiplayer


import org.scalajs.dom.raw._

class JSWebsocketClient(connectionString: String) extends WebsocketClient {
  val ws = new WebSocket(connectionString)

  def onMessage(handler: (WebsocketClient, String) => Unit): WebsocketClient = {
    ws.onmessage = (event: MessageEvent) => handler(this, event.data.toString)
    this
  }

  def sendMessage(message: String): Unit = {
    ws.send(message)
  }
}

