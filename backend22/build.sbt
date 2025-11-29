val scala3Version = "3.7.4"

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-Wvalue-discard",
    "-Wunused:all",
  ),
  Compile / console / scalacOptions ~= { _.filterNot(_ == "-Wunused:all") },
  scalafmtOnCompile := true,
)

lazy val root = project
  .in(file("."))
  .settings(
    name         := "ai_wasei3",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    commonSettings,
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.4.0",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,
  )
