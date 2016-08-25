package cwinter.codecraft.core.multiplayer


case class Status(
  clientWaiting: Boolean,
  runningGames: Int
)

case class DetailedStatus(
  clientWaiting: Boolean,
  games: Seq[GameStatus]
)

case class GameStatus(
  closeReason: Option[String],
  fps: Int,
  timestep: Long
)

