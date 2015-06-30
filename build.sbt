import Dependencies._

// Required to create a fat jar containing all jogl native libraries
// Is probably a bit of a hack
// see: https://github.com/sbt/sbt-assembly/issues/141 and
// https://github.com/GiGurra/dcs-remote/blob/master/dcs-remote-renderer/build.sbt
val jogl_merge_strategy = new sbtassembly.MergeStrategy {
  val name = "jogl_rename"
  def apply(tempDir: File, path: String, files: Seq[File]) =
    Right(files flatMap { file =>
      val (jar, _, _, isJar) = sbtassembly.AssemblyUtils.sourceOfFileForMerge(tempDir, file)
      if (isJar) Seq(file -> s"natives/${jar.getPath.split("-natives-")(1).split(".jar")(0)}/$path")
      else Seq(file -> path)
    })
}

lazy val util = (crossProject in file("util")).
  settings(Commons.settings: _*).
  settings(
    name := "cg.util",
    libraryDependencies ++= commonDependencies
  )
lazy val utilJVM = util.jvm
lazy val utilJS = util.js

lazy val graphics = (crossProject in file("graphics")).
  settings(Commons.settings: _*).
  settings(
    name := "cg.graphics",
    libraryDependencies ++= commonDependencies
  ).jvmSettings(
    libraryDependencies ++= graphicsJVMDependencies
  ).jsSettings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.8.0"
  ).dependsOn(util)
lazy val graphicsJVM = graphics.jvm
lazy val graphicsJS = graphics.js

lazy val collisions = (crossProject in file("collisions")).
  settings(Commons.settings: _*).
  settings(
    name := "cg.collisions",
    libraryDependencies ++= commonDependencies
  ).dependsOn(util)
lazy val collisionsJVM = collisions.jvm
lazy val collisionsJS = collisions.js

lazy val physics = (crossProject in file("physics")).
  settings(Commons.settings: _*)
  .settings(
    name := "cg.physics",
    libraryDependencies ++= commonDependencies
  ).dependsOn(util, collisions)
lazy val physicsJVM = physics.jvm
lazy val physicsJS = physics.js

lazy val testai = (project in file("testai")).
  settings(Commons.settings: _*).
  settings(
    name := "cg.testai",
    libraryDependencies ++= commonDependencies
  ).dependsOn(coreJVM)

lazy val demos = (project in file("demos")).
  settings(Commons.settings: _*).
  settings(
    name := "cg.demos",
    libraryDependencies ++= commonDependencies
  ).dependsOn(graphicsJVM, collisionsJVM, physicsJVM)


lazy val core = (crossProject in file("core")).
  settings(Commons.settings: _*).
  settings(
    name := "codecraft",
    libraryDependencies ++= commonDependencies
  ).jvmSettings(
    assemblyMergeStrategy in assembly := {
      case x if x.endsWith(".so") || x.endsWith(".dll") || x.endsWith(".jnilib") =>
        jogl_merge_strategy
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  ).jsSettings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.8.0"
  ).dependsOn(graphics, physics, collisions, util)
lazy val coreJVM = core.jvm
lazy val coreJS = core.js


lazy val scalajsTest = (project in file("scalajs-test")).
  enablePlugins(ScalaJSPlugin).
  settings(Commons.settings: _*).
  settings(
    name := "scalajs-test",
    libraryDependencies ++= commonDependencies,
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.8.0"
  ).dependsOn(coreJS)







