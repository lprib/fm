use super::{
    serialized::{InPort, OutPort},
    DspNode, ProgramState,
};
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "lowercase")]
pub struct Mixer {
    pub in1: InPort,
    pub in2: InPort,
    pub in3: InPort,
    pub mix1: InPort,
    pub mix2: InPort,
    pub mix3: InPort,
    #[serde(default)]
    pub out: OutPort,
}
impl DspNode for Mixer {
    fn next_sample(&mut self, state: &mut ProgramState) {
        let in1 = self.in1.read(state);
        let in2 = self.in2.read(state);
        let in3 = self.in3.read(state);
        let mix1 = self.mix1.read(state);
        let mix2 = self.mix2.read(state);
        let mix3 = self.mix3.read(state);
        let out = in1 * mix1 + in2 * mix2 + in3 * mix3;
        self.out.write(out, state);
    }
}
