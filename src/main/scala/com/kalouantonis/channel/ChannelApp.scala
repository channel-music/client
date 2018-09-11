package com.kalouantonis.channel

import java.io.File

import scala.collection.JavaConverters._

import javafx.application.Application
import javafx.beans.value.ObservableValue
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.stage.Stage
import javafx.scene.{Parent, Scene}
import javafx.scene.control.{Button, TableColumn, TableView, TableRow}
import javafx.scene.input.MouseButton
import javafx.scene.layout.{HBox, VBox}

import cats.effect.IO
import com.kalouantonis.channel.media.Track

object Views {
  def trackListView(tracks: Seq[Track]): TableView[Track] = {
    val table = new TableView[Track]()
    table.getColumns.addAll(
      column("Title", track => new SimpleStringProperty(track.title)),
      column("Album", track => new SimpleStringProperty(track.album)),
      column("Artist", track => new SimpleStringProperty(track.artist)))
    table.setEditable(false)
    table.setItems(FXCollections.observableArrayList(tracks.asJava))
    table
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

// NOTE
//
// No one until fucking JAVA 9!!!!! decided to allow starting
// an application by passing it NORMAL parameters, so we use this
// as the entry point.
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
    val root = loadUI(primaryStage, config).unsafeRunSync()
    primaryStage.setTitle(FullAppName)
    primaryStage.setWidth(1260)
    primaryStage.setHeight(860)
    primaryStage.setScene(new Scene(root))
    primaryStage.show()
  }

  def loadUI(primaryStage: Stage, config: Config): IO[Parent] = IO {
    val availableListView = Views.trackListView(availableTracks)
    availableListView.setRowFactory { _ =>
      val row = new TableRow[Track]
      row.setOnMouseClicked { ev =>
        if (ev.getButton == MouseButton.PRIMARY && ev.getClickCount == 2) {
          println(s"Double clicked: ${row.getItem}")
        }
      }
      row
    }

    val hbox = new HBox()
    hbox.getChildren.addAll(
      availableListView,
      Views.trackListView(queuedTracks))

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
