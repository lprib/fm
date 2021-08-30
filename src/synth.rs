use serde::{Deserialize, Serialize};
use std::f64::consts::PI;

pub struct Program {
    pub state: ProgramState,
    pub nodes: Vec<Box<dyn DspNode>>,
    pub mono: bool,
    pub sample_rate: u32,
}

impl Program {
    pub fn from_node_graph(graph: NodeGraph, mono: bool, sample_rate: u32) -> Self {
        // map enum into trait object
        let nodes: Vec<Box<dyn DspNode>> = graph
            .0
            .into_iter()
            .map(|x| -> Box<dyn DspNode> {
                match x {
                    DspNodeEnum::Adsr(x) => Box::new(x) as _,
                    DspNodeEnum::SinOsc(x) => Box::new(x) as _,
                }
            })
            .collect();
        Program {
            state: ProgramState::new(100),
            nodes,
            mono,
            sample_rate,
        }
    }
}

#[derive(Serialize, Deserialize)]
pub struct NodeGraph(pub Vec<DspNodeEnum>);

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum DspNodeEnum {
    Adsr(Adsr),
    SinOsc(SinOsc),
}

impl Program {
    pub fn next_sample(&mut self) -> f64 {
        for node in &mut self.nodes {
            node.next_sample(&mut self.state);
        }
        self.state.t += 1.0 / self.sample_rate as f64;
        self.state.links[SpecialPorts::LeftChan as usize]
    }

    pub fn set_freq(&mut self, freq: f64) {
        self.state.links[SpecialPorts::NoteFreq as usize] = freq;
    }

    pub fn set_gate(&mut self, gate: bool) {
        self.state.links[SpecialPorts::NoteGate as usize] = if gate { 1.0 } else { 0.0 };
    }
}

pub enum SpecialPorts {
    NoteFreq = 0,
    NoteGate,
    LeftChan,
    RightChan,
    NumSpecialPorts,
}

pub struct ProgramState {
    links: Vec<f64>,
    t: f64,
}

impl ProgramState {
    pub fn new(num_links: usize) -> Self {
        ProgramState {
            // Links must be at least (SpecialPorts::NumSpecialPorts as usize) elements long
            // TODO assert length
            links: vec![0.0; num_links],
            t: 0.0,
        }
    }
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum InPort {
    Link(usize),
    Const(f64),
}

impl InPort {
    pub fn read(&self, state: &ProgramState) -> f64 {
        match *self {
            Self::Link(i) => state.links[i],
            Self::Const(n) => n,
        }
    }
}

impl Default for InPort {
    fn default() -> Self {
        Self::Const(0.0)
    }
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum OutPort {
    Link(usize),
    Unused,
}

impl Default for OutPort {
    fn default() -> Self {
        Self::Unused
    }
}

impl OutPort {
    pub fn write(&self, val: f64, state: &mut ProgramState) {
        if let OutPort::Link(i) = *self {
            state.links[i] = val
        }
    }
}

pub trait DspNode {
    fn next_sample(&mut self, state: &mut ProgramState);
}

#[derive(Serialize, Deserialize, Default)]
#[serde(rename_all = "lowercase")]
pub struct SinOsc {
    pub freq: InPort,
    pub phase: InPort,
    pub vol: InPort,
    pub feedback: InPort,
    pub mult: InPort,
    pub out: OutPort,
}

impl SinOsc {
    pub fn new(
        freq: InPort,
        phase: InPort,
        vol: InPort,
        feedback: InPort,
        mult: InPort,
        out: OutPort,
    ) -> Self {
        SinOsc {
            freq,
            phase,
            vol,
            feedback,
            mult,
            out,
            ..Default::default()
        }
    }
}

impl DspNode for SinOsc {
    fn next_sample(&mut self, state: &mut ProgramState) {
        let vol = self.vol.read(state);
        let freq = self.freq.read(state);
        let phase = self.phase.read(state);
        let mult = self.mult.read(state);

        let out = vol * (2.0 * PI * mult * freq * state.t + phase).sin();

        self.out.write(out, state);
    }
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub struct Adsr {
    pub gate: InPort,
    pub a: InPort,
    pub d: InPort,
    pub s: InPort,
    pub r: InPort,
    pub out: OutPort,
    // Time of most recent gate rising edge
    #[serde(skip)]
    gate_start: f64,
    // Time of most recent gate falling edge
    #[serde(skip)]
    gate_end: f64,
    // previous value of gate, to detect edges
    #[serde(skip)]
    prev_gate: bool,
    #[serde(skip)]
    releasing: bool,
}

impl Default for Adsr {
    fn default() -> Self {
        Adsr {
            gate: Default::default(),
            a: Default::default(),
            d: Default::default(),
            s: Default::default(),
            r: Default::default(),
            out: Default::default(),
            // We must init these to -Inf, because then the ADSR will assume the 'previous' gate is
            // long passed and should ouput zero as an initial state.
            gate_start: f64::NEG_INFINITY,
            gate_end: f64::NEG_INFINITY,
            prev_gate: false,
            releasing: false,
        }
    }
}

impl Adsr {
    pub fn new(gate: InPort, a: InPort, d: InPort, s: InPort, r: InPort, out: OutPort) -> Self {
        Adsr {
            gate,
            a,
            d,
            s,
            r,
            out,
            ..Default::default()
        }
    }

    // Returns whether a given gate value is considered 'on' and should trigger the ADSR.
    fn gate_on(gate: f64) -> bool {
        gate != 0.0
    }
}

impl DspNode for Adsr {
    fn next_sample(&mut self, state: &mut ProgramState) {
        let gate = Self::gate_on(self.gate.read(state));

        // Set up gate start and end markers on rising/falling edges of gate.
        if !self.prev_gate && gate {
            self.gate_start = state.t;
            self.releasing = false;
        }
        if self.prev_gate && !gate {
            self.gate_end = state.t;
            self.releasing = true;
        }
        self.prev_gate = gate;

        let a = self.a.read(state);
        let d = self.d.read(state);
        let s = self.s.read(state);
        let r = self.r.read(state);

        // reset on new gate:

        let t = state.t - self.gate_start;
        let out = if t <= 0.0 {
            0.0
        } else if t <= a {
            t / a
        } else if t <= a + d {
            1.0 - ((t - a) / d * (1.0 - s))
        } else if gate {
            // The attack/decay is over. If the gate is still held, return the
            // sustain level.
            s
        } else if self.releasing {
            // !gate && releasing
            let release = s - ((t - self.gate_end) / r * s);
            if release <= 0.0 {
                self.releasing = false;
            }
            // Clamp release in case it went a bit below zero, which would cause an inversion
            // discontinuity.
            release.max(0.0)
        } else {
            // !gate && !releasing
            0.0
        };
        self.out.write(out, state);
    }
}
