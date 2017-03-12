name := "akka-streams-workshop"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies ++= {
  val akkaV       = "2.4.17"
  val akkaHttpV   = "10.0.4"
  Seq(
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-http" % akkaHttpV
  )
}