name := "JOGLMaven"

version := "1.0"

scalaVersion := "2.11.4"


scalacOptions ++= Seq(
  "-Xlint",
  "-deprecation",
  "-Xfatal-warnings"
)

// libraryDependencies += groupID % artifactID % revision

libraryDependencies += "org.jogamp.gluegen" % "gluegen-rt-main" % "2.2.4"

libraryDependencies += "org.jogamp.jogl" % "jogl-all-main" % "2.2.4"

libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "1.0.1"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"

libraryDependencies += "joda-time" % "joda-time" % "2.7"

