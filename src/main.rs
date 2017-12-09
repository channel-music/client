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

use std::sync::Arc;

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
    let mut player = Arc::new(media::Player::new());

    println!("Queuing tracks in audio player...");
    for t in tracks { Arc::make_mut(&mut player).queue(&t); }

    let window = Window::new(WindowType::Toplevel);
    window.set_title("Channel");
    window.set_default_size(350, 70);

    let play_button = Button::new_with_label("Play");
    let pause_button = Button::new_with_label("Pause");

    let vbox = gtk::Box::new(gtk::Orientation::Vertical, 0);
    vbox.add(&play_button);
    vbox.add(&pause_button);
    window.add(&vbox);
    window.show_all();

    window.connect_delete_event(|_, _| {
        gtk::main_quit();
        Inhibit(false)
    });

    {
        let player = player.clone();
        play_button.connect_clicked(move |_| {
            player.play();
        });
    }

    {
        let player = player.clone();
        pause_button.connect_clicked(move |_| {
            player.pause();
        });
    }

    gtk::main();
}
