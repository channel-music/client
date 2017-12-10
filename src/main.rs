#[macro_use]
extern crate log;
extern crate simple_logger;
extern crate rand;
// GTK
extern crate glib;
extern crate gio;
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
mod ui;

use std::sync::mpsc;

use gio::prelude::*;
use gtk::prelude::*;

struct Application {
    gtk: gtk::Application,
    player: media::PlayerSender,
}

impl Application {
    fn new(app_id: &'static str) -> Application {
        // TODO: handle errors
        let application = gtk::Application::new(
            app_id, gio::ApplicationFlags::FLAGS_NONE
        ).unwrap();

        let player = media::Player::new();

        Application {
            gtk: application,
            player: media::start_player(player),
        }
    }
}

fn build_ui(app: &gtk::Application, player: media::PlayerSender) {
    let window = gtk::ApplicationWindow::new(&app);
    window.set_title("Channel");
    window.set_default_size(350, 70);

    let play_button = gtk::Button::new_with_label("Play");
    let pause_button = gtk::Button::new_with_label("Pause");
    let next_button = gtk::Button::new_with_label("Next");
    let previous_button = gtk::Button::new_with_label("Previous");

    let vbox = gtk::Box::new(gtk::Orientation::Vertical, 0);
    vbox.add(&play_button);
    vbox.add(&pause_button);
    vbox.add(&next_button);
    vbox.add(&previous_button);
    window.add(&vbox);
    window.show_all();

    {
        let player = player.clone();
        play_button.connect_clicked(move |_| {
            player.send(media::PlayerCommand::Play).unwrap();
        });
    }

    {
        let player = player.clone();
        pause_button.connect_clicked(move |_| {
            player.send(media::PlayerCommand::Pause).unwrap();
        });
    }

    {
        let player = player.clone();
        next_button.connect_clicked(move |_| {
            player.send(media::PlayerCommand::Next).unwrap();
        });
    }

    {
        let player = player.clone();
        previous_button.connect_clicked(move |_| {
            player.send(media::PlayerCommand::Previous).unwrap();
        });
    }

    {
        let player = player.clone();
        window.connect_delete_event(move |_, _| {
            gtk::main_quit();
            // Ensure that player is stopped so that application can die
            player.send(media::PlayerCommand::Kill).unwrap();
            Inhibit(false)
        });
    }
}

fn main() {
    simple_logger::init().unwrap();

    debug!("Fetching tracks from API...");
    let tracks = api::fetch_tracks();

    gtk::init().unwrap();
    media::init_audio_subsystem().unwrap();

    let app = Application::new("com.kalouantonis.channel");

    for t in tracks {
        app.player.send(media::PlayerCommand::Queue(t)).unwrap();
    }

    {
        let player = app.player.clone();
        app.gtk.connect_startup(move |gtk_app| {
            build_ui(&gtk_app, player.clone());
            gtk::main();
        });
    }

    app.gtk.connect_activate(|_| {});
    app.gtk.run(&std::env::args().collect::<Vec<_>>());
}
