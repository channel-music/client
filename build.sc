import mill._, scalalib._

object channel extends ScalaModule {
  def scalaVersion = "2.12.4"
  def mainClass = Some("kalouantonis.channel.Application")

  def ivyDeps = Agg(
    // Audio
    ivy"com.googlecode.soundlibs:tritonus-share:0.3.7.4",
    ivy"com.googlecode.soundlibs:jlayer:1.0.1.4",
    ivy"com.googlecode.soundlibs:mp3spi:1.9.5.4",
    // Streaming
    ivy"co.fs2::fs2-core:0.10.5",
    ivy"co.fs2::fs2-io:0.10.5",
    // Testing -- FIXME: only add during test run
    ivy"org.scalatest::scalatest:3.0.5"
  )
}
