package cwinter.codecraft.core.multiplayer

import java.nio.ByteBuffer

import akka.actor.{ActorRef, Props}
import akka.util.ByteString
import spray.can.websocket
import spray.can.websocket.FrameCommandFailed
import spray.can.websocket.frame.{CloseFrame, BinaryFrame, TextFrame}
import spray.http.HttpRequest

import scala.language.postfixOps

private[core] trait WebsocketWorker {
  private[this] var websocketActor: Option[ActorRef] = None

  def receive(message: String): Unit

  def receiveBytes(bytes: ByteBuffer): Unit

  def installActorRef(actorRef: ActorRef): Unit = {
    websocketActor = Some(actorRef)
  }

  def send(message: ByteBuffer): Unit = websocketActor match {
    case Some(actor) => actor ! WebsocketActor.Send(message)
    case None =>
      throw new Exception(
        "WebsocketWorker must be installed with a RemoteWebsocketClient before calling send.")
  }

  def closeConnection(): Unit = websocketActor.foreach(_ ! WebsocketActor.Close)
}

private[core] class WebsocketActor(
  val serverConnection: ActorRef,
  val websocketWorker: WebsocketWorker
) extends websocket.WebSocketServerWorker {
  import WebsocketActor._

  websocketWorker.installActorRef(this.self)

  override def receive = handshaking orElse closeLogic

  def businessLogic: Receive = {
    case BinaryFrame(bytes) =>
      websocketWorker.receiveBytes(bytes.asByteBuffer)
    case TextFrame(text) =>
      val decoded = text.decodeString("UTF-8")
      websocketWorker.receive(decoded)
    case Send(message) =>
      send(BinaryFrame(ByteString.fromByteBuffer(message)))
    case Close =>
      send(CloseFrame())
      context.stop(self)
    case x: FrameCommandFailed =>
      log.error("frame command failed", x)
      context.stop(self)
      throw new Exception(s"Frame command failed: $x")
    case x: HttpRequest =>
      context.stop(self)
      throw new Exception("Unexpected HttpRequest")
  }

}

private[core] object WebsocketActor {
  def props(serverConnection: ActorRef, worker: WebsocketWorker) =
    Props(classOf[WebsocketActor], serverConnection, worker)

  case class Send(message: ByteBuffer)
  case object Close
}
