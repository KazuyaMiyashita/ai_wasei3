val scala3Version = "3.7.4"

lazy val commonSettings = Seq(
  version      := "0.1.0-SNAPSHOT",
  scalaVersion := scala3Version,
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
    name := "ai_wasei3",
    commonSettings,
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml"    % "2.4.0",
    libraryDependencies += "com.illposed.osc"        % "javaosc-core" % "0.9",
    libraryDependencies += "org.scalatest"          %% "scalatest"    % "3.2.19" % Test,
  )

lazy val server = project
  .in(file("server"))
  .settings(
    name := "ai_wasei3_server",
    commonSettings,
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml"         % "2.4.0",
    libraryDependencies += "com.illposed.osc"        % "javaosc-core"      % "0.9",
    libraryDependencies += "dev.zio"                %% "zio"               % "2.1.24",
    libraryDependencies += "dev.zio"                %% "zio-test"          % "2.1.24" % Test,
    libraryDependencies += "dev.zio"                %% "zio-test-sbt"      % "2.1.24" % Test,
    libraryDependencies += "dev.zio"                %% "zio-http"          % "3.3.3",
    libraryDependencies += "dev.zio"                %% "zio-json"          % "0.7.43",
    libraryDependencies += "dev.zio"                %% "zio-logging"       % "2.5.2",
    libraryDependencies += "dev.zio"                %% "zio-logging-slf4j" % "2.5.2",
    libraryDependencies += "ch.qos.logback"          % "logback-classic"   % "1.5.6",
    libraryDependencies += "org.scalatest"          %% "scalatest"         % "3.2.19" % Test,
  )
  .dependsOn(root)
