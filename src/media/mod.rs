mod play_queue;

// use self::play_queue::PlayQueue;

#[derive(Debug, Clone, PartialEq)]
struct Song {
    id: u64,
    track: u8,
    title: String,
    artist: String,
    album: String,
    // TODO: use a URI
    file: String,
}

// struct Player {
//     queue: PlayQueue<Song>,
// }
