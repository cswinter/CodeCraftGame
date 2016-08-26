package cwinter.codecraft.core.multiplayer

object CrossPlatformWebsocket {
  def create(connectionString: String): WebsocketClient =
    new JSWebsocketClient(connectionString)
}
