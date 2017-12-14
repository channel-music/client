from collections import namedtuple
import functools
import logging

import gi
gi.require_version('Gst', '1.0')  # noqa
from gi.repository import Gst, GLib

from channel.media import play_queue as pq
from channel.media.stream import AudioStreamer, StreamState

logger = logging.getLogger(__name__)


Song = namedtuple('Song', ('id', 'title', 'album', 'artist',
                           'genre', 'track', 'file'))


class Player:
    def __init__(self, looping=False):
        """An abstraction over playing music allowing standard music player
        functionality.

        looping: True if tracks should loop after exhausted, defaults to False
        """
        self.is_looping = looping
        self._play_queue = pq.PlayQueue()

        self._streamer = AudioStreamer()
        self._streamer.on_stream_ended(self._on_finished)

    @property
    def is_playing(self):
        """True if the player is currently playing audio."""
        return self._streamer.state is StreamState.PLAYING

    @property
    def current_track(self):
        """The current playing track."""
        return self._play_queue.current

    def _on_finished(self, _):
        self._queue_next()

    def _queue_next(self):
        try:
            self._play_queue.next()
        except pq.Exhausted:
            if not self.is_looping:
                return
            self._play_queue.reset()
        self._streamer.queue(self.current_track.file)

    def queue(self, song):
        """Add a song on to the end of the play queue."""
        self._play_queue.append(song)

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
        assert self._streamer.state is not StreamState.PLAYING

        self._streamer.queue(self.current_track.file)
        self._streamer.start()

    def stop(self):
        """Stop playback of the current song.

        This will work even if the player is currently paused or stopped.
        """
        self._streamer.stop()

    def pause(self):
        """Pause playback of the current song.

        Starting playback again will result in the song continuing from
        the previous point. This will work even if the player is currently
        paused or stopped.
        """
        self._streamer.pause()

    def jump_to(self, song):
        """Jump player to a given song.

        Will stop the current song and move to the new one. The play queue will
        be reorganized.
        """
        self.stop()
        self._play_queue.jump_to(song)
        self.play()

    def _ensure_stopped(self):
        if self._streamer.state is StreamState.PLAYING:
            self.stop()

    @property
    def position(self):
        """Return the current track position as a percentage of current position over
        the total duration."""
        try:
            return self._streamer.position / self._streamer.duration
        except ZeroDivisionError:
            return 0

    @position.setter
    def position(self, value):
        """Set the position of the current track as a pecentage of position over
        total duration."""
        if 0.0 <= value <= 1.0:
            self._streamer.seek(self._streamer.duration * value)
        else:
            msg = '`value` must be between 0 and 1, got `{!g}`'
            raise ValueError(msg.format(value))
