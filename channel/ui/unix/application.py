import logging
import signal

import gi
gi.require_version('Gtk', '3.0')  # noqa
from gi.repository import Gtk, Gio

from channel import media
from channel.ui.unix.components.player_action_bar import PlayerActionBar
from channel.ui.unix.components.song_list_view import SongListView

logger = logging.getLogger(__name__)


class ApplicationWindow(Gtk.ApplicationWindow):
    def __init__(self, player=None, songs=None, **kwargs):
        super().__init__(**kwargs)
        self.player = player

        self.song_list = SongListView()
        self.song_list.connect(
            'double-clicked',
            self.on_song_list_double_clicked
        )
        self.play_list = SongListView()
        self.play_list.connect(
            'double-clicked',
            self.on_play_list_double_clicked
        )

        for song in songs:
            self.song_list.append(song)

        song_list_scrolled = Gtk.ScrolledWindow()
        song_list_scrolled.add(self.song_list)

        play_list_scrolled = Gtk.ScrolledWindow()
        play_list_scrolled.add(self.play_list)

        actionbar = PlayerActionBar(player)
        music_list_box = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL)
        music_list_box.pack_start(song_list_scrolled, True, True, 0)
        music_list_box.pack_start(play_list_scrolled, True, True, 2)

        box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL,
                      spacing=5, border_width=12)
        box.pack_start(music_list_box, True, True, 0)
        box.pack_start(actionbar, False, True, 2)
        self.add(box)

    def on_song_list_double_clicked(self, _, song):
        logger.debug('Adding song to play list: %r' % repr(song))
        self.player.queue(song)
        self.play_list.append(song)

    def on_play_list_double_clicked(self, _, song):
        logger.debug('Playing song: %r' % repr(song))
        self.player.jump_to(song)


class Application(Gtk.Application):
    def __init__(self, **options):
        super().__init__(application_id='com.kalouantonis.channel',
                         flags=Gio.ApplicationFlags.HANDLES_COMMAND_LINE)
        self.options = options
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
                                            **self.options)
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


def start_app(argv, context):
    # Close on keyboard interrupt
    signal.signal(signal.SIGINT, signal.SIG_DFL)
    app = Application(**context)
    app.run(argv)
