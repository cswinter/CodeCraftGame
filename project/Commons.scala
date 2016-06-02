import sbt._
import Keys._

object Commons {
  val appVersion = "0.3.0.3-SNAPSHOT"

  val settings: Seq[Def.Setting[_]] = Seq(
    organization := "org.codecraftgame",
    version := appVersion,
    scalaVersion := "2.11.7",
    scalacOptions := Seq(
      "-Xlint",
      "-deprecation",
      "-Xfatal-warnings",
      "-feature"
    ),
    autoAPIMappings := true,

    publishArtifact in Test := false,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
      else Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },

    pomIncludeRepository := { _ => false },
    pomExtra :=
      <url>http://www.codecraftgame.org</url>
      <licenses>
        <license>
          <name>MIT License</name>
          <url>http://www.opensource.org/licenses/mit-license.php</url>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:cswinter/CodeCraftGame.git</url>
        <connection>scm:git:git@github.com:cswinter/CodeCraftGame.git</connection>
      </scm>
      <developers>
        <developer>
          <id>cswinter</id>
          <name>Clemens Winter</name>
          <url>https://github.com/cswinter</url>
          <organizationUrl>http://www.codecraftgame.org</organizationUrl>
        </developer>
      </developers>
  )
}

