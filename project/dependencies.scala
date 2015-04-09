import sbt._
import Keys._

object Dependencies {
  // libraryDependencies += groupID % artifactID % revision
  val gluegen = "org.jogamp.gluegen" % "gluegen-rt-main" % "2.2.4"
  val jogl = "org.jogamp.jogl" % "jogl-all-main" % "2.2.4"
  val scalaSwing = "org.scala-lang.modules" %% "scala-swing" % "1.0.1"
  val jodaTime =   "joda-time" % "joda-time" % "2.7"
  val jodaConvert = "org.joda" % "joda-convert" % "1.2"
  val scalatest = "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"

  val commonDependencies: Seq[ModuleID] = Seq(
    scalatest
  )

  val graphicsDependencies: Seq[ModuleID] = Seq(
    gluegen,
    jogl,
    scalaSwing,
    jodaTime,
    jodaConvert
  )
}
