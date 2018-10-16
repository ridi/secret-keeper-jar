import sbt.Keys.publishMavenStyle

name := "secret-keeper-jar"

version := "0.1.0-TEST"

scalaVersion := "2.12.7"

organization := "com.ridi"

resolvers += "Maven2 Repository" at "http://repo1.maven.org/maven2/"

libraryDependencies ++= Seq(
  "com.google.http-client" % "google-http-client" % "1.23.0",
  "com.google.http-client" % "google-http-client-jackson2" % "1.23.0",
  "org.apache.logging.log4j" % "log4j-core" % "2.11.1",
  "org.apache.logging.log4j" % "log4j-api" % "2.11.1",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.scalamock" %% "scalamock" % "4.1.0" % "test"
)

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
bintrayOrganization := Some("ridi-data")
publishMavenStyle := true
