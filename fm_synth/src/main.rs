mod synth;

use rodio::{buffer::SamplesBuffer, OutputStream, Sink};
use synth::{Adsr, DspNodeEnum, InPort, NodeGraph, OutPort, Program, SinOsc, SpecialPorts};

const SAMPLE_RATE: u32 = 44100;

fn main() {
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
    // sink.append(buf);
    // sink.sleep_until_end();
    let node_graph = example_node_graph();
    let json = serde_json::to_string(&node_graph).unwrap();
    println!("{}", json);
}

fn example_node_graph() -> NodeGraph {
    NodeGraph(vec![
        // mod adsr
        DspNodeEnum::Adsr(Adsr::new(
            InPort::Link(SpecialPorts::NoteGate as usize),
            InPort::Const(0.01),
            InPort::Const(3.5),
            InPort::Const(0.0),
            InPort::Const(0.0),
            OutPort::Link(5),
        )),
        // mod osc
        DspNodeEnum::SinOsc(SinOsc::new(
            InPort::Link(SpecialPorts::NoteFreq as usize),
            InPort::Const(0.0),
            InPort::Link(5),
            InPort::Const(0.0),
            InPort::Const(7.1),
            OutPort::Link(6),
        )),
        // main adsr
        DspNodeEnum::Adsr(Adsr::new(
            InPort::Link(SpecialPorts::NoteGate as usize),
            InPort::Const(0.01),
            InPort::Const(0.2),
            InPort::Const(0.4),
            InPort::Const(0.4),
            OutPort::Link(4),
        )),
        // main osc
        DspNodeEnum::SinOsc(SinOsc::new(
            InPort::Link(SpecialPorts::NoteFreq as usize),
            InPort::Link(6),
            InPort::Link(4),
            InPort::Const(0.0),
            InPort::Const(1.0),
            OutPort::Link(SpecialPorts::LeftChan as usize),
        )),
    ])
}
