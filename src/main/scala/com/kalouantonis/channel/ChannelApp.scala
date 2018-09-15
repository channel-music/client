package com.kalouantonis.channel

import java.io.File

import javafx.application.{Application, Platform}
import javafx.beans.value.ObservableValue
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.stage.Stage
import javafx.scene.{Parent, Scene}
import javafx.scene.control.{Button, TableColumn, TableView, TableRow}
import javafx.scene.input.{MouseButton, MouseEvent}
import javafx.scene.layout.{HBox, VBox}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

import cats.effect.IO
import com.kalouantonis.channel.media.Track
import monix.execution.Scheduler
import monix.reactive.Observable
import monix.execution.Cancelable
import monix.execution.cancelables.SingleAssignmentCancelable
import monix.reactive.Observable
import monix.reactive.OverflowStrategy.Unbounded

object schedulers {
  val JavaFx = Scheduler(
    new ExecutionContext {
      override def execute(runnable: Runnable): Unit =
        Platform.runLater(runnable)

      override def reportFailure(t: Throwable): Unit =
        sys.error(t.getMessage())
    })

  object Implicits {
    implicit val JavaFx = schedulers.JavaFx
  }
}

object Views {
  def trackList(tracks: Seq[Track]): TableView[Track] = {
    val delegate = new TableView[Track]()
    delegate.getColumns.addAll(
      column("Title", track => new SimpleStringProperty(track.title)),
      column("Album", track => new SimpleStringProperty(track.album)),
      column("Artist", track => new SimpleStringProperty(track.artist)))
    delegate.setEditable(false)
    delegate
  }

  private def column[Item, T](
      title: String,
      valueFactory: Item => ObservableValue[T]
  ): TableColumn[Item, T] = {
    val col = new TableColumn[Item, T](title)
    col.setCellValueFactory(cell => valueFactory(cell.getValue))
    col
  }
}

object Observables {
}

// NOTE
//
// Jdk8 does not support starting an application by passing it NORMAL parameters,
// so we use this as the entry point for now.
//
// On the plus side, it makes lifecycle management a bit simpler in regards
// to running on a generic "platform" (e.g. mobile or desktop).
final class ChannelApp extends Application {
  import ChannelApp._

  var availableTracks: Seq[Track] = Seq(
    Track("N.I.B", "Black Sabbath", "Black Sabbath", new File("song.mp3").toURI),
    Track("Black Sabbath", "Black Sabbath", "Black Sabbath", new File("song.wav").toURI))
  var queuedTracks: Seq[Track] = Seq()

  // This is basically the new main, is there a pre-stage initialization part? more callbacks?
  override def start(primaryStage: Stage): Unit = {
    // TODO
    val config = ChannelApp.loadConfig().unsafeRunSync()
    val root = loadUI(primaryStage, config)
    primaryStage.setTitle(FullAppName)
    primaryStage.setWidth(1260)
    primaryStage.setHeight(860)
    primaryStage.setScene(new Scene(root))
    primaryStage.show()
  }

  def tableSelections(
      table: TableView[Track])(
      implicit scheduler: Scheduler
  ): Observable[Track] =
    Observable.create(Unbounded) { subscriber =>
      def isDoubleClick(ev: MouseEvent): Boolean =
        ev.getButton == MouseButton.PRIMARY && ev.getClickCount == 2

      val c = SingleAssignmentCancelable()
      table.setRowFactory { _ =>
        val row = new TableRow[Track]
        // TODO: should I keep track of handlers?
        row.setOnMouseClicked { ev =>
          if (isDoubleClick(ev) && row.getItem != null) {
            subscriber
              .onNext(row.getItem)
              .syncOnStopOrFailure(_ => c.cancel())
          }
        }
        row
      }
      c := Cancelable(() => table.setRowFactory(null))
    }

  def loadUI(primaryStage: Stage, config: Config): Parent = {
    val availableListView = Views.trackList(availableTracks)
    val queuedListView = Views.trackList(queuedTracks)

    implicit val jfxScheduler = schedulers.JavaFx
    tableSelections(availableListView)
      .dump("O")
      .subscribe()

    availableListView.setItems(
      FXCollections.observableList(availableTracks.asJava))

    // TODO: produce a observable over the mouse click events
    val hbox = new HBox()
    hbox.getChildren.addAll(availableListView, queuedListView)

    val vbox = new VBox()
    vbox.getChildren.addAll(
      hbox,
      new Button("Play/Pause"))
    vbox
  }
}

object ChannelApp {
  val FullAppName: String = "Channel"

  case class Config()

  def loadConfig(): IO[Config] = IO(Config())

  def main(args: Array[String]): Unit = {
    // FIXME: we can't pass anything but string args to Application, so it'll
    //        have to load configuration in a stateful maner.
    Application.launch(classOf[ChannelApp], args: _*)
  }
}
