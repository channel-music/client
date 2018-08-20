package com.kalouantonis.channel

import scala.collection.mutable

import javafx.scene.input.{MouseButton, MouseEvent}
import scalafx.application.JFXApp
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.Scene
import scalafx.scene.media.{Media, MediaPlayer}
import scalafx.scene.control.{TableView, TableColumn, TableRow}
import scalafx.scene.layout.{HBox, Priority}

import com.kalouantonis.channel.media.PlayQueue
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.tag.FieldKey

// FIXME: are these API's thread-safe?
class Player(tracks: IndexedSeq[Track]) {
  // FIXME: vars "everywhere"
  private var playQueue: PlayQueue[Track] = PlayQueue(tracks)
  private var currentStream: Option[MediaPlayer] = None

  def queue(track: Track): Unit = {
    playQueue = playQueue.append(track)
  }

  def play(): Unit = {
    currentStream.foreach(_.stop())

    playQueue.current.foreach { track =>
      val media = new Media(track.uri.toString)
      // FIXME: is this all completely thread safe?
      val player = new MediaPlayer(media)
      player.play()
      player.onEndOfMedia = { next() }
      currentStream = Some(player)
    }
  }

  def pause(): Unit = {
    currentStream.foreach(_.pause())
  }

  def next(): Unit = {
    if (playQueue.hasNext) {
      stop()
      this.playQueue = playQueue.next
      play()
    }
  }

  def previous(): Unit = {
    if (playQueue.hasPrevious) {
      stop()
      this.playQueue = playQueue.previous
      play()
    }
  }

  def stop(): Unit = {
    currentStream.foreach(_.stop())
  }

  def jumpTo(track: Track): Boolean =
    playQueue.jumpTo(track) map { playQueue =>
      this.playQueue = playQueue
      stop()
      play()
      true
    } getOrElse false


  def clear(): Unit = {
    playQueue = PlayQueue.empty
    currentStream = None
  }
}

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
          .filter(_.nonEmpty).getOrElse(file.getName),
        album = Option(tag.getFirst(FieldKey.ALBUM))
          .filter(_.nonEmpty).getOrElse("Unknown"),
        artist = Option(tag.getFirst(FieldKey.ARTIST))
          .filter(_.nonEmpty).getOrElse("Unknown"),
        uri = file.toURI))
  }
}

class TrackListView(tracks: ObservableBuffer[Track]) {
  var clickHandlers: mutable.ArrayBuffer[Track => Unit] =
    mutable.ArrayBuffer()

  val root: TableView[Track] = {
    val titleCol = new TableColumn[Track, String] {
      text = "Title"
      cellValueFactory = (cell => StringProperty(cell.value.title))
    }
    val albumCol = new TableColumn[Track, String] {
      text = "Album"
      cellValueFactory = (cell => StringProperty(cell.value.album))
    }
    val artistCol = new TableColumn[Track, String] {
      text = "Artist"
      cellValueFactory = (cell => StringProperty(cell.value.artist))
    }

    new TableView[Track](tracks) {
      columns += (titleCol, albumCol, artistCol)
      // Expand to parent
      hgrow = Priority.Always
      rowFactory = { _ =>
        val row: TableRow[Track] = new TableRow()
        row.onMouseClicked = { ev =>
          if (isDoubleClick(ev) && !row.isEmpty) {
            clickHandlers.foreach(_(row.getItem))
          }
        }
        row
      }
    }
  }

  def onDoubleClicked(f: Track => Unit): Unit = {
    clickHandlers += f
  }

  private def isDoubleClick(event: MouseEvent): Boolean =
    event.getButton == MouseButton.PRIMARY && event.getClickCount == 2
}

object ChannelApp extends JFXApp {
  private val player = new Player(IndexedSeq())

  val availableTracks: ObservableBuffer[Track] =
    ObservableBuffer(
      getDirectoryFiles("/home/slacker/Music")
        .map(Track.fromFile)
        .collect {
          // FIXME
          case Some(track) => track
        })

  val queuedTracks = new ObservableBuffer[Track]()

  stage = new JFXApp.PrimaryStage {
    title.value = "Channel"
    width = 1024
    height = 740
    scene = new Scene {
      root = new HBox {
        val availableTrackList = new TrackListView(availableTracks)
        availableTrackList.onDoubleClicked { track =>
          queuedTracks.append(track)
          player.queue(track)
        }
        val queuedTrackList = new TrackListView(queuedTracks)
        queuedTrackList.onDoubleClicked { track =>
          println(s"Playing track: $track")
          player.jumpTo(track)
        }

        children = Seq(
          availableTrackList.root,
          queuedTrackList.root
        )
      }
    }
  }

  def getDirectoryFiles(path: String): List[java.io.File] = {
    val directory = new java.io.File(path)
    val files = directory.listFiles
    files.flatMap { file =>
      if (file.isFile)
        Seq(file)
      else
        getDirectoryFiles(file.getPath)
    }.toList
  }
}
