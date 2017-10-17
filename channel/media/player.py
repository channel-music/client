import enum
import functools
import logging

import gi
gi.require_version('Gst', '1.0')  # noqa
from gi.repository import Gst, GLib

from channel.media import play_queue as pq

logger = logging.getLogger(__name__)


def gstreamer_version_str():
    """Return The GStreamer version as a string."""
    return '%d.%d.%d.%d' % Gst.version()


def init_gstreamer():
    """Initialize the GStreamer library."""
    logger.debug('Initializing gstreamer...')
    Gst.init_check(None)
    logger.debug('GStreamer version: %s' % gstreamer_version_str())



class Player:
    def __init__(self, looping=False):
        """An abstraction over playing music allowing standard music player
        functionality.

        looping: True if tracks should loop after exhausted, defaults to False
        """
        self.is_looping = looping
        self._play_queue = pq.PlayQueue()


        # Initialize Gstreamer
        init_gstreamer()
        self._player = Gst.ElementFactory.make('playbin', 'player')
        self._player.connect('about-to-finish', self._on_finished)
        self._player.bus.connect('message', self._on_bus_message)

    def on_song_progress(self, callback):
        """Accept a callback to be called when a song progresses.

        The callback will only be called when the song is actually playing,
        not when it is stopped or paused.
        """
        @functools.wraps(callback)
        def wrapper():
            if self.is_playing:
                callback(self, self.position)
            return True
        # TODO: handle removal and rescheduling
        # FIXME: abstract away Gtk stuff
        GLib.timeout_add(250, wrapper)

    def _on_bus_message(self, bus, message):
        if message.type == Gst.Message.EOS:
            logger.debug('Bus message: EOS')
            self.next_track()
        elif message.type == Gst.Message.ERROR:
            self.stop()
            err, debug = message.parse_error()
            logger.error('GStreamer Bus Error: %s - %s' % (err, debug))

    def _on_finished(self, player):
        self._queue_next()

    def _queue_next(self):
        try:
            self._play_queue.next()
        except pq.Exhausted:
            if not self.is_looping:
                return
            self._play_queue.reset()
        self._player.set_property('uri', self._play_queue.current.file)

    def queue(self, song):
        """Add a song on to the end of the play queue."""
        self._play_queue.append(song)

    @property
    def is_playing(self):
        return self._player.get_state() == Gst.State.PLAYING

    @property
    def current_track(self):
        """Return the current playing track."""
        return self._play_queue.current

    def next_track(self):
        """Stop the current song and play the next one in the queue.

        If there are no more items in the queue the player will stop,
        unless `looping` is set to true, in which case it will start
        again from the beginning.
        """
        self._ensure_stopped()
        self._queue_next()
        self.play()

    # FIXME: don't go to previous song after N seconds
    def previous_track(self):
        """Stop the current song and play the previous one in the queue.

        If there are no more previous items the song will be started from
        the beginning.
        """
        self._ensure_stopped()

        try:
            self._play_queue.previous()
        except pq.Exhausted:
            pass
        # Just replay the same song if exhausted
        self.play()

    def play(self):
        """Begin playback of the current song.

        An `AssertionError` will be thrown if the player is already
        playing.
        """
        assert self._player.get_state is not Gst.State.PLAYING

        self._player.set_property('uri', self._play_queue.current.file)
        self._player.set_state(Gst.State.PLAYING)

    def stop(self):
        """Stop playback of the current song.

        This will work even if the player is currently paused or stopped.
        """
        self._player.set_state(Gst.State.NULL)

    def pause(self):
        """Pause playback of the current song.

        Starting playback again will result in the song continuing from
        the previous point. This will work even if the player is currently
        paused or stopped.
        """
        self._player.set_state(Gst.State.PAUSED)

    def jump_to(self, song):
        """Jump player to a given song.

        Will stop the current song and move to the new one. The play queue will
        be reorganized.
        """
        self.stop()
        self._play_queue.jump_to(song)
        self.play()

    def _ensure_stopped(self):
        if self._player.get_state() is Gst.State.PLAYING:
            self.stop()

    @property
    def position(self):
        """Return the current track position as a percentage of current position over
        the total duration."""
        _, current_time = self._player.query_position(Gst.Format.TIME)
        _, duration = self._player.query_duration(Gst.Format.TIME)
        try:
            return current_time / duration
        except ZeroDivisionError:
            return 0

    @position.setter
    def position(self, value):
        """Set the position of the current track as a pecentage of position over
        total duration."""
        _, duration = self._player.query_duration(Gst.Format.Time)
        position = duration * value
        assert position <= 1.0

        self._player.seek_simple(
            Gst.Format.PERCENT,
            Gst.SeekFlags.KEY_UNIT,
            position
        )
