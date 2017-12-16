from gi.repository import Gtk, GLib

from channel.ui.unix.helpers import builder_from_file


@builder_from_file('data/ui/player_action_bar.ui')
class PlayerActionBar(Gtk.ActionBar):
    def __init__(self, player):
        super().__init__()
        # TODO: don't use pooling, use events
        GLib.timeout_add(250, self._update_progress)

        self.player = player

        self.previous_button = self.builder.get_object('previous_button')
        self.play_button = self.builder.get_object('play_button')
        self.next_button = self.builder.get_object('next_button')

        self.previous_button.connect('clicked', self._on_previous_clicked)
        self.play_button.connect('clicked', self._on_play_clicked)
        self.next_button.connect('clicked', self._on_next_clicked)

        self.progress_bar = self.builder.get_object('progress_bar')

        self.add(self.builder.get_object('player_action_bar'))

    def _on_previous_clicked(self, button):
        self.player.previous_track()

    def _on_play_clicked(self, button):
        if self.player.is_playing:
            self.player.pause()
        else:
            self.player.play()

    def _on_next_clicked(self, button):
        self.player.next_track()

    def _update_progress(self):
        if self.player.is_playing:
            self.progress_bar.set_fraction(self.player.position)
        return True  # reschedule
