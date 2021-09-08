use super::{
    serialized::{InPort, OutPort},
    DspNode, ProgramState,
};
use std::f64::consts::PI;

node_definition! {
    #[derive(Default, Clone, Debug)]
    SinOsc(freq, phase, vol, feedback => out)
}

impl DspNode for SinOsc {
    fn next_sample(&mut self, state: &mut ProgramState) {
        let vol = self.vol.read(state);
        let freq = self.freq.read(state);
        let phase = self.phase.read(state);

        let out = vol * (2.0 * PI * freq * state.t + phase).sin();

        self.out.write(out, state);
    }
}
