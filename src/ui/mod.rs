use gtk;
use gtk::prelude::*;

struct ApplicationWindow {
    window: gtk::Window
}

impl ApplicationWindow {
    pub fn new(title: String, size: (i32, i32)) -> ApplicationWindow {
        let (width, height) = size;
        let window = gtk::Window::new(gtk::WindowType::Toplevel);
        window.set_title(&title);
        window.set_default_size(width, height);

        ApplicationWindow {
            window: window
        }
    }
}
