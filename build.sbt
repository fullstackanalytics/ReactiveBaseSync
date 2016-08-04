lazy val commonSettings = Seq(
  organization := "io.fullstackanalytics",
  version := "0.1.0",
  scalaVersion := "2.11.8"
)

lazy val root = (project in file(".")).
  settings(commonSettings).
  settings(
    name := "ReactiveBaseSync",
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    libraryDependencies ++= Seq(
      "com.typesafe.play" % "play-json_2.11" % "2.5.4",
      "com.typesafe.akka" %% "akka-actor" % "2.4.8",
      "com.typesafe.akka" %% "akka-slf4j" % "2.4.8",
      "com.typesafe.akka" %% "akka-stream" % "2.4.8",
      "com.typesafe.akka" %% "akka-stream-testkit" % "2.4.8",
      "com.typesafe.akka" %% "akka-http-core" % "2.4.8",
      "com.typesafe.akka" %% "akka-http-experimental" % "2.4.8",
      "com.typesafe.akka" % "akka-http-testkit_2.11" % "2.4.8" % "test",
      "com.typesafe.akka" %% "akka-testkit" % "2.4.8",
      "org.scalactic" %% "scalactic" % "2.2.6",
      "org.scalatest" %% "scalatest" % "2.2.6" % "test"
    )
  )
