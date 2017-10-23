mod play_queue;

use std::error::Error;

use glib;
use gstreamer as gst;
use gstreamer::prelude::*;

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

/// Initialize the audio subsystem used for streaming and playing audio.
///
/// Will return a string description of the internal error on failure.
pub fn init_audio_subsystem() -> Result<(), String> {
    gst::init().map_err(|e| {
        String::from(e.description())
    })
}

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
