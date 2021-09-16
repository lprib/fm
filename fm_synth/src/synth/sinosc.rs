use std::f64::consts::PI;

use crate::synth::dsp_node::DspNode;
use crate::synth::port::{InPort, OutPort};

use super::{
    SAMPLE_PERIOD, voice::ProgramState,
};

node_definition! {
    #[derive(Default, Clone, Debug)]
    SinOsc(freq, phase, vol, feedback => out) {
        frequency_integral: f64,
    }
}

impl DspNode for SinOsc {
    fn next_sample(&mut self, state: &mut ProgramState) {
        self.resolve_inputs(state);
        self.frequency_integral += self.resolved.freq * SAMPLE_PERIOD;
        let out =
            self.resolved.vol * (2.0 * PI * self.frequency_integral + self.resolved.phase).sin();

        self.out.write(out, state);
    }
}
