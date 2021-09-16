use super::{
    serialized::{InPort, OutPort},
    voice::ProgramState,
    DspNode,
};

node_definition! {
    #[derive(Default, Clone, Debug)]
    Mixer(in1, in2, in3, in4 => out)
}
impl DspNode for Mixer {
    fn next_sample(&mut self, state: &mut ProgramState) {
        self.resolve_inputs(state);
        self.out.write(
            self.resolved.in1 + self.resolved.in2 + self.resolved.in3 + self.resolved.in4,
            state,
        );
    }
}
