from concurrent.futures import ThreadPoolExecutor
import logging
import sys

from channel import api, media
from channel.ui import start_app

def load_songs(thread_pool):
    songs = thread_pool.submit(api.fetch_songs).result()
    return [media.Song(**s) for s in songs.json()]


if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG)

    with ThreadPoolExecutor(max_workers=5) as thread_pool:
        songs = load_songs(thread_pool)

    player = media.Player()
    start_app(sys.argv, {'player': player, 'songs': songs})
