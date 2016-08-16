package cwinter.codecraft.core.multiplayer


import java.nio.ByteBuffer

import org.scalajs.dom.raw._

import scala.scalajs.js.typedarray.TypedArrayBufferOps._
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}


private[core] class JSWebsocketClient(connectionString: String) extends WebsocketClient {
  val ws = new WebSocket(connectionString)

  def onMessage(handler: (WebsocketClient, ByteBuffer) => Unit): WebsocketClient = {
    ws.onmessage = (event: MessageEvent) => {
      event.data match {
        case blob: Blob =>
          val reader = new FileReader
          reader.addEventListener("loadend", (x: Any) => {
            val buffer = TypedArrayBuffer.wrap(reader.result.asInstanceOf[ArrayBuffer])
            handler(this, buffer)
          })
          reader.readAsArrayBuffer(blob)
        case _ => throw new Exception("Received message with unexpected encoding.")
      }
    }
    this
  }

  def sendMessage(message: ByteBuffer): Unit = {
    if (isConnecting) {
      assert(ws.onopen == null, "Broken code, previous message is lost.")
      ws.onopen = (event: Event) => ws.send(message.arrayBuffer())
    } else { ws.send(message.arrayBuffer()) }
  }

  def isConnecting: Boolean =
    ws.readyState == 0
}

