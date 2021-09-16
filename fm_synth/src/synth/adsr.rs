use super::{
    serialized::{InPort, OutPort, Port},
    voice::ProgramState,
    DspNode,
};

node_definition! {
    #[derive(Default, Clone, Debug)]
    Adsr(gate, a, d, s, r => out) {
        state: AdsrState,
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
        let gate = Self::gate_on(self.gate.read(state));
        self.out.write(self.gate.read(state), state);
    }
}
