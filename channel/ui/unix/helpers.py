from gi.repository import Gtk


def builder_from_file(file_path):
    """Adds a builder property, loading data from the given
    file path.
    """
    builder = Gtk.Builder()
    builder.add_from_file(file_path)

    def class_decorator(cls):
        cls.builder = builder
        return cls
    return class_decorator
