import enum
import logging

import gi
gi.require_version('Gst', '1.0')  # noqa
from gi.repository import Gst


logger = logging.getLogger(__name__)


def gstreamer_version_str():
    """Return The GStreamer version as a string."""
    return '%d.%d.%d.%d' % Gst.version()


def init_gstreamer():
    """Initialize the GStreamer library."""
    logger.debug('Initializing gstreamer...')
    Gst.init_check(None)
    logger.debug('GStreamer version: %s' % gstreamer_version_str())


class StreamState(enum.Enum):
    PLAYING = 0
    PAUSED = 1
    STOPPED = 2

    @classmethod
    def from_gst_state(cls, gst_state):
        conversions = {
            Gst.State.PLAYING: cls.PLAYING,
            Gst.State.PAUSED: cls.PAUSED,
            Gst.State.NULL: cls.STOPPED,
        }
        _, current_state, _ = gst_state
        try:
            return conversions[current_state]
        except KeyError:
            raise ValueError('Unsupported Gst.State: {}'.format(gst_state))


class AudioStreamer:
    """Streaming music from a remote or local source.

    Abstracts away the internals of working with audio playback
    libraries and allows basic calls to play, pause, stop and
    queue audio streams.
    """

    def __init__(self):
        # FIXME: should this be done here?
        init_gstreamer()
        self._playbin = Gst.ElementFactory.make('playbin', 'player')
        self._playbin.bus.connect('message', self._on_bus_message)

    @property
    def state(self):
        """Return the current state of the streamer as a `StreamState` enum."""
        gst_state = self._playbin.get_state(Gst.CLOCK_TIME_NONE)
        return StreamState.from_gst_state(gst_state)

    def _on_bus_message(self, bus, message):
        if message.type == Gst.Message.EOS:
            logger.debug('Bus message: EOS')
            self.next_track()
        elif message.type == Gst.Message.ERROR:
            self.stop()
            err, debug = message.parse_error()
            logger.error('GStreamer Bus Error: %s - %s' % (err, debug))

    def on_stream_ended(self, callback):
        # FIXME: handle unscheduling
        self._playbin.connect('about-to-finish', callback)

    def queue(self, uri):
        """Queue a uri to be streamed next.

        If the streamer is already streaming the stream will be played
        once it finishes, effectively "moving" to the next stream.
        """
        self._playbin.set_property('uri', uri)

    def start(self):
        """Start the audio stream."""
        self._playbin.set_state(Gst.State.PLAYING)

    def stop(self):
        """Stop the audio stream."""
        self._playbin.set_state(Gst.State.NULL)

    def pause(self):
        """Pause the audio stream.

        The stream will continue where it left off.
        """
        self._playbin.set_state(Gst.State.PAUSED)

    @property
    def position(self):
        """Return the current track position as nanoseconds from the
        start time.

        Could be zero.
        """
        return self._playbin.query_position(Gst.Format.TIME)[1]

    @property
    def duration(self):
        """Return the total duration of the stream in nanoseconds.

        Could be zero.
        """
        return self._playbin.query_duration(Gst.Format.TIME)[1]

    def seek(self, position):
        """Set the position of the stream in nanoseconds from the beginning.

        Will raise an `AssertionError` if the position is smaller than 0 or
        larger than the stream's total duration.
        """
        assert 0.0 <= position <= self.duration
        self._playbin.seek(
            Gst.Format.TIME,
            Gst.SeekFlags.KEY_UNIT,
            position
        )
