use super::{
    serialized::{InPort, OutPort},
    DspNode, ProgramState,
};

node_definition!{
    #[derive(Clone, Debug)]
    Adsr(gate, a, d, s, r => out) {
        gate_start: f64,
        gate_end: f64,
        prev_gate: bool,
        releasing: bool,
    }
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
            let release = s - ((state.t - self.gate_end) / r * s);
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
