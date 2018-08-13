package com.kalouantonis.channel

import java.io.File
import javax.sound.sampled.AudioSystem

// FIXME: transient dependency
import cats.effect.IO
import com.kalouantonis.channel.media.{Audio, PlayQueue}

object Player {
  case class Track(title: String, album: String, artist: String)

  // FIXME: partial function
  def audioStream(url: java.net.URL): fs2.Stream[IO, Unit] = {
    val bufSize = 4096 // FIXME: is this dependant on format?
    val inputStream = AudioSystem.getAudioInputStream(url)
    val (decodedStream, decodedFormat) = Audio.decoder[IO](inputStream, bufSize)
    // Fetch the audio output stream
    val pcmOutputStream = Audio.sourceDataLine(decodedFormat)

    decodedStream.to(pcmOutputStream)
  }

  def playUntilFinish(filePath: String): IO[Unit] = {
    val url = new File(filePath).toURI.toURL
    audioStream(url)
      .compile
      .drain
  }
}

object Application {
  def main(args: Array[String]): Unit = {
    val pq = PlayQueue(
      Vector(
        Player.Track("Black Sabbath", "Black Sabbath", "Black Sabbath"),
        Player.Track("N.I.B", "Black Sabbath", "Black Sabbath")))

    println(s"Current item: ${pq.current}")
    println(s"Next item: ${pq.next.current}")

    val filePath = args.headOption.getOrElse("song.mp3")
    println(s"Attempting to play song: $filePath")
    Player.playUntilFinish(filePath).unsafeRunSync()
  }
}
