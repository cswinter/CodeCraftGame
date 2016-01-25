import sbt._
import Keys._

object Commons {
  val appVersion = "0.2.1.6"

  val settings: Seq[Def.Setting[_]] = Seq(
    organization := "org.cwinter",
    version := appVersion,
    scalaVersion := "2.11.7",
    scalacOptions := Seq(
      "-Xlint",
      "-deprecation",
      "-Xfatal-warnings",
      "-feature"
    ),
    autoAPIMappings := true
  )
}
