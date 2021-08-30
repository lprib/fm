mod synth;

use rodio::{buffer::SamplesBuffer, OutputStream, Sink};
use synth::{Adsr, InPort, OutPort, Program, ProgramState, SinOsc, SpecialPorts};

const SAMPLE_RATE: u32 = 44100;

fn main() {
    let mut program = example_program();
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
    sink.append(buf);
    sink.sleep_until_end();
}

fn example_program() -> Program {
    Program {
        state: ProgramState::new(100),
        nodes: vec![
            // mod adsr
            Box::new(Adsr::new(
                InPort::Link(SpecialPorts::NoteGate as usize),
                InPort::Const(0.01),
                InPort::Const(3.5),
                InPort::Const(0.0),
                InPort::Const(0.0),
                OutPort::Link(5),
            )),
            // mod osc
            Box::new(SinOsc::new(
                InPort::Link(SpecialPorts::NoteFreq as usize),
                InPort::Const(0.0),
                InPort::Link(5),
                InPort::Const(0.0),
                InPort::Const(2.0),
                OutPort::Link(6),
            )),
            // main adsr
            Box::new(Adsr::new(
                InPort::Link(SpecialPorts::NoteGate as usize),
                InPort::Const(0.01),
                InPort::Const(0.2),
                InPort::Const(0.4),
                InPort::Const(0.4),
                OutPort::Link(4),
            )),
            // main osc
            Box::new(SinOsc::new(
                InPort::Link(SpecialPorts::NoteFreq as usize),
                InPort::Link(6),
                InPort::Link(4),
                InPort::Const(0.0),
                InPort::Const(1.0),
                OutPort::Link(SpecialPorts::LeftChan as usize),
            )),
        ],
        mono: true,
        sample_rate: SAMPLE_RATE,
    }
}
