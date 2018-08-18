package com.kalouantonis.channel

import java.io.{ BufferedReader, File, InputStream, OutputStream }
import java.util.concurrent.{Executors, LinkedBlockingQueue}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import cats.effect.IO
import javax.sound.sampled.{AudioSystem, AudioFormat, AudioInputStream, SourceDataLine}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import com.kalouantonis.channel.media.{Audio, StreamScheduler, PlayQueue}

case class Track(title: String, album: String, artist: String, url: java.net.URL)

class Player(tracks: IndexedSeq[Track]) {
  private[this] val playQueue = new AtomicReference(PlayQueue(tracks))

  private[this] val scheduler = new StreamScheduler()

  // FIXME: dont do this here
  {
    scheduler.start()
  }

  def queue(track: Track): Unit = {
    playQueue.getAndUpdate(_ append track)
  }

  def play(): Unit =
    playQueue.get
      .current
      .foreach(track => scheduler.play(track.url))

  def pause(): Unit = {
    scheduler.pause()
  }
}

object Player {
  def apply(): Player = new Player(Vector.empty)
}

object Application {
  def main(args: Array[String]): Unit = {
    println("Welcome to channel 0.1!")
    println("==> You can type :q at any time to quit")

    val path = args.headOption.getOrElse("song.mp3")
    val file = new File(path)
    val player = Player()
    player.queue(
      Track(
        title = "Test track",
        album = "Test album",
        artist = "Test artist",
        url = file.toURI.toURL))
    player.play()
    player.pause()
  }
}
