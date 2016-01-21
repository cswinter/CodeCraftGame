package cwinter.codecraft.core.multiplayer


trait WebsocketClient {
  def onMessage(handler: (WebsocketClient, String) => Unit): WebsocketClient
  def sendMessage(message: String): Unit
}
