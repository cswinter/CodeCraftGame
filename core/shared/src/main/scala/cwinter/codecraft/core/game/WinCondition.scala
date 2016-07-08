package cwinter.codecraft.core.game

private[codecraft] sealed trait WinCondition
private[codecraft] case object DestroyEnemyMotherships extends WinCondition

