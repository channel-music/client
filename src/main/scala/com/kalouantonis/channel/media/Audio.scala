package com.kalouantonis.channel.media

import javax.sound.sampled.{AudioFormat, AudioSystem, AudioInputStream, SourceDataLine}

import cats.effect.IO
import fs2.{Chunk, Stream, Sink}

// FIXME: clean up this whole module

object Audio {
  private[this] val BUFFER_SIZE = 4096

  /**
    * Represents a stream of IO resulting in audio being played.
    */
  type AudioStream = Stream[IO, Unit]

  /**
    * Open a stream to the given URL.
    *
    * The stream is completely synchronous, as in it will receive audio data
    * and possibly block on writing. It is expected to fail when the URL can
    * not be reached, the input format is either not recognized or not decodable
    * or finally if the output stream can not be reached.
    */
  def openStream(url: java.net.URL): IO[AudioStream] =
    IO(AudioSystem.getAudioInputStream(url)) map { rawInputStream =>
      val targetFormat = new AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        rawInputStream.getFormat.getSampleRate,
        16,
        rawInputStream.getFormat.getChannels,
        rawInputStream.getFormat.getChannels * 2,
        rawInputStream.getFormat.getSampleRate,
        false
      )
      val decodedStream = decodeAudioStream(rawInputStream, targetFormat)

      decodedStream.to(pcmOutputStream(targetFormat))
    }

  private[this] def decodeAudioStream(inputStream: AudioInputStream,
                                      targetFormat: AudioFormat): Stream[IO, Byte] =
    fs2.io.readInputStream[IO](
      IO(AudioSystem.getAudioInputStream(targetFormat, inputStream)),
      // NOTE: I think 4096 left here is fine for now
      BUFFER_SIZE, closeAfterUse = true)


  private[this] def pcmOutputStream(format: AudioFormat): Sink[IO, Byte] =
    writeToSourceDataLine(
      IO {
        val line = AudioSystem.getSourceDataLine(format)
        line.open(format)
        line.start()
        line
      })

  private[this] def writeToSourceDataLine(fos: IO[SourceDataLine]): Sink[IO, Byte] = s => {
    def writeBytesToLine(os: SourceDataLine, bytes: Chunk[Byte]): IO[Unit] =
      // TODO: do something if not all the bytes aren't written
      IO{ val _ = os.write(bytes.toArray, 0, bytes.size); ()}

    def sink(os: SourceDataLine): Stream[IO, Unit] =
      s.chunks.evalMap(writeBytesToLine(os, _))

    // Always free the stream after use
    Stream.bracket(fos)(sink, line => IO(line.close()))
  }
}
