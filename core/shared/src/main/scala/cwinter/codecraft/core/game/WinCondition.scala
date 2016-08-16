package cwinter.codecraft.core.game

private[codecraft] sealed trait WinCondition
private[codecraft] case object DestroyEnemyMotherships extends WinCondition
private[codecraft] case class LargestFleet(timeout: Int) extends WinCondition


