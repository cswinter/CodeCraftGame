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
    name := "codecraft-util",
    libraryDependencies ++= commonDependencies,
    libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.11.7"
  )
lazy val utilJVM = util.jvm
lazy val utilJS = util.js

lazy val graphics = (crossProject in file("graphics")).
  settings(Commons.settings: _*).
  settings(
    name := "codecraft-graphics",
    libraryDependencies ++= commonDependencies
  ).jvmSettings(
    libraryDependencies ++= graphicsJVMDependencies
  ).jsSettings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.8.2",
    // add resources of the current project into the build classpath
    unmanagedClasspath in Compile <++= unmanagedResources in Compile
  ).dependsOn(util)
lazy val graphicsJVM = graphics.jvm
lazy val graphicsJS = graphics.js

lazy val collisions = (crossProject in file("collisions")).
  settings(Commons.settings: _*).
  settings(
    name := "codecraft-collisions",
    libraryDependencies ++= commonDependencies
  ).dependsOn(util)
lazy val collisionsJVM = collisions.jvm
lazy val collisionsJS = collisions.js

lazy val physics = (crossProject in file("physics")).
  settings(Commons.settings: _*).
  settings(
    name := "codecraft-physics",
    libraryDependencies ++= commonDependencies
  ).dependsOn(util, collisions)
lazy val physicsJVM = physics.jvm
lazy val physicsJS = physics.js

lazy val testai = (project in file("testai")).
  settings(Commons.settings: _*).
  settings(
    name := "codecraft-testai",
    libraryDependencies ++= commonDependencies
  ).dependsOn(coreJVM)

val demos = (crossProject in file("demos")).
  settings(Commons.settings: _*).
  settings(
    name := "codecraft-demos",
    libraryDependencies ++= commonDependencies
  ).dependsOn(graphics, collisions, physics)
lazy val demosJVM = demos.jvm
lazy val demosJS = demos.js


val core = (crossProject in file("core")).
  settings(Commons.settings: _*).
  settings(
    name := "codecraft",
    libraryDependencies ++= commonDependencies,
    libraryDependencies +=  "org.scala-lang.modules" % "scala-async_2.11" % "0.9.5",
    libraryDependencies += "com.lihaoyi" %%% "upickle" % "0.3.6"
  ).jvmSettings(
    assemblyMergeStrategy in assembly := {
      case x if x.endsWith(".so") || x.endsWith(".dll") || x.endsWith(".jnilib") =>
        jogl_merge_strategy
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    resolvers += "Spray" at "http://repo.spray.io",
    libraryDependencies ++= coreJVMDependencies
  ).jsSettings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.8.2"
  ).dependsOn(graphics, physics, collisions, util)
lazy val coreJVM = core.jvm
lazy val coreJS = core.js


lazy val scalajsTest = (project in file("scalajs-test")).
  enablePlugins(ScalaJSPlugin).
  settings(Commons.settings: _*).
  settings(
    name := "codecraft-scalajs-test",
    libraryDependencies ++= commonDependencies,
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.8.2"
  ).dependsOn(coreJS, demosJS)


val skippedPackages = List(
  "cwinter.codecraft.core.replay",
  "scala.scalajs.js.annotation",
  "cwinter.codecraft.core.multiplayer"
)
val projects: Seq[ProjectReference] = Seq(coreJVM, graphicsJVM, utilJVM, physicsJVM, collisionsJVM)
lazy val docs = (project in file("docs"))
  .settings(Commons.settings: _*)
  .settings(
    name := "docs",
    scalacOptions += "-Ymacro-expand:none",
    scalacOptions in (Compile, doc) ++= Seq("-doc-root-content", baseDirectory.value + "/root-doc.txt"),
    libraryDependencies ++= commonDependencies,
    libraryDependencies ++= graphicsJVMDependencies,
    libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.11.7"
  )
  .settings(
    libraryDependencies +=  "org.scala-lang.modules" % "scala-async_2.11" % "0.9.5",
    libraryDependencies += "com.lihaoyi" %%% "upickle" % "0.3.6",
    libraryDependencies ++= coreJVMDependencies,
    unmanagedSourceDirectories in Compile <<=
      (projects map (unmanagedSourceDirectories in _ in Compile)).join.apply{(s) => s.flatten},
    // this might not work under Windows (which uses ; as separator)
    scalacOptions in (Compile, doc) ++= List(s"-skip-packages", skippedPackages.mkString(":"))
  )


lazy val root = project.in(file(".")).
  aggregate(coreJS, coreJVM, graphicsJS, graphicsJVM, utilJS, utilJVM, physicsJS, physicsJVM, collisionsJS, collisionsJVM)


