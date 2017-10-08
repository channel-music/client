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
class SongListView(Gtk.TreeView):
    displayed_fields = [
        (int, '#', 'track'),
        (str, 'Title', 'title'),
        (str, 'Album', 'album'),
        (str, 'Artist', 'artist'),
    ]

    hidden_fields = [
        (int, 'id'),
    ]

    def __init__(self):
        self._list_store = Gtk.ListStore(*self._field_types())
        super().__init__(model=self._list_store)
        self._songs = {}
        self._callbacks = {}

        self.connect('button-press-event', self._on_cell_clicked)

        for idx, display_name in enumerate(self._field_display_names()):
            add_treeview_column(
                self, display_name, idx + len(self.hidden_fields)
            )

    def _field_attrs(self):
        # Hidden fields always come first
        fields = [a for _, a in self.hidden_fields]
        fields += [a for _, _, a in self.displayed_fields]
        return fields

    def _field_types(self):
        types = [t for t, _ in self.hidden_fields]
        types += [t for t, _, _ in self.displayed_fields]
        return types

    def _field_display_names(self):
        return [n for _, n, _ in self.displayed_fields]

    def append(self, song):
        attrs = self._field_attrs()
        row_data = [getattr(song, a) for a in attrs]
        self._list_store.append(row_data)
        self._songs[song.id] = song

    def on_double_click(self, callback):
        self._callbacks['double-click'] = callback

    def _on_cell_clicked(self, treeview, event):
        try:
            callback = self._callbacks['double-click']
        except KeyError:
            pass
        else:
            if event.type == Gdk.EventType._2BUTTON_PRESS:
                try:
                    path, _, _, _ = treeview.get_path_at_pos(event.x, event.y)
                except TypeError:  # not iterable
                    pass
                else:
                    song_id = treeview.get_model()[path][0]
                    callback(self._songs[song_id])


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
        self.song_list.on_double_click(self.on_song_list_double_clicked)
        self.play_list = SongListView()
        self.play_list.on_double_click(self.on_play_list_double_clicked)

        song_list_scrolled = Gtk.ScrolledWindow()
        song_list_scrolled.add(self.song_list)

        play_list_scrolled = Gtk.ScrolledWindow()
        play_list_scrolled.add(self.play_list)

        actionbar = MusicActionBar()
        actionbar.play_button.connect('clicked', self.on_play_clicked)
        actionbar.next_button.connect('clicked', self.on_next_clicked)
        actionbar.prev_button.connect('clicked', self.on_prev_clicked)

        music_list_box = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL)
        music_list_box.pack_start(song_list_scrolled, True, True, 0)
        music_list_box.pack_start(play_list_scrolled, True, True, 2)

        box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL,
                      spacing=5, border_width=12)
        box.pack_start(music_list_box, True, True, 0)
        box.pack_start(actionbar, False, True, 2)
        self.add(box)

    def load_songs(self):
        # Load songs
        songs = self.thread_pool.submit(api.fetch_songs)
        songs.add_done_callback(self.on_ready_callback)

    def on_play_clicked(self, button):
        logger.debug('Play button clicked')
        self.player.play()

    def on_next_clicked(self, button):
        logger.debug('Next button pressed')
        self.player.next_track()

    def on_prev_clicked(self, button):
        logger.debug('Previous button pressed')
        self.player.previous_track()

    def on_song_list_double_clicked(self, song):
        logger.debug('Adding song to play list: %r' % repr(song))
        self.player.queue(song)
        self.play_list.append(song)

    def on_play_list_double_clicked(self, song):
        logger.debug('Playing song: %r' % repr(song))
        self.player.jump_to(song)

    def on_ready_callback(self, future):
        try:
            songs = future.result()
        except Exception as e:
            # FIXME: use specific exception
            print('Something went wrong: {}'.format(e))
        else:
            for song_dict in songs.json():
                song = media.Song(**song_dict)
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
