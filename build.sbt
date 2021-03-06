name := "akka-shop"

version := "0.1"

scalaVersion := "2.13.2"

lazy val akkaVersion = "2.6.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.1.12",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.slick" %% "slick" % "3.3.2",
  "org.xerial" % "sqlite-jdbc" % "3.31.1",
  "org.jsoup" % "jsoup" % "1.13.1",
  "org.scalatest" %% "scalatest" % "3.1.0" % Test,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
)
