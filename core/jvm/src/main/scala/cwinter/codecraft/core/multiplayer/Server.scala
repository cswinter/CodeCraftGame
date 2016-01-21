package cwinter.codecraft.core.multiplayer

import akka.actor._
import akka.io.IO
import spray.can.Http
import spray.can.server.UHttp

import scala.concurrent.Await
import scala.concurrent.duration.Duration


object Server {
  def spawnServerInstance(displayGame: Boolean = false): Unit = {
    implicit val system = ActorSystem()

    val server = system.actorOf(MultiplayerServer.props(displayGame), "websocket")

    IO(UHttp) ! Http.Bind(server, "0.0.0.0", 8080)

    system.awaitTermination()
  }

  def main(args: Array[String]): Unit = {
    spawnServerInstance(true)
  }
}


class MultiplayerServer(displayGame: Boolean = false) extends Actor with ActorLogging {
  def receive = {
    // when a new connection comes in we register a WebSocketConnection actor as the per connection handler
    case Http.Connected(remoteAddress, localAddress) =>
      val serverConnection = sender()
      val conn = context.actorOf(MultiplayerGameWorker.props(serverConnection, displayGame))
      serverConnection ! Http.Register(conn)
  }
}

object MultiplayerServer {
  def props(displayGame: Boolean = false) = Props(classOf[MultiplayerServer], displayGame)
}

