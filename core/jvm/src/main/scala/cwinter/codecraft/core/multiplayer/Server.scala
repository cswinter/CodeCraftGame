package cwinter.codecraft.core.multiplayer

import akka.actor._
import akka.io.IO
import spray.can.Http
import spray.can.server.UHttp

import scala.concurrent.Await
import scala.concurrent.duration.Duration


object Server {
  def spawnServerInstance(): Unit = {
    implicit val system = ActorSystem()

    val server = system.actorOf(MultiplayerServer.props(), "websocket")

    IO(UHttp) ! Http.Bind(server, "localhost", 8080)

    Await.result(system.whenTerminated, Duration.Inf)
  }

  def main(args: Array[String]): Unit = {
    spawnServerInstance()
  }
}


class MultiplayerServer extends Actor with ActorLogging {
  def receive = {
    // when a new connection comes in we register a WebSocketConnection actor as the per connection handler
    case Http.Connected(remoteAddress, localAddress) =>
      val serverConnection = sender()
      val conn = context.actorOf(MultiplayerGameWorker.props(serverConnection))
      serverConnection ! Http.Register(conn)
  }
}

object MultiplayerServer {
  def props() = Props(classOf[MultiplayerServer])
}

