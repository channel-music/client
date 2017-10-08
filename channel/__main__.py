import logging
import sys

from channel.ui.unix.application import Application
from channel.ui.unix.threading import GLibThreadPoolExecutor


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    with GLibThreadPoolExecutor(max_workers=5) as pool:
        app = Application(thread_pool=pool)
        app.run(sys.argv)
