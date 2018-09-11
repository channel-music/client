package com.kalouantonis.channel.media

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.tag.FieldKey

// This is the Track used for UI state, not necessarily player state
case class Track(
  title: String,
  album: String,
  artist: String,
  uri: java.net.URI
)

object Track {
  def fromFile(file: java.io.File): Option[Track] = {
    val tag =
      try {
        Some(AudioFileIO.read(file).getTag)
      } catch {
        case _: CannotReadException =>
          None
      }

    tag.map(tag =>
      Track(
        title = Option(tag.getFirst(FieldKey.TITLE))
          .filter(_.nonEmpty)
          .getOrElse(file.getName),
        album = Option(tag.getFirst(FieldKey.ALBUM))
          .filter(_.nonEmpty)
          .getOrElse("Unknown"),
        artist = Option(tag.getFirst(FieldKey.ARTIST))
          .filter(_.nonEmpty)
          .getOrElse("Unknown"),
        uri = file.toURI))
  }

  sealed trait Event
  case class Selected(item: Track) extends Event
}
