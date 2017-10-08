import concurrent.futures
import logging
import sys

import gi
gi.require_version('Gtk', '3.0')  # noqa
from gi.repository import Gdk, Gtk, GLib, Gio

from channel import api, media


logger = logging.getLogger(__name__)


class GLibThreadPoolExecutor(concurrent.futures.ThreadPoolExecutor):
    """Thread pool executor that adds done callbacks on to the Gtk
    main event loop to be executed on idle."""

    def add_done_callback(self, func):
        def wrapper(*args, **kwargs):
            GLib.idle_add(func, *args, **kwargs)
        return super().add_done_callback(wrapper)


def add_treeview_column(treeview, text, index=0):
    renderer = Gtk.CellRendererText()
    column = Gtk.TreeViewColumn(text, renderer, text=index)
    treeview.append_column(column)


# Not really appropriate to inherit from, but whatever
class SongListView:
    def __init__(self, displayed_fields, hidden_fields=None):
        hidden_fields = hidden_fields or []
        self._callbacks = {}
        self._list_store = Gtk.ListStore(*field_types)
        self._tree_view = Gtk.TreeView(model=self._list_store)

        self.tree_view.connect('button-press-event', self._on_cell_clicked)

        for field in displayed_fields:
            add_treeview_column(
                self._tree_view,
                field['display_name'],
                idx + len(hidden_fields)
            )

    def append(self, song):
        attrs = [a for _, a in self.hidden_fields]
        attrs += [a for _, _, a in self.shown_fields]
        row_data = [getattr(song, a) for a in attrs]
        self._list_store.append(row_data)

    def connect(self, event_type, callback):
        self._callbacks[event_type] = callback

    def _on_cell_clicked(self, treeview, event):
        try:
            callback = self._callbacks['double-clicked']
        except KeyError:
            pass
        else:
            if event.type == Gdk.EventType._2BUTTON_PRESS:
                path, _, _, _ = song_list.get_path_at_pos(event.x, event.y)
                song_id = song_list.get_model()[path][0]
                logger.info('Song selected: %r' % song_id)


class MusicActionBar(Gtk.ActionBar):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

        self.prev_button = Gtk.Button.new_from_stock(Gtk.STOCK_MEDIA_PREVIOUS)
        self.play_button = Gtk.Button.new_from_stock(Gtk.STOCK_MEDIA_PLAY)
        self.next_button = Gtk.Button.new_from_stock(Gtk.STOCK_MEDIA_NEXT)
        self.progress_bar = Gtk.ProgressBar()

        self.pack_start(self.prev_button)
        self.pack_start(self.play_button)
        self.pack_start(self.next_button)
        self.pack_start(self.progress_bar)


class ApplicationWindow(Gtk.ApplicationWindow):
    def __init__(self, thread_pool=None, **kwargs):
        super().__init__(**kwargs)
        self.thread_pool = thread_pool
        self.player = media.Player()

        self.song_list = SongListView()
        self.song_list.connect('double-clicked', self.on_song_double_clicked)

        scrolled = Gtk.ScrolledWindow()
        # FIXME
        scrolled.add(self.song_list)

        actionbar = MusicActionBar()
        actionbar.play_button.connect('clicked', self.on_play_clicked)
        actionbar.next_button.connect('clicked', self.on_next_clicked)
        actionbar.prev_button.connect('clicked', self.on_prev_clicked)

        box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL,
                      spacing=5, border_width=12)
        box.pack_start(scrolled, True, True, 0)
        box.pack_start(actionbar, False, True, 2)
        self.add(box)

    def load_songs(self):
        # Load songs
        songs = self.thread_pool.submit(api.fetch_songs)
        songs.add_done_callback(self.on_ready_callback)

    def on_song_double_clicked(self, song):
        self.player.skip_to(song)

    def on_play_clicked(self, button):
        logger.debug('Play button clicked')
        self.player.play()

    def on_next_clicked(self, button):
        logger.debug('Next button pressed')
        self.player.next_track()

    def on_prev_clicked(self, button):
        logger.debug('Previous button pressed')
        self.player.previous_track()

    def on_ready_callback(self, future):
        try:
            songs = future.result()
        except Exception as e:
            # FIXME: use specific exception
            print('Something went wrong: {}'.format(e))
        else:
            for song_dict in songs.json():
                song = media.Song(**song_dict)
                self.player.queue(song)
                self.song_list.append(song)


class Application(Gtk.Application):
    def __init__(self, thread_pool=None, **kwargs):
        super().__init__(application_id='com.kalouantonis.channel',
                         flags=Gio.ApplicationFlags.HANDLES_COMMAND_LINE,
                         **kwargs)
        self.thread_pool = thread_pool
        self.window = None

    def do_startup(self):
        Gtk.Application.do_startup(self)

        action = Gio.SimpleAction.new('about', None)
        action.connect('activate', self.on_about)
        self.add_action(action)

        action = Gio.SimpleAction.new('quit', None)
        action.connect('activate', self.on_quit)
        self.add_action(action)

    def do_activate(self):
        if not self.window:
            self.window = ApplicationWindow(application=self, title='Channel',
                                            default_width=1024, default_height=860,
                                            thread_pool=self.thread_pool)
            self.window.load_songs()  # TODO: move me
            self.window.show_all()
        self.window.present()

    def do_command_line(self, command_line):
        # options = command_line.get_options_dict()
        self.activate()
        return 0

    def on_about(self, action, param):
        about_dialog = Gtk.AboutDialog(transient_for=self.window, modal=True)
        about_dialog.present()

    def on_quit(self, action, param):
        self.quit()


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)
    with GLibThreadPoolExecutor(max_workers=5) as pool:
        app = Application(thread_pool=pool)
        app.run(sys.argv)
