package cwinter.codecraft.core.multiplayer

import akka.actor._
import akka.io.IO
import spray.can.server.UHttp
import spray.can.websocket.FrameCommandFailed
import spray.can.websocket.frame.{BinaryFrame, TextFrame}
import spray.can.{Http, websocket}
import spray.http.HttpRequest

import scala.concurrent.Await
import scala.concurrent.duration.Duration


object Server extends App {
  final case class Push(msg: String)
  final case class PushToChildren(msg: String)

  object WebSocketServer {
    def props() = Props(classOf[WebSocketServer])
  }
  class WebSocketServer extends Actor with ActorLogging {
    def receive = {
      // when a new connection comes in we register a WebSocketConnection actor as the per connection handler
      case Http.Connected(remoteAddress, localAddress) =>
        val serverConnection = sender()
        val conn = context.actorOf(WebSocketWorker.props(serverConnection))
        serverConnection ! Http.Register(conn)
      case PushToChildren(msg: String) =>
        val children = context.children
        println("pushing to all children : " + msg)
        children.foreach(ref => ref ! Push(msg))
    }
  }

  object WebSocketWorker {
    def props(serverConnection: ActorRef) = Props(classOf[WebSocketWorker], serverConnection)
  }
  class WebSocketWorker(val serverConnection: ActorRef)
  extends websocket.WebSocketServerWorker {
    override def receive = handshaking orElse closeLogic

    def businessLogic: Receive = {
      // just bounce frames back for Autobahn testsuite
      case x @(_: BinaryFrame | _: TextFrame) =>
        sender() ! x

      case Push(msg) => send(TextFrame(msg))

      case x: FrameCommandFailed =>
        log.error("frame command failed", x)

      case x: HttpRequest => // do something
    }
  }

  def doMain(): Unit = {
    implicit val system = ActorSystem()

    val server = system.actorOf(WebSocketServer.props(), "websocket")

    IO(UHttp) ! Http.Bind(server, "localhost", 8080)

    var msg: String = null
    do {
      msg = io.StdIn.readLine("Give a message and hit ENTER to push message to the children ...\n")
      server ! PushToChildren(msg)
    } while (msg != "stop")

    // never reached ...
    system.terminate()
    Await.result(system.whenTerminated, Duration.Inf)
  }

  // because otherwise we get an ambiguous implicit if doMain is inlined
  doMain()
}

