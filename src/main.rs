use std::{f64::consts::PI, vec};

use rodio::{buffer::SamplesBuffer, OutputStream, Sink};

const SAMPLE_RATE: u32 = 44100;

fn main() {
    let mut program = example_program();
    let mut samples = vec![0.0; 5 * SAMPLE_RATE as usize];

    program.set_gate(true);
    program.set_freq(440.0);
    for i in 0..samples.len() {
        if i == 3 * samples.len() / 4 {
            program.set_gate(false);
        }
        samples[i] = program.next_sample() as f32;
    }

    let buf = SamplesBuffer::new(1, SAMPLE_RATE, samples);

    let (_stream, handle) = OutputStream::try_default().unwrap();
    let sink = Sink::try_new(&handle).unwrap();
    sink.append(buf);
    sink.sleep_until_end();
}

fn example_program() -> Program {
    Program {
        state: ProgramState {
            links: vec![0.0; 100],
            t: 0.0,
        },
        nodes: vec![
            // mod adsr
            Box::new(Adsr {
                gate: InPort::Link(SpecialPorts::NoteGate as usize),
                a: InPort::Const(0.01),
                d: InPort::Const(3.5),
                s: InPort::Const(0.0),
                r: InPort::Const(0.0),
                out: OutPort::Link(5),
                ..Default::default()
            }),
            // mod osc
            Box::new(SinOsc {
                freq: InPort::Link(SpecialPorts::NoteFreq as usize),
                phase: InPort::Const(0.0),
                vol: InPort::Link(5),
                feedback: InPort::Const(0.0),
                mult: InPort::Const(2.0),
                out: OutPort::Link(6),
            }),
            // main adsr
            Box::new(Adsr {
                gate: InPort::Link(SpecialPorts::NoteGate as usize),
                a: InPort::Const(0.01),
                d: InPort::Const(0.2),
                s: InPort::Const(0.4),
                r: InPort::Const(0.4),
                out: OutPort::Link(4),
                ..Default::default()
            }),
            // main osc
            Box::new(SinOsc {
                freq: InPort::Link(SpecialPorts::NoteFreq as usize),
                phase: InPort::Link(6),
                vol: InPort::Link(4),
                feedback: InPort::Const(0.0),
                mult: InPort::Const(1.0),
                out: OutPort::Link(SpecialPorts::LeftChan as usize),
            }),
        ],
        mono: true,
    }
}

struct Program {
    state: ProgramState,
    nodes: Vec<Box<dyn DspNode>>,
    mono: bool,
}

impl Program {
    fn next_sample(&mut self) -> f64 {
        for node in &mut self.nodes {
            node.next_sample(&mut self.state);
        }
        self.state.t += 1.0 / SAMPLE_RATE as f64;
        self.state.links[SpecialPorts::LeftChan as usize]
    }

    fn set_freq(&mut self, freq: f64) {
        self.state.links[SpecialPorts::NoteFreq as usize] = freq;
    }

    fn set_gate(&mut self, gate: bool) {
        self.state.links[SpecialPorts::NoteGate as usize] = if gate { 1.0 } else { 0.0 };
    }
}

enum SpecialPorts {
    NoteFreq = 0,
    NoteGate,
    LeftChan,
    RightChan,
    NumSpecialPorts,
}

struct ProgramState {
    // Links must be at least (SpecialPorts::NumSpecialPorts as usize) elements long
    links: Vec<f64>,
    t: f64,
}

enum InPort {
    Link(usize),
    Const(f64),
}

impl InPort {
    fn read(&self, state: &ProgramState) -> f64 {
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

enum OutPort {
    Link(usize),
    Unused,
}

impl Default for OutPort {
    fn default() -> Self {
        Self::Unused
    }
}

impl OutPort {
    fn write(&self, val: f64, state: &mut ProgramState) {
        if let OutPort::Link(i) = *self {
            state.links[i] = val
        }
    }
}

trait DspNode {
    fn next_sample(&mut self, state: &mut ProgramState);
}

struct SinOsc {
    pub freq: InPort,
    pub phase: InPort,
    pub vol: InPort,
    pub feedback: InPort,
    pub mult: InPort,
    pub out: OutPort,
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

struct Adsr {
    pub gate: InPort,
    pub a: InPort,
    pub d: InPort,
    pub s: InPort,
    pub r: InPort,
    pub out: OutPort,
    // Time of most recent gate rising edge
    gate_start: f64,
    // Time of most recent gate falling edge
    gate_end: f64,
    // previous value of gate, to detect edges
    prev_gate: bool,
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
