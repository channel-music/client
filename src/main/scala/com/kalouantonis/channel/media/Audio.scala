package com.kalouantonis.channel.media

import java.{io => jio}
import javax.sound.sampled.{AudioFormat, AudioSystem, AudioInputStream, SourceDataLine}

import cats.effect.{IO, Sync}
import fs2.{Stream, Sink}

object Audio {
  // FIXME: this is practically a duplicate of fs2.io.JavaInputOutputStream,
  //        could we maybe convert it to an output stream?
  private[media] object AudioStreamUtil {
    // Is structural typing a runtime thing?
    def writeBytesToLine[F[_]](os: SourceDataLine, bytes: fs2.Chunk[Byte])(
      implicit F: Sync[F]): F[Unit] =
      F.delay(os.write(bytes.toArray, 0, bytes.size))

    def writeSourceDataLine[F[_]](fos: F[SourceDataLine],
                                  closeAfterUse: Boolean = true)(
      implicit F: Sync[F]): fs2.Sink[F, Byte] = s => {
      def setOs(os: SourceDataLine): Stream[F, Unit] =
        s.chunks.evalMap(writeBytesToLine(os, _))

      if (closeAfterUse)
        Stream.bracket(fos)(setOs, line => F.delay(line.close()))
      else
        Stream.eval(fos).flatMap(setOs)
    }
  }

  import AudioStreamUtil._

  def decoder[F[_]](
    sourceAIS: AudioInputStream, bufSize: Int)(
    implicit F: Sync[F]): (Stream[F, Byte], AudioFormat) = {
    val sourceFormat = sourceAIS.getFormat
    val targetFormat = new AudioFormat(
      AudioFormat.Encoding.PCM_SIGNED,
      sourceFormat.getSampleRate,
      16,
      sourceFormat.getChannels,
      sourceFormat.getChannels * 2,
      sourceFormat.getSampleRate,
      false
    )
    val decodedAIS: F[jio.InputStream] = F.delay(
      AudioSystem.getAudioInputStream(targetFormat, sourceAIS))

    val stream = fs2.io.readInputStream[F](
      decodedAIS, bufSize, closeAfterUse = true)

    (stream, targetFormat)
  }

  def sourceDataLine(targetFormat: AudioFormat): Sink[IO, Byte] =
    writeSourceDataLine(
      IO {
        val line = AudioSystem.getSourceDataLine(targetFormat)
        line.open(targetFormat)
        line.start()
        line
      })
}
