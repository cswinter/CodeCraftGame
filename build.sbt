import Dependencies._


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
    name := "cg.collisions"
  ).dependsOn(util)

lazy val physics = (project in file("physics")).
  settings(Commons.settings: _*)
  .settings(
    name := "cg.physics",
    libraryDependencies ++= commonDependencies
  ).dependsOn(util, collisions)

lazy val core = (project in file("core")).
  settings(Commons.settings: _*).
  settings(
    name := "cg.core",
    libraryDependencies ++= commonDependencies
  ).dependsOn(graphics, physics, collisions, util)

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
