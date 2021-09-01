use super::{
    serialized::{InPort, OutPort},
    DspNode, ProgramState,
};
use serde::{Deserialize, Serialize};
use std::f64::consts::PI;

#[derive(Serialize, Deserialize, Default)]
#[serde(rename_all = "lowercase")]
pub struct SinOsc {
    pub freq: InPort,
    pub phase: InPort,
    pub vol: InPort,
    pub feedback: InPort,
    pub mult: InPort,
    #[serde(default)]
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
