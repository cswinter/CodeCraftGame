package cwinter.codecraft.core.multiplayer

import java.nio.ByteBuffer


private[core] trait WebsocketClient {
  def onMessage(handler: (WebsocketClient, ByteBuffer) => Unit): WebsocketClient
  def sendMessage(message: ByteBuffer): Unit
}
