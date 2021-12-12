name := "Akka streams workshop"

version := "0.1"

scalaVersion := "2.13.4"

val akkaVersion = "2.6.15"
libraryDependencies ++= List(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-contrib" % "0.11",
  "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "3.0.3",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test
)

val sttpVersion = "3.3.11"
libraryDependencies ++= List(
  "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
  "com.softwaremill.sttp.client3" %% "akka-http-backend" % sttpVersion,
  "com.softwaremill.sttp.client3" %% "zio-json" % sttpVersion,
  "com.softwaremill.sttp.client3" %% "akka-http-backend" % sttpVersion
)

libraryDependencies += "org.fusesource.jansi" % "jansi" % "2.3.4"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.10" % "test"

scalacOptions ++= List(
  "-Xfatal-warnings",
)

