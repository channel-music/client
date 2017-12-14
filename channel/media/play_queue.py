import random


class Exhausted(Exception):
    """Exception raised by `PlayQueue` when it can not move
    further forward or backward."""
    pass


# TODO: implement a shuffle method
class PlayQueue:
    def __init__(self, items=None):
        """A queue containing a set of items, allowing movement back and forth.

        items: List of song items to add in to the queue
        """
        if items:
            self._tracks = list(items)
        else:
            self._tracks = []
        self._current_idx = 0

    def next(self):
        """Move to the next track in the queue.

        Throws an `AssertionError` if there are no tracks in the queue
        or if the queue can not move forward.
        """
        assert self._tracks
        if self._current_idx >= len(self._tracks):
            raise Exhausted('Can not move queue further forward')

        self._current_idx += 1

    def previous(self):
        """Move to the previous track in the queue.

        Throws an `AssertionError` if there are no tracks in the queue
        or if the queue can not move backwards.
        """
        assert self._tracks
        if self._current_idx <= 0:
            raise Exhausted('Can not move queue further back')

        self._current_idx -= 1

    @property
    def current(self):
        """The current track in the queue, returns `None` if there
        isn't one."""
        try:
            track = self._tracks[self._current_idx]
        except IndexError:
            track = None
        return track

    def append(self, item):
        """Add a track to the end of the queue."""
        self._tracks.append(item)

    def clear(self):
        """Remove all tracks from the queue."""
        self._tracks = []
        self._current_idx = 0

    def jump_to(self, item):
        """Jump to a specific item in the queue."""
        # FIXME: throw a missing song exception
        index = self._tracks.index(item)
        self._current_idx = index

    def reset(self):
        """Reset queue back to the first item."""
        self._current_idx = 0

    def shuffle(self):
        """Shuffle tracks in queue, keeping the current item at the front.

        Throws an `AssertionError` if there are no tracks in the queue.
        """
        assert self._tracks

        current_track = self.current
        shuffled_tracks = list(self._tracks)
        # Remove current track from tracks and add it later so that
        # the current track is always first
        del shuffled_tracks[self._current_idx]
        random.shuffle(shuffled_tracks)
        self._tracks = [current_track] + shuffled_tracks
        self._current_idx = 0
