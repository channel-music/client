import concurrent.futures

from gi.repository import GLib


class GLibThreadPoolExecutor(concurrent.futures.ThreadPoolExecutor):
    """Thread pool executor that adds done callbacks on to the Gtk
    main event loop to be executed on idle."""

    def add_done_callback(self, func):
        def wrapper(*args, **kwargs):
            GLib.idle_add(func, *args, **kwargs)
        return super().add_done_callback(wrapper)
