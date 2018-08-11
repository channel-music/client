package kalouantonis.channel.media.decoder

import javax.sound.sampled.{AudioInputStream, AudioFormat}
import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider

class MP3Decoder(inputStream: AudioInputStream) extends Decoder {

  private[this] val targetFormat = new AudioFormat(
    AudioFormat.Encoding.PCM_SIGNED,
    inputStream.getFormat.getSampleRate,
    16,
    inputStream.getFormat.getChannels,
    inputStream.getFormat.getChannels * 2,
    inputStream.getFormat.getSampleRate,
    false
  )

  private[this] val mpegInputStream = new MpegFormatConversionProvider()
    .getAudioInputStream(targetFormat, inputStream)

  def open(): Unit = { /* do nothing */ }

  def close(): Unit = {
    mpegInputStream.close()
    // FIXME: the decoder is taking ownership of the stream, is this correct?
    inputStream.close()
  }

  def read(pcmBuf: Array[Byte]): Int = {
    mpegInputStream.read(pcmBuf)
  }

  def audioFormat: AudioFormat = mpegInputStream.getFormat
}
