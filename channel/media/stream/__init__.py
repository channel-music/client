import sys

if sys.platform == 'linux' or sys.platform == 'darwin':
    from channel.media.stream.gstreamer import AudioStreamer, StreamState
else:
    raise RuntimeError('Unsupported platform: %s' % sys.platform)

# TODO: move SteamState out of gstreamer
__all__ = [AudioStreamer, StreamState]
