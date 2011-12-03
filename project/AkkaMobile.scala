import sbt._
import Keys._


object MyBuild extends Build {

  import Dependencies._

  lazy val buildSettings = Seq(
    organization := "info.gamlor.akkamobile",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.9.1"
  )

  lazy val root = Project("root", file(".")) aggregate(akkamobileClient, akkamobileServer, akkamobileTest)

  lazy val akkamobileClient: Project = Project(
    id = "akka-mobile-client",
    base = file("./akka-mobile-client"),
    settings = defaultSettings ++ Seq(
      unmanagedBase <<= baseDirectory {
        base => base / "lib"
      },
      libraryDependencies ++= Seq(akkaActors, scalaTest, akkaTestKit, mockito)
    ))

  lazy val akkamobileServer: Project = Project(
    id = "akka-mobile-server",
    base = file("./akka-mobile-server"),
    settings = defaultSettings ++ Seq(
      libraryDependencies ++= Seq(akkaActors, akkaRemoteActors, netty, asyncHttp, scalaTest, akkaTestKit, mockito)
    )
  ) dependsOn (akkamobileClient % "compile->compile;test->test")

  lazy val akkamobileTest: Project = Project(
    id = "akka-mobile-test",
    base = file("./akka-mobile-test"),
    settings = defaultSettings ++ Seq(
      libraryDependencies ++= Seq(akkaActors, akkaRemoteActors, netty, scalaTest, akkaTestKit)
    )) dependsOn (akkamobileServer % "compile->compile;test->test")


  lazy val akkaMobileDemoServer: Project = Project(
    id = "akka-mobile-demo-server",
    base = file("./akka-mobile-demo-server"),
    settings = defaultSettings ++ Seq(
      libraryDependencies ++= Seq(akkaActors, akkaRemoteActors, netty, h2Database, scalaQuery, scalaTest)
    )) dependsOn (akkamobileServer % "compile->compile;test->test")

  override lazy val settings = super.settings ++ buildSettings

  lazy val defaultSettings = Defaults.defaultSettings ++ Seq(
    resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers += "Tools-Repo" at "http://scala-tools.org/repo-releases/",

    // compile options
    scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked") ++ (
      if (true || (System getProperty "java.runtime.version" startsWith "1.7")) Seq() else Seq("-optimize")), // -optimize fails with jdk7
    javacOptions ++= Seq("-source", "1.6", "-target", "1.6", "-Xlint:deprecation"),
    // show full stack traces
    testOptions in Test += Tests.Argument("-oF")
  )


}

object Dependencies {

  val scalaTest = "org.scalatest" %% "scalatest" % "1.6.1" % "test"
  val netty = "org.jboss.netty" % "netty" % "3.2.5.Final"
  val asyncHttp = "com.ning" % "async-http-client" % "1.6.5"

  val akkaTestKit = "se.scalablesolutions.akka" % "akka-testkit" % "1.2" % "test"
  val akkaActors = "se.scalablesolutions.akka" % "akka-actor" % "1.2"
  val akkaRemoteActors = "se.scalablesolutions.akka" % "akka-remote" % "1.2"
  val mockito = "org.mockito" % "mockito-core" % "1.9.0-rc1" % "test"

  val h2Database = "com.h2database" % "h2" % "1.3.161"
  val scalaQuery = "org.scalaquery" % "scalaquery_2.9.0-1" % "0.9.5"
}
