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



lazy val util = (project in file("util")).
  settings(Commons.settings: _*).
  settings(
    name := "cg.util",
    libraryDependencies ++= commonDependencies
  )

lazy val graphics = (project in file("graphics")).
  settings(Commons.settings: _*).
  settings(
    name := "cg.graphics",
    libraryDependencies ++= commonDependencies,
    libraryDependencies ++= graphicsDependencies
  ).dependsOn(util)

lazy val collisions = (project in file("collisions")).
  settings(Commons.settings: _*).
  settings(
    name := "cg.collisions",
    libraryDependencies ++= commonDependencies
  ).dependsOn(util)

lazy val physics = (project in file("physics")).
  settings(Commons.settings: _*)
  .settings(
    name := "cg.physics",
    libraryDependencies ++= commonDependencies
  ).dependsOn(util, collisions)

lazy val testai = (project in file("testai")).
  settings(Commons.settings: _*).
  settings(
    name := "cg.testai",
    libraryDependencies ++= commonDependencies
  ).dependsOn(core)

lazy val demos = (project in file("demos")).
  settings(Commons.settings: _*).
  settings(
    name := "cg.demos",
    libraryDependencies ++= commonDependencies
  ).dependsOn(graphics, collisions, physics)



lazy val core = (project in file("core")).
  settings(Commons.settings: _*).
  settings(
    name := "codecraft",
    libraryDependencies ++= commonDependencies,
    assemblyMergeStrategy in assembly := {
      case x if x.endsWith(".so") || x.endsWith(".dll") || x.endsWith(".jnilib") =>
        jogl_merge_strategy
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  ).dependsOn(graphics, physics, collisions, util)

