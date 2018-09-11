lazy val fs2Version = "0.10.5"

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
    resolvers := Seq(
      "jThink Maven2 Repository" at "https://dl.bintray.com/ijabz/maven"
    ),
    libraryDependencies ++= Seq(
      // Stdlib
      "org.typelevel" %% "cats-core" % "1.1.0",
      "org.typelevel" %% "cats-effect" % "1.0.0-RC3",
      "io.monix" %% "monix" % "3.0.0-RC1",
      // Audio
      "com.googlecode.soundlibs" % "tritonus-share" % "0.3.7.4" % Runtime,
      "com.googlecode.soundlibs" % "jlayer" % "1.0.1.4" % Runtime,
      "com.googlecode.soundlibs" % "mp3spi" % "1.9.5.4" % Runtime,
      "net.jthink" % "jaudiotagger" % "2.2.3",
      // JavaFX
      "org.scalafx" %% "scalafx" % "8.0.144-R12",
      // Tests
      "org.scalatest" %% "scalatest" % "3.0.5" % Test
    ),
    // wartremoverErrors ++= Warts.unsafe,
    // FIXME
    //
    // Fork must be enabled for run because the SPI's for sound
    // aren't loaded properly in the SBT environment, for some reason.
    Compile / run / fork := true
  )
