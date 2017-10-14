import sys

if sys.platform == 'linux':
    from channel.ui.unix.application import start_app
else:
    raise RuntimeError('Unsupported platform: %s' % sys.platform)

__all__ = ['start_app']
