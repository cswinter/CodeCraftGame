package cwinter.codecraft.core.replay


private[codecraft] object AsInt {
  def unapply(s: String) = try {
    Some(s.toInt)
  } catch {
    case e: NumberFormatException => None
  }
}

