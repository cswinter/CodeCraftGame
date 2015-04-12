import Dependencies._


lazy val maths = (project in file("maths")).
  settings(Commons.settings: _*).
  settings(
    name := "cg.maths",
    libraryDependencies ++= commonDependencies
  )


lazy val graphics = (project in file("graphics")).
  settings(Commons.settings: _*).
  settings(
    name := "cg.graphics",
    libraryDependencies ++= commonDependencies,
    libraryDependencies ++= graphicsDependencies
  ).dependsOn(maths)


lazy val collisions = (project in file("collisions")).
  settings(Commons.settings: _*).
  settings(
    name := "cg.collisions"
  ).dependsOn(maths)

lazy val physics = (project in file("physics")).
  settings(Commons.settings: _*)
  .settings(
    name := "cg.physics",
    libraryDependencies ++= commonDependencies
  ).dependsOn(graphics, maths)

lazy val simulation = (project in file("simulation")).
  settings(Commons.settings: _*).
  settings(
    name := "cg.simulator",
    libraryDependencies ++= commonDependencies
  ).dependsOn(graphics, collisions, maths)

