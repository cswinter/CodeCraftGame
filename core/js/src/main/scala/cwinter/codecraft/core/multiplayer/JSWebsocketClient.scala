package cwinter.codecraft.core.multiplayer


import org.scalajs.dom.raw._

class JSWebsocketClient(connectionString: String) extends WebsocketClient {
  val ws = new WebSocket(connectionString)

  def onMessage(handler: (WebsocketClient, String) => Unit): WebsocketClient = {
    ws.onmessage = (event: MessageEvent) => handler(this, event.data.toString)
    this
  }

  def sendMessage(message: String): Unit = {
    if (isConnecting) {
      assert(ws.onopen == null, "Broken code, previous message is lost.")
      ws.onopen = (event: Event) => ws.send(message)
    } else {
      ws.send(message)
    }
  }

  def isConnecting: Boolean =
    ws.readyState == 0
}

