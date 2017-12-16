from gi.repository import Gtk, Gdk, GObject


def add_treeview_column(treeview, text, index=0):
    renderer = Gtk.CellRendererText()
    column = Gtk.TreeViewColumn(text, renderer, text=index)
    treeview.append_column(column)


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

    __gsignals__ = {
        'double-clicked': (GObject.SIGNAL_RUN_FIRST, None, (object,))
    }

    def __init__(self, songs = None):
        self._list_store = Gtk.ListStore(*self._field_types())
        super().__init__(model=self._list_store)
        self._songs = songs or {}
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

    def _on_cell_clicked(self, treeview, event):
        if event.type == Gdk.EventType._2BUTTON_PRESS:
            try:
                path, _, _, _ = treeview.get_path_at_pos(event.x, event.y)
            except TypeError:  # not iterable
                pass
            else:
                song_id = treeview.get_model()[path][0]
                self.emit('double-clicked', self._songs[song_id])
