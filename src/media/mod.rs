mod play_queue;

use std::error::Error;

use glib;
use gstreamer as gst;
use gstreamer::prelude::*;

use self::play_queue::PlayQueue;

#[derive(Debug, Clone, PartialEq)]
pub struct Track {
    id: u64,
    track: u8,
    title: String,
    artist: String,
    album: String,
    // TODO: use a URI
    file: String,
}

/// Initialize the audio subsystem used for streaming and playing audio.
///
/// Will return a string description of the internal error on failure.
pub fn init_audio_subsystem() -> Result<(), String> {
    gst::init().map_err(|e| {
        String::from(e.description())
    })
}

#[derive(PartialEq)]
enum StreamState {
    Playing,
    Paused,
    Stopped,
}

/// Represents a behaviour that can be used to stream data of any type
/// from a URI.
trait Streamable {
    /// Queue an item with the given URI in to the stream.
    fn queue(&self, uri: String);
    /// Start the stream playback
    fn start(&self);
    /// Stop the stream playback, will reset the position to the beginning.
    fn stop(&self);
    /// Pause the stream playback, maintaining the current position.
    fn pause(&self);
    /// Return the state that the stream is currently in.
    fn state(&self) -> StreamState;
}

impl From<gst::State> for StreamState {
    fn from(state: gst::State) -> Self {
        match state {
            gst::State::Playing => StreamState::Playing,
            gst::State::Paused  => StreamState::Paused,
            gst::State::Null
                | gst::State::Ready
                | gst::State::VoidPending => StreamState::Stopped,
            gst::State::__Unknown(e) => panic!("Stream has entered an unknown state: {}", e),
        }
    }
}

/// Internal audio streamer. Can stream audio from remote or local
/// files.
struct AudioStreamer {
    playbin: gst::Element,
}

impl AudioStreamer {
    fn new() -> AudioStreamer {
        AudioStreamer {
            playbin: gst::ElementFactory::make("playbin", None).unwrap()
        }
    }

    fn change_state(&self, state: gst::State) {
        let new_state = self.playbin.set_state(state);
        assert_ne!(new_state, gst::StateChangeReturn::Failure);
    }
}

impl Streamable for AudioStreamer {
    fn queue(&self, uri: String) {
        self.playbin.set_property("uri", &glib::Value::from(&uri)).unwrap();
    }

    fn start(&self) {
        self.change_state(gst::State::Playing);
    }

    fn stop(&self) {
        self.change_state(gst::State::Null);
    }

    fn pause(&self) {
        self.change_state(gst::State::Paused);
    }

    fn state(&self) -> StreamState {
        // FIXME: Could block UI thread
        let (_, current_state, _) = self.playbin.get_state(gst::CLOCK_TIME_NONE);
        StreamState::from(current_state)
    }
}

pub struct Player {
    pub is_looping: bool,
    play_queue: PlayQueue<Track>,
    // TODO; use generics
    streamer: AudioStreamer,
}

impl Player {
    pub fn new(is_looping: bool) -> Player {
        Player {
            is_looping: is_looping,
            play_queue: PlayQueue::new(),
            streamer: AudioStreamer::new(),
        }
    }

    /// Returns `true` if the player is currently streaming audio.
    ///
    /// WARNING: May block the UI thread.
    pub fn is_playing(&self) -> bool {
        self.streamer.state() == StreamState::Playing
    }

    /// Returns the current track in the queue. May or may not
    /// be currently playing.
    pub fn current_track(&self) -> Option<&Track> {
        self.play_queue.current()
    }

    /// Add a track to the end of the play queue.
    pub fn queue(&mut self, track: &Track) {
        self.play_queue.append(track);
    }

    /// Begin playback of the current track.
    ///
    /// Will panic if the player is already playing.
    pub fn play(&self) {
        assert!(self.streamer.state() != StreamState::Playing);

        if let Some(track) = self.current_track() {
            // TODO: investigate clone
            self.streamer.queue(track.file.clone());
            self.streamer.start();
        }
    }

    /// Stop playback of the current track.
    ///
    /// Will do nothing if player is already stopped.
    pub fn stop(&self) {
        if self.is_playing() {
            self.streamer.stop()
        }
    }

    /// Stop the current track and play the next one in the queue.
    ///
    /// If there are no more items, the player will stop, unless `is_looping`
    /// is set to true, in which case it will start again from the beginning.
    pub fn next_track(&mut self) {
        self.stop();

        if self.play_queue.next().is_none() {
            if self.is_looping {
                self.play_queue.reset();
            } else {
                return;
            }
        }

        self.play();
    }

    /// Stop the current track and play the previous one in the queue.
    ///
    /// If there are no more previous items the track will be started
    /// from the beginning.
    pub fn previous_track(&mut self) {
        self.stop();
        self.play_queue.previous();
        self.play();
    }
}
