pub mod adsr;
pub mod mixer;
pub mod serialized;
pub mod sinosc;

use std::sync::mpsc::Receiver;

use self::serialized::{DspNodeEnum, PatchDefinition, IO};
use rodio::Source;

pub trait DspNode {
    fn next_sample(&mut self, state: &mut SynthState);
}

pub struct Patch {
    state: SynthState,
    nodes: Vec<Box<dyn DspNode + Send>>,
    io: IO,
    sample_rate: u32,
    event_rx: Receiver<SynthInputEvent>,
    pending_sample: Option<f64>,
}

pub struct SynthState {
    links: Vec<f64>,
    t: f64,
}

impl SynthState {
    pub fn new(num_links: usize) -> Self {
        SynthState {
            links: vec![0.0; num_links],
            t: 0.0,
        }
    }
}
impl Patch {
    pub fn new(def: PatchDefinition, event_rx: Receiver<SynthInputEvent>) -> Self {
        // map enum into trait object
        let dyn_nodes: Vec<Box<dyn DspNode + Send>> = def
            .nodes
            .into_iter()
            .map(|x| -> Box<dyn DspNode + Send> {
                match x {
                    DspNodeEnum::Adsr(x) => Box::new(x) as _,
                    DspNodeEnum::SinOsc(x) => Box::new(x) as _,
                    DspNodeEnum::Mixer(x) => Box::new(x) as _,
                }
            })
            .collect();
        Patch {
            state: SynthState::new(100),
            nodes: dyn_nodes,
            io: def.io,
            sample_rate: 44100,
            event_rx,
            pending_sample: None,
        }
    }

    pub fn next_sample(&mut self) -> (f64, f64) {
        for node in &mut self.nodes {
            node.next_sample(&mut self.state);
        }
        self.state.t += 1.0 / self.sample_rate as f64;

        (
            self.io.lchan.map(|i| self.state.links[i]).unwrap_or(0.0),
            self.io.rchan.map(|i| self.state.links[i]).unwrap_or(0.0),
        )
    }

    pub fn set_freq(&mut self, freq: f64) {
        if let Some(i) = self.io.freq {
            self.state.links[i] = freq;
        }
    }

    pub fn set_gate(&mut self, gate: bool) {
        if let Some(i) = self.io.gate {
            self.state.links[i] = if gate { 1.0 } else { 0.0 };
            println!("new gate {}", self.state.links[i]);
        }
    }
}

#[derive(Debug)]
pub enum SynthInputEvent {
    KeyDown { freq: f64 },
    KeyUp,
}

impl Iterator for Patch {
    type Item = f32;

    fn next(&mut self) -> Option<Self::Item> {
        // get all events in the queue
        while let Ok(event) = self.event_rx.try_recv() {
            match event {
                SynthInputEvent::KeyDown { freq } => {
                    self.set_freq(freq);
                    self.set_gate(true);
                }
                SynthInputEvent::KeyUp => {
                    self.set_gate(false);
                }
            };
        }

        // Perform interlacing
        if let Some(samp) = self.pending_sample {
            self.pending_sample = None;
            Some(samp as f32)
        } else {
            // No pending sample, so generate
            let (l, r) = self.next_sample();
            self.pending_sample = Some(r);
            Some(l as f32)
        }
    }
}

impl Source for Patch {
    fn current_frame_len(&self) -> Option<usize> {
        None
    }

    fn channels(&self) -> u16 {
        2
    }

    fn sample_rate(&self) -> u32 {
        self.sample_rate
    }

    fn total_duration(&self) -> Option<std::time::Duration> {
        None
    }
}
