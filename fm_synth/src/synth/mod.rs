pub mod adsr;
pub mod mixer;
pub mod serialized;
pub mod sinosc;

use serde::{Deserialize, Serialize};
use serialized::{PatchDefinition, IO};

pub trait DspNode {
    fn next_sample(&mut self, state: &mut SynthState);
}

#[derive(Serialize, Deserialize)]
#[serde(from = "PatchDefinition")]
pub struct Patch {
    #[serde(skip)]
    pub state: SynthState,
    #[serde(skip)]
    pub nodes: Vec<Box<dyn DspNode>>,
    #[serde(skip)]
    pub io: IO,
    #[serde(skip)]
    pub sample_rate: u32,
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
        }
    }
}
