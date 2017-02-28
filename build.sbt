name := "concurrent-map-benchmarks"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= {
  val akkaV       = "2.4.17"
  Seq(
    "com.typesafe.akka" %% "akka-stream" % akkaV
  )
}