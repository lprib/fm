use super::{
    serialized::{InPort, OutPort},
    DspNode, ProgramState,
};

node_definition! {
    #[derive(Default, Clone, Debug)]
    Mixer(in1, in2, in3, in4 => out)
}
impl DspNode for Mixer {
    fn next_sample(&mut self, state: &mut ProgramState) {
        let in1 = self.in1.read(state);
        let in2 = self.in2.read(state);
        let in3 = self.in3.read(state);
        let in4 = self.in4.read(state);
        self.out.write(in1 + in2 + in3 + in4, state);
    }
}
