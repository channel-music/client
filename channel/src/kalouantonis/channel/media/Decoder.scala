package kalouantonis.channel.media

import javax.sound.sampled.{AudioFormat, AudioInputStream}

trait Decoder {
  /**
    * Open the decoder for reading.
    */
  def open(): Unit

  /**
    * Close the decoder and any provided input streams.
    */
  def close(): Unit

  /**
    * Decode the stream in to the given buffer as raw PCM data.
    *
    * FIXME: allow specifying offset and size
    */
  def read(pcmBuf: Array[Byte]): Int

  /**
    * Return the audio format of the file.
    */
  def audioFormat: AudioFormat
}

class MP3Decoder(inputStream: AudioInputStream) extends Decoder {
  import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider

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
