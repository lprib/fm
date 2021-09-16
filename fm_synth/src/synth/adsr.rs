use crate::synth::SAMPLE_PERIOD;

use super::{
    serialized::{InPort, OutPort, Port},
    voice::ProgramState,
    DspNode,
};

node_definition! {
    #[derive(Default, Clone, Debug)]
    Adsr(gate, a, d, s, r => out) {
        prev_gate: bool,
        state: AdsrState,
        val: f64
    }
}

#[derive(Clone, Debug)]
enum AdsrState {
    Idle,
    Attacking,
    Decaying,
    Sustaining,
    Releasing,
}

impl Default for AdsrState {
    fn default() -> Self {
        Self::Idle
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
        self.resolve_inputs(state);
        let gate = Self::gate_on(self.resolved.gate);

        if !self.prev_gate && gate {
            self.val = 0.0;
            self.state = AdsrState::Attacking;
        }

        match self.state {
            AdsrState::Idle => {}
            AdsrState::Attacking => {
                self.val += SAMPLE_PERIOD / self.resolved.a;
                if self.val >= 1.0 {
                    self.val = 1.0;
                    self.state = AdsrState::Decaying;
                }
            }
            AdsrState::Decaying => {
                self.val -= (1.0 - self.resolved.s) * (SAMPLE_PERIOD / self.resolved.d);
                if self.resolved.d == 0.0 || self.val <= self.resolved.s {
                    self.val = self.resolved.s;
                    self.state = AdsrState::Sustaining;
                }
            }
            AdsrState::Sustaining => {
                if self.prev_gate && !gate {
                    self.state = AdsrState::Releasing;
                }
            }
            AdsrState::Releasing => {
                self.val -= self.resolved.s * (SAMPLE_PERIOD / self.resolved.r);
                if self.resolved.s == 0.0 || self.val <= 0.0 {
                    // TODO notify note finished?
                    self.val = 0.0;
                    self.state = AdsrState::Idle;
                }
            }
        }
        assert!(!self.val.is_nan(), "ADSR output must not be NaN");

        self.prev_gate = gate;
        self.out.write(self.val, state);
    }
}
