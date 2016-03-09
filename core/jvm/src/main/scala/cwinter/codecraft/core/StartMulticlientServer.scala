package cwinter.codecraft.core

private[core] object StartMulticlientServer {
  def main(args: Array[String]): Unit = {
    multiplayer.Server.spawnServerInstance2()
  }
}
