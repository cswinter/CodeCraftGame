package cwinter.codecraft.core.replay

private[codecraft] object AsDouble {
  def unapply(s: String) = try {
    Some(s.toDouble)
  } catch {
    case e: NumberFormatException => None
  }
}
