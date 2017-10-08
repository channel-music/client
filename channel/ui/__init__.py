# FIXME: only do for unix
from channel.ui.unix.application import Application
from channel.ui.unix.threading import GLibThreadPoolExecutor

def start_app(argv, max_workers=5):
    with GLibThreadPoolExecutor(max_workers=max_workers) as pool:
        app = Application(thread_pool=pool)
        app.run(argv)
