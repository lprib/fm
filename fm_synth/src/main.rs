use std::{
    sync::{
        atomic::{AtomicUsize, Ordering},
        Arc,
    },
    time::Duration,
};

use crossbeam_channel::unbounded;
use rodio::{OutputStream, Sink, Source};

use midi::{get_midi_input, parse_midi};
use server::{start_websocket_server, ClientRequest};

use crate::synth::Patch;

mod midi;
mod server;
mod synth;

fn main() {
    // Channel to send midi events to synth audio `Source`
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
                    // Send event over channel
                    synth_event_tx.send(event).unwrap();
                }
            },
            (),
        )
        .expect("couldnt connect");

    let (_stream, handle) = OutputStream::try_default().unwrap();
    let sink = Sink::try_new(&handle).unwrap();

    // Index of the currently active patch. All other patches periodically check if
    // their index equals this, and stop/destroy themselves if not.
    let active_patch_number = Arc::new(AtomicUsize::new(0));

    while let Ok(req) = websocket_rx.recv() {
        if let ClientRequest::UpdatePatch(patch_def) = req {
            println!("Received patch");
            let active_patch_number = active_patch_number.clone();
            active_patch_number.fetch_add(1, Ordering::SeqCst);

            let patch = Patch::new(
                patch_def,
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
