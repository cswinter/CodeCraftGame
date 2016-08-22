package cwinter.codecraft.core.game

sealed trait WinCondition
case object DestroyEnemyMotherships extends WinCondition
case class LargestFleet(timeout: Int) extends WinCondition


object WinCondition {
  def default: Seq[WinCondition] = Seq(DestroyEnemyMotherships, LargestFleet(15 * 60 * 60))
}

