package kalouantonis.channel

import java.io.File
import javax.sound.sampled.AudioSystem

// FIXME: transient dependency
import cats.effect.IO

import kalouantonis.channel.media.{Audio, PlayQueue}

object Player {
  case class Track(title: String, album: String, artist: String)

  // FIXME: partial function
  def audioStream(url: java.net.URL): fs2.Stream[IO, Unit] = {
    val inputStream = AudioSystem.getAudioInputStream(url)
    val decodedFormat = Audio.targetFormat(inputStream.getFormat)
    // FIXME: figure out integral number of bytes for buffer
    // FIXME: do these differ for in and out?
    val bufferSize = decodedFormat.getSampleRate * decodedFormat.getSampleSizeInBits
    println(s"buffer size: $bufferSize")
    // The format used by the current encoded stream
    val encodedStream = fs2.io.readInputStream[IO](IO(inputStream), bufferSize.toInt)
    // Decode input stream
    val mp3Decoder = Audio.mp3Decoder[IO](decodedFormat)
    // Fetch the audio output stream
    val pcmOutputStream = Audio.sourceDataLineSink(decodedFormat)

    encodedStream
      // NOTE: could use through2 to stream to filesystem
      .through(mp3Decoder)
      .to(pcmOutputStream)
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

    args match {
      case Array(filePath, _*) =>
        println(s"Attempting to play song: $filePath")
        Player.playUntilFinish(filePath).unsafeRunSync()

      case _ =>
        println("No song provided, not playing anything...")
    }
  }
}
