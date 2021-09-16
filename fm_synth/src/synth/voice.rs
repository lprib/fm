use std::fmt::Display;

use crate::synth::dsp_node::DspNode;

use super::{
    serialized::{DspNodeEnum, IO, PatchDefinition}, SynthInputEvent,
};

pub struct Program {
    state: ProgramState,
    nodes: Vec<Box<dyn DspNode + Send>>,
    io: IO,
    // used for LR interlacing
    pending_sample: Option<f64>,
}

pub struct ProgramState {
    pub links: Vec<f64>,
}

impl ProgramState {
    pub fn new(num_links: usize) -> Self {
        ProgramState {
            links: vec![0.0; num_links],
        }
    }
}

impl Program {
    pub fn new(def: &PatchDefinition) -> Self {
        // map enum into trait object
        let dyn_nodes: Vec<Box<dyn DspNode + Send>> = def
            .nodes
            .iter()
            .map(|x| -> Box<dyn DspNode + Send> {
                match x {
                    DspNodeEnum::Adsr(x) => Box::new(x.clone()) as _,
                    DspNodeEnum::SinOsc(x) => Box::new(x.clone()) as _,
                    DspNodeEnum::Mixer(x) => Box::new(x.clone()) as _,
                }
            })
            .collect();
        Program {
            state: ProgramState::new(100),
            nodes: dyn_nodes,
            io: def.io.clone(),
            pending_sample: None,
        }
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

    pub fn process_event(&mut self, event: SynthInputEvent) {
        match event {
            SynthInputEvent::KeyDown { freq, .. } => {
                self.set_freq(freq);
                self.set_gate(true);
            }
            SynthInputEvent::KeyUp { .. } => {
                self.set_gate(false);
            }
        };
    }

    pub fn next_sample(&mut self) -> (f64, f64) {
        for node in &mut self.nodes {
            node.next_sample(&mut self.state);
        }

        (
            self.io.lchan.map(|i| self.state.links[i]).unwrap_or(0.0),
            self.io.rchan.map(|i| self.state.links[i]).unwrap_or(0.0),
        )
    }
}

impl Iterator for Program {
    type Item = f32;

    fn next(&mut self) -> Option<Self::Item> {
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

impl Display for Program {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        // for node in &self.nodes {}
        write!(f, "program TODO")
    }
}
