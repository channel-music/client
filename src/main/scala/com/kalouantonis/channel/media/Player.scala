package com.kalouantonis.channel.media

import scalafx.scene.media.{Media, MediaPlayer}

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

  def isPlaying: Boolean =
    currentStream.exists(_.status == MediaPlayer.Status.Playing)

  def isPaused: Boolean =
    currentStream.exists(_.status == MediaPlayer.Status.Paused)

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
