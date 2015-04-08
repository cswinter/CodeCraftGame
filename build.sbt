
lazy val root = (project in file(".")).
  settings(
    name := "JOGLMaven",
    version := "1.0",
    scalaVersion := "2.11.4",
    scalacOptions := Seq(
      "-Xlint",
      "-deprecation",
      "-Xfatal-warnings"
    ),
    // libraryDependencies += groupID % artifactID % revision
    libraryDependencies := Seq(
      "org.jogamp.gluegen" % "gluegen-rt-main" % "2.2.4",
      "org.jogamp.jogl" % "jogl-all-main" % "2.2.4",
      "org.scala-lang.modules" %% "scala-swing" % "1.0.1",
      "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test",
      "joda-time" % "joda-time" % "2.7",
      "org.joda" % "joda-convert" % "1.2"
    )
  )
