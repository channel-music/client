extern crate rand;
// GTK
extern crate glib;
extern crate gtk;
extern crate gstreamer;
// JSON
extern crate serde;
extern crate serde_json;
#[macro_use]
extern crate serde_derive;
// HTTP
extern crate reqwest;

mod api;
mod media;

use gtk::prelude::*;
use gtk::{Button, Window, WindowType};

fn main() {
    println!("Fetching tracks...");
    let tracks = api::fetch_tracks();
    println!("Tracks: {:?}", tracks);

    println!("Initializing GTK+...");
    if gtk::init().is_err() {
        println!("Failed to intialize GTK.");
        return;
    }

    println!("Initializing audio subsystem...");
    media::init_audio_subsystem().unwrap();

    println!("Creating audio player...");
    let mut player = media::Player::new();

    println!("Queuing tracks in audio player...");
    for t in tracks { player.queue(&t); }

    let window = Window::new(WindowType::Toplevel);
    window.set_title("Channel");
    window.set_default_size(350, 70);

    let button = Button::new_with_label("Click me!");
    window.add(&button);
    window.show_all();

    window.connect_delete_event(|_, _| {
        gtk::main_quit();
        Inhibit(false)
    });

    button.connect_clicked(move |_| {
        player.play();
    });

    gtk::main();
}
