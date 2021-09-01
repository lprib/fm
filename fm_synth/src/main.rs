mod synth;

use std::fs;

use crate::synth::{
    adsr::Adsr,
    serialized::{DspNodeEnum, InPort, OutPort, PatchDefinition, IO},
    sinosc::SinOsc,
    Patch,
};
use rodio::{buffer::SamplesBuffer, OutputStream, Sink};

const SAMPLE_RATE: u32 = 44100;

fn main() {
    println!("start compute");
    let patch = fs::read_to_string("../node_editor/data/test3.json").expect("couldnt read file");
    let mut patch: Patch = serde_json::from_str(&patch).expect("couldnt parse");
    // let mut patch: Patch = example_patch().into();
    let mut samples = vec![0.0; 5 * 2 * SAMPLE_RATE as usize];

    patch.set_gate(true);
    patch.set_freq(440.0);
    for i in 0..samples.len() / 2 {
        if i * 2 >= 3 * samples.len() / 4 {
            patch.set_gate(false);
        }
        let next_sample = patch.next_sample();
        // println!("{} {}", next_sample.0, next_sample.1);
        samples[i * 2] = next_sample.0 as f32;
        samples[i * 2 + 1] = next_sample.1 as f32;
    }
    println!("end compute");

    let buf = SamplesBuffer::new(2, SAMPLE_RATE, samples);

    let (_stream, handle) = OutputStream::try_default().unwrap();
    let sink = Sink::try_new(&handle).unwrap();

    sink.append(buf);
    sink.sleep_until_end();
    // let node_graph = example_program();
    // let json = serde_json::to_string(&node_graph).unwrap();
    // println!("{}", json);
}

fn example_patch() -> PatchDefinition {
    PatchDefinition {
        nodes: vec![
            DspNodeEnum::Adsr(Adsr::new(
                InPort::Link(0),
                InPort::Const(0.01),
                InPort::Const(0.2),
                InPort::Const(0.2),
                InPort::Const(0.5),
                OutPort::Link(3),
            )),
            DspNodeEnum::SinOsc(SinOsc::new(
                InPort::Link(1),
                InPort::Const(0.0),
                InPort::Link(3),
                InPort::Const(0.0),
                InPort::Const(1.0),
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
