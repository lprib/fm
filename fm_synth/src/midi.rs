use std::io::{stdin, stdout, Write};

use midir::{MidiInput, MidiInputPort};

use crate::synth::SynthInputEvent;

pub fn get_midi_input() -> Result<(MidiInput, MidiInputPort), String> {
    let mut midi_in =
        MidiInput::new("FM Synth Input").map_err(|_| "Could not create midi input")?;
    midi_in.ignore(midir::Ignore::None);

    let mut in_ports = midi_in.ports();
    let in_port = match in_ports.len() {
        0 => return Err("no input port found".into()),
        1 => {
            println!(
                "Choosing the only available input port: {}",
                midi_in.port_name(&in_ports[0]).unwrap()
            );
            in_ports.remove(0)
        }
        2 => {
            println!("Only two ports, assuming one as pass-through and choosing the second");
            in_ports.remove(1)
        }
        _ => {
            println!("\nAvailable input ports:");
            for (i, p) in in_ports.iter().enumerate() {
                println!("{}: {}", i, midi_in.port_name(p).unwrap());
            }
            print!("Please select input port: ");
            stdout().flush().map_err(|_| "could not flush stdout")?;
            let mut input = String::new();
            stdin()
                .read_line(&mut input)
                .map_err(|_| "could not read input")?;
            let index = input
                .trim()
                .parse::<usize>()
                .map_err(|_| "couldnt parse port selection")?;
            if index > in_ports.len() {
                return Err(String::from("Selected index out of range"));
            } else {
                in_ports.remove(index)
            }
        }
    };

    Ok((midi_in, in_port))
}

pub fn parse_midi(bytes: &[u8]) -> Option<SynthInputEvent> {
    if (bytes[0] & 0xF0) == 0x90 {
        Some(SynthInputEvent::KeyDown {
            key: bytes[1],
            freq: key_to_freq(bytes[1]),
        })
    } else if (bytes[0] & 0xF0) == 0x80 {
        Some(SynthInputEvent::KeyUp { key: bytes[1] })
    } else {
        None
    }
}

fn key_to_freq(key: u8) -> f64 {
    2.0_f64.powf((key as f64 - 69.0) / 12.0) * 440.0
}
