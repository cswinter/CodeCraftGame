package cwinter.codecraft.core


private[codecraft] sealed trait WinCondition

private[codecraft] case object DestroyEnemyMotherships extends WinCondition

