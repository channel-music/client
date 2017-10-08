from unittest import TestCase

from channel.media import play_queue
from channel.media.play_queue import PlayQueue


class PlayQueueTest(TestCase):
    def test_current_none_when_empty(self):
        pq = PlayQueue()
        self.assertEqual(pq.current, None)

    def test_current_when_not_empty(self):
        pq = PlayQueue([1, 2])
        self.assertEqual(pq.current, 1)

    def test_clear_removes_all_items(self):
        pq = PlayQueue([1, 2])
        self.assertEqual(pq.current, 1)
        pq.clear()
        self.assertEqual(pq.current, None)

    def test_append_adds_new_items(self):
        pq = PlayQueue()
        pq.append(1)
        self.assertEqual(pq.current, 1)
        pq.append(2)
        self.assertEqual(pq.current, 1)

    def test_next_moves_to_next_item(self):
        pq = PlayQueue([1, 2])
        self.assertEqual(pq.current, 1)
        pq.next()
        self.assertEqual(pq.current, 2)

    def test_next_fails_with_no_items(self):
        pq = PlayQueue()
        with self.assertRaises(AssertionError):
            pq.next()

    def test_next_fails_if_there_are_no_other_items(self):
        pq = PlayQueue([1])
        pq.next()
        with self.assertRaises(play_queue.Exhausted):
            pq.next()

    def test_reset_moves_queue_to_beginning(self):
        pq = PlayQueue([1, 2, 3])
        pq.next()
        pq.next()
        self.assertEqual(pq.current, 3)
        pq.reset()
        self.assertEqual(pq.current, 1)

    def test_previous_moves_to_previous_item(self):
        pq = PlayQueue([1, 2])
        pq.next()
        pq.previous()
        self.assertEqual(pq.current, 1)

    def test_previous_fails_with_no_items(self):
        pq = PlayQueue()
        with self.assertRaises(AssertionError):
            pq.previous()

    def test_previous_fails_if_there_are_no_other_items(self):
        pq = PlayQueue([1, 2])
        pq.next()
        pq.previous()
        with self.assertRaises(play_queue.Exhausted):
            pq.previous()

    def test_jump_to_jumps_to_given_item(self):
        pq = PlayQueue([1, 2, 3, 4])
        pq.jump_to(3)
        self.assertEqual(pq.current, 3)

    def test_jump_to_non_existent_item(self):
        pq = PlayQueue([1, 2, 3])
        with self.assertRaises(ValueError):
            pq.jump_to(42)

    def test_shuffle_on_empty_queue(self):
        pq = PlayQueue()
        with self.assertRaises(AssertionError):
            pq.shuffle()

    def test_shuffle_keeps_first_item(self):
        pq = PlayQueue(range(5))
        pq.shuffle()
        self.assertEqual(pq.current, 0)

    def test_shuffle_rearanges_items_randomly(self):
        original_items = list(range(100))
        # There's a very tiny chance that the shuffle results in
        # the same queue, just rerun the tests.
        pq = PlayQueue(original_items)
        pq.shuffle()

        shuffled_items = []
        for i in range(100):
            shuffled_items.append(pq.current)
            pq.next()

        self.assertNotEqual(original_items, shuffled_items)
