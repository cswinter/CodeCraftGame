package cwinter.codecraft.util

import scala.io.Source
import scala.language.experimental.macros
import scala.reflect.macros.blackbox


/**
 * Macro to import file contents as a string at compile time.
 * To make this work with sbt, add this line to the settings:
 * `unmanagedClasspath in Compile <++= unmanagedResources in Compile`
 */
private[cwinter] object CompileTimeLoader {
  def loadResource(path: String): String = macro loadResourceImpl

  def loadResourceImpl(c: blackbox.Context)(path: c.Expr[String]) = {
    import c.universe._
    path.tree match {
      case Literal(Constant(s: String)) =>
        val stream = this.getClass.getResourceAsStream("/" + s)
        if (stream == null) c.abort(c.enclosingPosition, "Couldn't get shader resource: " + s)
        Literal(Constant(Source.fromInputStream(stream).mkString))
      case _ =>
        c.abort(c.enclosingPosition, "Need a literal path!")
    }
  }
}

