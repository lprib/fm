mod midi;
mod server;
mod synth;

use std::{
    sync::{
        atomic::{AtomicUsize, Ordering},
        Arc,
    },
    time::Duration,
};

use crate::synth::Patch;
use crossbeam_channel::unbounded;
use midi::{get_midi_input, parse_midi};
use rodio::{OutputStream, Sink, Source};
use server::{start_websocket_server, ClientRequest};

fn main() {
    let (synth_event_tx, synth_event_rx) = unbounded();
    let (websocket_tx, websocket_rx) = unbounded();

    start_websocket_server(websocket_tx);

    // setup midi input
    let (midi_in, port) = get_midi_input().unwrap();
    let _connection = midi_in
        .connect(
            &port,
            "fm_synth",
            move |_, message, _| {
                if let Some(event) = parse_midi(message) {
                    synth_event_tx.send(event).unwrap();
                }
            },
            (),
        )
        .expect("couldnt connect");

    let (_stream, handle) = OutputStream::try_default().unwrap();
    let sink = Sink::try_new(&handle).unwrap();

    let active_patch_number = Arc::new(AtomicUsize::new(0));

    while let Ok(req) = websocket_rx.recv() {
        if let ClientRequest::UpdatePatch(patchdef) = req {
            println!("Recieved patch");
            let active_patch_number = active_patch_number.clone();
            active_patch_number.fetch_add(1, Ordering::SeqCst);

            let patch = Patch::new(
                patchdef,
                synth_event_rx.clone(),
                active_patch_number.load(Ordering::SeqCst),
            )
            .stoppable()
            .periodic_access(Duration::from_millis(100), move |src| {
                // detect if this patch is stale and stop
                if src.inner().index != active_patch_number.load(Ordering::SeqCst) {
                    println!("Stopping patch {}", src.inner().index);
                    src.stop();
                }
            });
            sink.append(patch);
        }
    }
}
