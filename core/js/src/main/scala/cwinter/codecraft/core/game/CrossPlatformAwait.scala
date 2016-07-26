package cwinter.codecraft.core.game

import scala.concurrent.Awaitable
import scala.concurrent.duration.Duration


object CrossPlatformAwait {
  def result[T](awaitable: Awaitable[T], atMost: Duration): T =
    throw new Exception("Trying to Await in JavaScript!")
}

