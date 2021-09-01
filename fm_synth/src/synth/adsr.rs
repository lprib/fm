use super::{
    serialized::{InPort, OutPort},
    DspNode, ProgramState,
};
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub struct Adsr {
    pub gate: InPort,
    pub a: InPort,
    pub d: InPort,
    pub s: InPort,
    pub r: InPort,
    #[serde(default)]
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
