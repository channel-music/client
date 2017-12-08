use reqwest;
use media;
use serde_json;

use std::io::Read;

// TODO: handle errors
pub fn fetch_tracks() -> Vec<media::Track> {
    let mut resp = reqwest::get("http://localhost:3000/songs").unwrap();
    assert!(resp.status().is_success());

    let mut content = String::new();
    resp.read_to_string(&mut content).unwrap();
    serde_json::from_str(&content).unwrap()
}
