use super::{
    serialized::{InPort, OutPort, Port},
    voice::ProgramState,
    DspNode, SAMPLE_PERIOD,
};
use std::f64::consts::PI;

node_definition! {
    #[derive(Default, Clone, Debug)]
    SinOsc(freq, phase, vol, feedback => out) {
        frequency_integral: f64,
    }
}

impl DspNode for SinOsc {
    fn next_sample(&mut self, state: &mut ProgramState) {
        let vol = self.vol.read(state);
        let freq = self.freq.read(state);
        let phase = self.phase.read(state);

        self.frequency_integral += freq * SAMPLE_PERIOD;
        let out = vol * (2.0 * PI * self.frequency_integral + phase).sin();

        self.out.write(out, state);
    }
}
