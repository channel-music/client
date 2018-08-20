package com.kalouantonis.channel

import java.util.concurrent.atomic.AtomicReference

import scala.collection.mutable

import javafx.scene.input.{MouseButton, MouseEvent}
import scalafx.application.JFXApp
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.Scene
import scalafx.scene.control.{TableColumn, TableView, TableRow}
import scalafx.scene.layout.{HBox, Priority}
import scalafx.scene.media.{Media, MediaPlayer}

import com.kalouantonis.channel.media.PlayQueue

class Player(tracks: IndexedSeq[Track]) {
  // FIXME: vars "everywhere"
  private var playQueue = PlayQueue(tracks)
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
      currentStream = Some(player)
    }
  }

  def pause(): Unit = {
    currentStream.foreach(_.pause())
  }

  def stop(): Unit = {
    currentStream.foreach(_.stop())
  }

  def clear(): Unit = {
    playQueue = PlayQueue.empty
    currentStream = None
  }
}

// This is the Track used for UI state, not necessarily player state
case class Track(
    id: Int,
    title: String,
    album: String,
    artist: String,
    uri: java.net.URI)

// FIXME: this is highly coupled to the Track structure
case class StatefulTrack(initialState: Track) {
  // FIXME: i dont even want to mutate this stuff!
  val title = new StringProperty(this, "title", initialState.title)
  val album = new StringProperty(this, "album", initialState.album)
  val artist = new StringProperty(this, "artist", initialState.artist)
}

class TrackListView(tracks: ObservableBuffer[StatefulTrack]) {
  var clickHandlers: mutable.ArrayBuffer[StatefulTrack => Unit] =
    mutable.ArrayBuffer()

  val root: TableView[StatefulTrack] = {
    val titleCol = new TableColumn[StatefulTrack, String] {
      text = "Title"
      cellValueFactory = { _.value.title }
    }
    val albumCol = new TableColumn[StatefulTrack, String] {
      text = "Album"
      cellValueFactory = { _.value.album }
    }
    val artistCol = new TableColumn[StatefulTrack, String] {
      text = "Artist"
      cellValueFactory = { _.value.artist }
    }

    new TableView[StatefulTrack](tracks) {
      columns += (titleCol, albumCol, artistCol)
      // Expand to parent
      hgrow = Priority.Always
      rowFactory = { _ =>
        val row: TableRow[StatefulTrack] = new TableRow()
        row.onMouseClicked = { ev =>
          if (isDoubleClick(ev) && !row.isEmpty) {
            clickHandlers.foreach(_(row.getItem))
          }
        }
        row
      }
    }
  }

  def onDoubleClicked(f: StatefulTrack => Unit): Unit = {
    clickHandlers += f
  }

  private def isDoubleClick(event: MouseEvent): Boolean =
    event.getButton == MouseButton.PRIMARY && event.getClickCount == 2

}

object ChannelApp extends JFXApp {
  private val player = new Player(IndexedSeq())

  val availableTracks: ObservableBuffer[StatefulTrack] =
    ObservableBuffer(
      getDirectoryFiles("/home/slacker/Music")
        .zipWithIndex
        .map{
          case (file, idx) =>
            new StatefulTrack(Track(idx, s"test $idx", "Unknown", "Unknown", file.toURI))
        })

  val queuedTracks = new ObservableBuffer[StatefulTrack]()

  stage = new JFXApp.PrimaryStage {
    title.value = "Channel"
    width = 1024
    height = 740
    scene = new Scene {
      root = new HBox {
        val availableTrackList = new TrackListView(availableTracks)
        availableTrackList.onDoubleClicked { track =>
          queuedTracks.append(track)
        }
        val queuedTrackList = new TrackListView(queuedTracks)
        queuedTrackList.onDoubleClicked { track =>
          println(s"Playing track: $track")
          playAudio(track.initialState.uri)
        }

        children = Seq(
          availableTrackList.root,
          queuedTrackList.root
        )
      }
    }
  }

  def playAudio(uri: java.net.URI): Unit = {
    player.clear()
    player.queue(Track(id = 0, title = "test", album = "test", artist = "test", uri = uri))
    player.play()
  }

  def getDirectoryFiles(path: String): List[java.io.File] = {
    val directory = new java.io.File(path)
    val files = directory.listFiles
    files.filter(_.isFile).toList
  }
}
