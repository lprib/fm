mod synth;

use rodio::{buffer::SamplesBuffer, OutputStream, Sink};
use synth::{
    adsr::Adsr,
    serialized::{DspNodeEnum, InPort, OutPort, ProgramDefinition, IO},
    sinosc::SinOsc,
};

const SAMPLE_RATE: u32 = 44100;

fn main() {
    /*
    let mut program = Program::from_node_graph(example_node_graph(), true, SAMPLE_RATE);
    let mut samples = vec![0.0; 5 * SAMPLE_RATE as usize];

    program.set_gate(true);
    program.set_freq(440.0);
    for i in 0..samples.len() {
        if i == 3 * samples.len() / 4 {
            program.set_gate(false);
        }
        samples[i] = program.next_sample() as f32;
    }

    let buf = SamplesBuffer::new(1, SAMPLE_RATE, samples);

    let (_stream, handle) = OutputStream::try_default().unwrap();
    let sink = Sink::try_new(&handle).unwrap();
    */
    // sink.append(buf);
    // sink.sleep_until_end();
    let node_graph = example_program();
    let json = serde_json::to_string(&node_graph).unwrap();
    println!("{}", json);
}

fn example_program() -> ProgramDefinition {
    ProgramDefinition {
        nodes: vec![
            DspNodeEnum::Adsr(Adsr::new(
                InPort::Link(0),
                InPort::Const(1.0),
                InPort::Const(2.0),
                InPort::Const(3.0),
                InPort::Const(4.0),
                OutPort::Link(3),
            )),
            DspNodeEnum::SinOsc(SinOsc::new(
                InPort::Link(1),
                InPort::Link(3),
                InPort::Const(0.0),
                InPort::Const(0.0),
                InPort::Const(2.0),
                OutPort::Link(2),
            )),
        ],
        io: IO {
            rchan: Some(2),
            lchan: Some(2),
            freq: Some(1),
            gate: Some(0),
        },
    }
}
