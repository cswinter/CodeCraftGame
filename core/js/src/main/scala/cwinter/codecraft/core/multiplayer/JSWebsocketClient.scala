package cwinter.codecraft.core.multiplayer


import java.nio.ByteBuffer

import org.scalajs.dom.raw._

private[core] class JSWebsocketClient(connectionString: String) extends WebsocketClient {
  val ws = new WebSocket(connectionString)

  def onMessage(handler: (WebsocketClient, ByteBuffer) => Unit): WebsocketClient = {
    ws.onmessage = (event: MessageEvent) => throw new Exception(event.data.toString)
    // TODO: implement (need to know how to get ByteBuffer from event)
    //handler(this, event.data.toString)
    this
  }

  def sendMessage(message: ByteBuffer): Unit = ???
  /*{
    if (isConnecting) {
      assert(ws.onopen == null, "Broken code, previous message is lost.")
      ws.onopen = (event: Event) => ws.send(message)
    } else {
      ws.send(message)
    }
  }*/

  def isConnecting: Boolean =
    ws.readyState == 0
}

