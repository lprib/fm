pub mod adsr;
pub mod serialized;
pub mod sinosc;

use serde::{Deserialize, Serialize};
use serialized::{InPort, OutPort, ProgramDefinition, IO};

pub trait DspNode {
    fn next_sample(&mut self, state: &mut ProgramState);
}

#[derive(Serialize, Deserialize)]
#[serde(from = "ProgramDefinition")]
pub struct Program {
    #[serde(skip)]
    pub state: ProgramState,
    #[serde(skip)]
    pub nodes: Vec<Box<dyn DspNode>>,
    #[serde(skip)]
    pub io: IO,
    #[serde(skip)]
    pub sample_rate: u32,
}

pub struct ProgramState {
    links: Vec<f64>,
    t: f64,
}

impl ProgramState {
    pub fn new(num_links: usize) -> Self {
        ProgramState {
            links: vec![0.0; num_links],
            t: 0.0,
        }
    }
}

impl Program {
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
