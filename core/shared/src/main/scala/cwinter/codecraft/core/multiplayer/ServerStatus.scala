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
  timestep: Long,
  startTimestamp: Long
)

