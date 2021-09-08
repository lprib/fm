mod midi;
mod synth;

use std::{fs, sync::mpsc::channel};

use crate::synth::{serialized::PatchDefinition, Patch};
use midi::{get_midi_input, parse_midi};
use rodio::{OutputStream, Sink};

fn main() {
    let (tx, rx) = channel();

    let patch_str =
        fs::read_to_string("../test.json").expect("couldnt read file");
    let patch_def: PatchDefinition = serde_json::from_str(&patch_str).expect("couldnt parse");
    let patch = Patch::new(patch_def, rx);

    // setup midi input
    let (midi_in, port) = get_midi_input().unwrap();
    let _connection = midi_in
        .connect(
            &port,
            "test",
            move |_, message, _| {
                if let Some(event) = parse_midi(message) {
                    println!("sending {:?}", event);
                    tx.send(event).unwrap();
                }
            },
            (),
        )
        .expect("couldnt connect");

    let (_stream, handle) = OutputStream::try_default().unwrap();
    let sink = Sink::try_new(&handle).unwrap();
    sink.append(patch);
    sink.sleep_until_end();
}

fn main2() {
    ws::listen("localhost:8089", |client| {
        println!("New connection");
        move |msg| {
            println!("message {}", msg);
            Ok(())
        }
    })
    .unwrap();
}
