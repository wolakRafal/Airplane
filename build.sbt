name := "Airplanes"

version := "1.0"

scalaVersion := "2.11.2"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "latest.integration" % "test",
    "com.typesafe.akka" % "akka-testkit_2.11" % "2.3.5",
	"com.typesafe.akka" %% "akka-actor" % "2.3.5"
)
