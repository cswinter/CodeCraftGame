import sbt._

object Dependencies {
  // libraryDependencies += groupID % artifactID % revision
  val gluegen = "org.jogamp.gluegen" % "gluegen-rt-main" % "2.3.2"
  val jogl = "org.jogamp.jogl" % "jogl-all-main" % "2.3.2"
  val scalaSwing = "org.scala-lang.modules" % "scala-swing_2.11" % "1.0.1"
  val jodaTime =   "joda-time" % "joda-time" % "2.7"
  val jodaConvert = "org.joda" % "joda-convert" % "1.2"
  val scalatest = "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"
  val sprayWebsocket = "com.wandoulabs.akka" %% "spray-websocket" % "0.1.4"
  val akka = "com.typesafe.akka" %% "akka-actor" % "2.3.13"
  val javaxWebsocket = "javax.websocket" % "javax.websocket-client-api" % "1.1"
  val javaxWebsocketImpl = "org.glassfish.tyrus" % "tyrus-container-grizzly-client" % "1.12"
  val jetm = "fm.void.jetm" % "jetm" % "1.2.3"


  val commonDependencies: Seq[ModuleID] = Seq(
    scalatest
  )

  val coreJVMDependencies: Seq[ModuleID] = Seq(
    sprayWebsocket,
    akka,
    javaxWebsocket,
    javaxWebsocketImpl,
    jetm
  )

  val graphicsJVMDependencies: Seq[ModuleID] = Seq(
    gluegen,
    jogl,
    scalaSwing,
    jodaTime,
    jodaConvert
  )
}
