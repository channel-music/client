import logging
import sys

from channel.ui import start_app


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    start_app(sys.argv)
