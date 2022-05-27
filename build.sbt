name := "AgeUSD"

updateOptions := updateOptions.value.withLatestSnapshots(false)

scalaVersion := "2.12.10"

resolvers ++= Seq(
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "SonaType" at "https://oss.sonatype.org/content/groups/public",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

libraryDependencies ++= Seq(
  "io.github.ergoplatform" %% "kiosk" % "1.0",
  "com.squareup.okhttp3" % "mockwebserver" % "3.14.9" % Test,
  "org.scalatest" %% "scalatest" % "3.0.8" % Test,
  "org.scalacheck" %% "scalacheck" % "1.14.+" % Test
)

lazy val root = (project in file("."))
