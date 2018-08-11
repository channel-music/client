package kalouantonis.channel.media.decoder

import javax.sound.sampled.AudioFormat

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
