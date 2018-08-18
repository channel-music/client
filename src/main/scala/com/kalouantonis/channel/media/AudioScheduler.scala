package com.kalouantonis.channel.media

import java.util.concurrent.{Executors, BlockingQueue, LinkedBlockingQueue}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import javax.sound.sampled.{AudioSystem, AudioFormat, AudioInputStream, SourceDataLine}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}

import StreamScheduler.Command

class StreamScheduler {
  import StreamScheduler._

  private implicit val ec: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))

  private[this] val consumerQueue: LinkedBlockingQueue[Command] =
    new LinkedBlockingQueue()

  def start(): Unit = {
    ec.execute(new Consumer(consumerQueue))
  }

  def play(url: java.net.URL): Unit = {
    consumerQueue.put(Play(url))
  }

  // def resume(): Unit = {}

  def pause(): Unit = {
    consumerQueue.put(Pause)
  }
}

object StreamScheduler {
  def openAudioStream(url: java.net.URL): (AudioInputStream, SourceDataLine) = {
    val rawIn = AudioSystem.getAudioInputStream(url)
    val sourceFormat = rawIn.getFormat
    val targetFormat =
      new AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        sourceFormat.getSampleRate,
        16,
        sourceFormat.getChannels,
        sourceFormat.getChannels * 2,
        sourceFormat.getSampleRate,
        false)

    val decodedIn = AudioSystem.getAudioInputStream(targetFormat, rawIn)
    val pcmOut = AudioSystem.getSourceDataLine(targetFormat)
    pcmOut.open(targetFormat)
    pcmOut.start()

    (decodedIn, pcmOut)
  }

  def readAudioStream(inputStream: AudioInputStream, bufSize: Int): Option[Array[Byte]] = {
    val buf = new Array[Byte](bufSize)
    val bytesRead = inputStream.read(buf, 0, bufSize)

    if (bytesRead == -1) None else Some(buf)
  }

  def writeToLine(line: SourceDataLine, bytes: Array[Byte]): Unit = {
    val bytesWritten = line.write(bytes, 0, bytes.length)
    // TODO: handle missing written bytes
  }

  sealed trait Command
  case class Play(url: java.net.URL) extends Command
  case object Pause extends Command

  class Consumer(queue: BlockingQueue[Command]) extends Runnable {
    type Stream = (AudioInputStream, SourceDataLine)

    private[this] val currentStream = new AtomicReference[Option[Stream]](None)

    override def run(): Unit = {
      while (true) {
        val item = queue.take()
        println(s"Received message: $item")
        consume(item)
      }
    }

    private def consume(cmd: Command): Unit = {
      cmd match {
        case Play(url) =>
          // FIXME close the previous stream
          currentStream.compareAndSet(None, Some(openAudioStream(url)))

          val (in, out) = currentStream.get.get // get is safe here

          while (queue.peek() == null) {
            readAudioStream(in, 4096) match {
              case Some(bytes) =>
                writeToLine(out, bytes)
              case None =>
            }
          }

          // if we have a message, reconsume it
          Option(queue.poll()).foreach(consume)
        case Pause =>
          // do nothing, will reconsume
      }
    }
  }
}
