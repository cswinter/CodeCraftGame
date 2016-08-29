package cwinter.codecraft.core.multiplayer


case class Status(
  clientWaiting: Boolean,
  runningGames: Int
)

case class DetailedStatus(
  clientWaiting: Boolean,
  connections: Int,
  games: Seq[GameStatus],
  timestamp: Long
)

case class GameStatus(
  closeReason: Option[String],
  fps: Int,
  averageFPS: Int,
  timestep: Long,
  startTimestamp: Long,
  endTimestamp: Option[Long],
  msSinceLastResponse: Int
)

