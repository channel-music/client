package com.kalouantonis.channel

import java.io.File
import javax.sound.sampled.AudioSystem

import cats.effect.IO
import com.kalouantonis.channel.media.{Audio, PlayQueue}

object Player {
  case class Track(title: String, album: String, artist: String)

  // This is part of the player API
  def playUntilComplete(filePath: String): IO[Unit] = {
    val file = new File(filePath)
    val tags = AudioSystem.getAudioFileFormat(file)

    for {
      stream <- Audio.openStream(file.toURI.toURL)
      // We're just running these streams unsafely for now
    } yield stream.compile.drain.unsafeRunSync()
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
    Player.playUntilComplete(filePath).unsafeRunSync()
  }
}
