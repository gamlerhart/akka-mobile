import sbt._
import Keys._


object MyBuild extends Build {

  import Dependencies._

  lazy val buildSettings = Seq(
    organization := "info.gamlor.akkamobile",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.9.1"
  )

  lazy val root = Project("root", file(".")) aggregate (akkamobile)

  lazy val akkamobile: Project = Project(
    id = "akka-mobile",
    base = file("./akka-mobile"),
    settings = defaultSettings ++ Seq(	
      unmanagedBase <<= baseDirectory { base => base / "lib" },
      libraryDependencies ++= Seq(akkaActors, scalaTest)
    ))
	
  lazy val akkamobileTest: Project = Project(
    id = "akka-mobile-test",
    base = file("./akka-mobile-test"),
    dependencies = Seq(akkamobile),
    settings = defaultSettings ++ Seq(
      libraryDependencies ++= Seq(akkaActors, akkaRemoteActors, scalaTest, akkaTestKit)
    ))


  override lazy val settings = super.settings ++ buildSettings

  lazy val defaultSettings = Defaults.defaultSettings ++ Seq(
    resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",

    // compile options
    scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked") ++ (
      if (true || (System getProperty "java.runtime.version" startsWith "1.7")) Seq() else Seq("-optimize")), // -optimize fails with jdk7
    javacOptions ++= Seq("-Xlint:deprecation"),
    // show full stack traces
    testOptions in Test += Tests.Argument("-oF")
  )


}

object Dependencies {

  val scalaTest = "org.scalatest" %% "scalatest" % "1.6.1" % "test"
  val akkaTestKit = "se.scalablesolutions.akka" % "akka-testkit" % "1.2" % "test"


  val akkaActors = "se.scalablesolutions.akka" % "akka-actor" % "1.2"
  val akkaRemoteActors = "se.scalablesolutions.akka" % "akka-remote" % "1.2"
}
