package cwinter.codecraft.core.multiplayer

import java.nio.ByteBuffer

import org.scalajs.dom.raw._

import scala.scalajs.js.typedarray.TypedArrayBufferOps._
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}

private[core] class JSWebsocketClient(connectionString: String) extends WebsocketClient {
  var ws = Option.empty[WebSocket]

  def connect(): Unit = {
    val ws = new WebSocket(connectionString)
    this.ws = Some(ws)

    ws.onmessage = (event: MessageEvent) => {
      event.data match {
        case blob: Blob =>
          val reader = new FileReader
          reader.addEventListener("loadend", (x: Any) => {
            val buffer = TypedArrayBuffer.wrap(reader.result.asInstanceOf[ArrayBuffer])
            runOnMessageCallbacks(buffer)
          })
          reader.readAsArrayBuffer(blob)
        case _ => throw new Exception("Received message with unexpected encoding.")
      }
    }

    ws.onopen = (event: Event) => {
      runOnOpenCallbacks()
    }
  }

  def sendMessage(message: ByteBuffer): Unit = ws match {
    case Some(s) => s.send(message.arrayBuffer())
    case None => throw new IllegalStateException("Need to connect() before sending messages.")
  }

  def isConnecting: Boolean = ws.forall(_.readyState == 0)
}
