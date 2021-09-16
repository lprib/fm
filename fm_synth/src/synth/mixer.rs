use super::{
    serialized::{InPort, OutPort},
    voice::ProgramState,
    DspNode,
};

node_definition! {
    #[derive(Default, Clone, Debug)]
    Mixer(in1, in2, mix1, mix2 => out)
}
impl DspNode for Mixer {
    fn next_sample(&mut self, state: &mut ProgramState) {
        self.resolve_inputs(state);
        self.out.write(
            self.resolved.in1 * self.resolved.mix1 + self.resolved.in2 * self.resolved.mix2,
            state,
        );
    }
}
