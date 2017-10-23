extern crate rand;
extern crate glib;
extern crate gtk;
extern crate gstreamer;

mod media;

use gtk::prelude::*;
use gtk::{Button, Window, WindowType};

fn main() {
    println!("Initializing GTK+...");
    if gtk::init().is_err() {
        println!("Failed to intialize GTK.");
        return;
    }

    println!("Initializing audio subsystem...");
    media::init_audio_subsystem().unwrap();

    println!("Creating audio player...");
    let _player = media::Player::new(false);

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

    button.connect_clicked(|_| {
        println!("Clicked!");
    });

    gtk::main();
}
