lazy val root = (project in file("."))
  .settings(
    name := "Channel",
    organization := "com.kalouantonis",
    scalaVersion := "2.12.6",
    scalacOptions ++= Seq(
      "-language:higherKinds",
      "-language:existentials",
      "-Xlint",
      "-deprecation",
      "-feature",
      "-unchecked"
    ),
    libraryDependencies ++= Dependencies.all,
    // FIXME
    //
    // Fork must be enabled for run because the SPI's for sound
    // aren't loaded properly in the SBT environment, for some reason.
    Compile / run / fork := true
  )
