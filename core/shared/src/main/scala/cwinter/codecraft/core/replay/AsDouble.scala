package cwinter.codecraft.core.replay

/**
 * Created by Clemens on 26/06/2015.
 */
object AsDouble {
  def unapply(s: String) = try {
    Some(s.toDouble)
  } catch {
    case e: NumberFormatException => None
  }
}
