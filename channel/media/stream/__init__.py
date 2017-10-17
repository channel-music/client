import sys

if sys.platform == 'linux' or sys.platform == 'darwin':
    from channel.media.stream.gstreamer import AudioStreamer
else:
    raise RuntimeError('Unsupported platform: %s' % sys.platform)

from channel.media.stream.common import StreamState


__all__ = [AudioStreamer, StreamState]
