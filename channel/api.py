import requests

# TODO: load from settings
API_URL = 'http://localhost:3000'


def fetch_songs(api_url=API_URL):
    return requests.get('{api}/songs'.format(api=api_url))


def fetch_song(song_id, api_url=API_URL):
    url = '{api}/songs/{id:.0f}'.format(api=api_url, id=song_id)
    return requests.get(url)
