package com.kalouantonis.channel

import java.io.File

import com.kalouantonis.channel.media.Track
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.beans.property.StringProperty
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableBuffer
import scalafx.scene.{Scene, Parent}
import scalafx.scene.control.{Button, TableColumn, TableView}
import scalafx.scene.layout.{HBox, GridPane}

// object schedulers {
//   val JavaFx = Scheduler(
//     new ExecutionContext {
//       override def execute(runnable: Runnable): Unit =
//         Platform.runLater(runnable)

//       override def reportFailure(t: Throwable): Unit =
//         sys.error(t.getMessage())
//     })

//   object Implicits {
//     implicit val JavaFx = schedulers.JavaFx
//   }
// }

object ChannelApp extends JFXApp {
  val availableTracks: Seq[Track] = Seq(
    Track("N.I.B", "Black Sabbath", "Black Sabbath", new File("song.mp3").toURI),
    Track("Black Sabbath", "Black Sabbath", "Black Sabbath", new File("song.wav").toURI))
  val queuedTracks: Seq[Track] = availableTracks.headOption.toSeq

  sealed trait Msg
  case class Queue(track: Track) extends Msg
  case object Play extends Msg
  case object Pause extends Msg
  case object Stop extends Msg

  case class Model(availableTracks: Seq[Track], queuedTracks: Seq[Track])

  object MainWidget extends Elm.Widget[Parent, Model, Msg] {
    private val mediaPanel: Parent =
      new HBox {
        children = Seq(
          new Button("Previous"),
          new Button("Play"),
          new Button("Pause"),
          new Button("Stop"),
          new Button("Next"))
      }

    private def trackList(tracks: Seq[Track]): TableView[Track] =
      new TableView[Track] {
        columns.addAll(
          column[Track, String]("Title", track => StringProperty(track.title)),
          column[Track, String]("Album", track => StringProperty(track.album)),
          column[Track, String]("Artist", track => StringProperty(track.artist)))
        editable = false
        items = ObservableBuffer(tracks)
      }

    private def column[Item, T](
      title: String,
      valueFactory: Item => ObservableValue[T, T]
    ): TableColumn[Item, T] = new TableColumn[Item, T] {
      cellValueFactory = (cell => valueFactory(cell.getValue))
    }

    override def view(model: Model): Elm.View[Parent, Msg] =
      new Elm.View[Parent, Msg]({ () =>
        new GridPane {
          add(trackList(model.availableTracks), 0, 0)
          add(trackList(model.queuedTracks), 1, 0)
          add(mediaPanel, 0, 1)
        }
      })
  }

  stage = new JFXApp.PrimaryStage {
    val initialModel = new Model(availableTracks, queuedTracks)

    scene = new Scene {
      content = MainWidget.view(initialModel).root
    }
  }
}
