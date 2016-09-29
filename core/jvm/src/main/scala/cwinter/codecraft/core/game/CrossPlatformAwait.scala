package cwinter.codecraft.core.game

import scala.concurrent.{Await, Awaitable}
import scala.concurrent.duration.Duration


private[codecraft] object CrossPlatformAwait {
  def result[T](awaitable: Awaitable[T], atMost: Duration): T =
    Await.result(awaitable, atMost)
}

