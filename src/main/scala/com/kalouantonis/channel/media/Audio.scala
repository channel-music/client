package kalouantonis.channel.media

import java.{io => jio}
import javax.sound.sampled.{AudioFormat, AudioSystem, SourceDataLine}

import cats.effect.{IO, Sync}
import fs2.{Stream, Pipe, Chunk, Sink}

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

  def targetFormat(sourceFormat: AudioFormat): AudioFormat =
    new AudioFormat(
      AudioFormat.Encoding.PCM_SIGNED,
      sourceFormat.getSampleRate,
      16,
      sourceFormat.getChannels,
      sourceFormat.getChannels * 2,
      sourceFormat.getSampleRate,
      false
    )

  def sourceDataLineSink(targetFormat: AudioFormat): Sink[IO, Byte] =
    writeSourceDataLine(
      IO {
        val line = AudioSystem.getSourceDataLine(targetFormat)
        line.open(targetFormat)
        line.start()
        line
      })

  def mp3Decoder[F[_]](targetFormat: AudioFormat)(
    implicit F: Sync[F]): Pipe[F, Byte, Byte] = {
    def sinkByteChunk(byteChunk: Chunk[Byte])(
      implicit F: Sync[F]): Stream[F, Byte] = {
      // FIXME: How efficient is it to create these each time?
      val byteIn = new jio.ByteArrayInputStream(byteChunk.toArray)
      val sourceAIS = AudioSystem.getAudioInputStream(byteIn)
      val decodedAIS = AudioSystem.getAudioInputStream(targetFormat, sourceAIS)

      fs2.io.readInputStream(
        F.delay(decodedAIS : jio.InputStream),
        byteChunk.size,
        closeAfterUse = true)
    }

    s => s.chunks flatMap sinkByteChunk
  }
}
