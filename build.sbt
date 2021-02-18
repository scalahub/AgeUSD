name := "AgeUSD"

version := "0.1"

updateOptions := updateOptions.value.withLatestSnapshots(false)

scalaVersion := "2.12.10"

lazy val Kiosk = RootProject(uri("git://github.com/scalahub/Kiosk.git"))

libraryDependencies ++= Seq(
  "com.squareup.okhttp3" % "mockwebserver" % "3.14.9" % Test,
  "org.scalatest" %% "scalatest" % "3.0.8" % Test,
  "org.scalacheck" %% "scalacheck" % "1.14.+" % Test
)

lazy val root = (project in file("."))
  .dependsOn(
    Kiosk
  )
