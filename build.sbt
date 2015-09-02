name := "http4s-middlewares"

organization := "me.ugo"

scalaVersion := "2.11.7"

val http4sVersion = "0.9.2"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-server" % http4sVersion
)

licenses +=("WTFPL", url("http://www.wtfpl.net/txt/copying/"))
