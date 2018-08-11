package kalouantonis.channel

import java.{io => jio}
import java.util.concurrent.atomic.AtomicReference
import javax.sound.sampled.{AudioSystem, AudioFormat, AudioInputStream}

import scala.concurrent.{blocking, Future, ExecutionContext}
import scala.util.Try

import kalouantonis.channel.media.{Decoder, MP3Decoder}

// FIXME: transient dependency
import cats.effect.IO
import fs2.Stream

final class PlayQueue[T] private(items: IndexedSeq[T], index: Int) {
  def append(item: T): PlayQueue[T] =
    new PlayQueue(item +: items, index)
  def reset: PlayQueue[T] =
    new PlayQueue(items, 0)

  def current: Option[T] = items.lift(index)
  def next: PlayQueue[T] =
    new PlayQueue(items, index + 1)
  def previous: PlayQueue[T] =
    new PlayQueue(items, if (index == 0) index else index - 1)

  def isEmpty: Boolean = items.isEmpty
}

object PlayQueue {
  def apply[T](items: IndexedSeq[T] = IndexedSeq.empty): PlayQueue[T] =
    new PlayQueue(items, 0)
}

object Player {
  case class Track(title: String, album: String, artist: String)

  sealed trait StreamState
  case object Playing extends StreamState
  case object Paused extends StreamState
  case object Stopped extends StreamState

  final class AudioStream(inputStream: AudioInputStream, val audioFormat: AudioFormat)
                         (implicit ec: ExecutionContext) {
    // what is the thread safety of this?
    private[this] val sourceLine = AudioSystem.getSourceDataLine(audioFormat)

    private[this] val bufSize = 2048

    def open: Stream[IO, Unit] = {
      val acquire = IO { sourceLine.open(audioFormat); () }
      val release = IO { sourceLine.close(); () }
      val stream = IO {
        // FIXME: direct mutation
        val buf = new Array[Byte](bufSize)
        // FIXME: need to keep track of total
        val bytesRead = inputStream.read(buf)
        sourceLine.write(buf, bytesRead, bufSize)
        ()
      }

      Stream.bracket(acquire)(_ => Stream.eval(stream), _ => release)
    }
  }

  def playSound(filePath: String): Unit = {
    import javax.sound.sampled._

    def runStream(line: SourceDataLine, decoder: Decoder): Unit = {
      val buf = new Array[Byte](4096)

      var isStopped = false
      while (!isStopped) {
        val bytesRead = decoder.read(buf)

        if (bytesRead == -1) {
          isStopped = true
        } else {
          line.write(buf, 0, bytesRead)
        }
      }
    }

    val audioStream = AudioSystem.getAudioInputStream(new jio.File(filePath))
    val audioDecoder = new MP3Decoder(audioStream)
    val outputStream = AudioSystem.getSourceDataLine(audioDecoder.audioFormat)

    audioDecoder.open()
    outputStream.open()
    outputStream.start()

    // stream
    runStream(outputStream, audioDecoder)

    audioDecoder.close()
    outputStream.drain()
    outputStream.close()

    // // FIXME: not the right type of executor
    // val audioStream = new AudioStream(inputStream, audioFileFormat.getFormat)(
    //   ExecutionContext.fromExecutor(java.util.concurrent.Executors.newSingleThreadExecutor()))
    // // wait for completion
    // audioStream.open.compile.drain.unsafeRunSync()
    // audioStream
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

    println("Lets try playing a song...")
    Player.playSound("song.mp3")
  }
}
