use std::iter::{repeat, repeat_with};

use crossbeam_channel::Receiver;
use rodio::Source;

pub use serialized::PatchDefinition;

use self::voice::Program;

#[macro_use]
mod dsp_node;
mod adsr;
mod mixer;
mod sinosc;
mod port;
mod voice;
mod serialized;

const SAMPLE_RATE: u32 = 44100;
const SAMPLE_PERIOD: f64 = 1.0 / SAMPLE_RATE as f64;

#[derive(Debug)]
pub enum SynthInputEvent {
    KeyDown { key: u8, freq: f64 },
    KeyUp { key: u8 },
}

pub struct Patch {
    voices: Vec<Program>,
    voice_assignments: Vec<Option<u8>>,
    event_rx: Receiver<SynthInputEvent>,
    // number used to uniquely identify this patch
    pub index: usize,
}

impl Patch {
    pub fn new(def: PatchDefinition, event_rx: Receiver<SynthInputEvent>, index: usize) -> Self {
        let num_voices = 9;
        Self {
            voices: repeat_with(|| Program::new(&def))
                .take(num_voices)
                .collect(),
            voice_assignments: repeat(None).take(num_voices).collect(),
            event_rx,
            index,
        }
    }

    pub fn handle_event(&mut self, event: SynthInputEvent) {
        match event {
            SynthInputEvent::KeyDown { key, .. } => {
                let unused_voice_idx = self.voice_assignments.iter().position(|k| *k == None);
                if let Some(unused_voice_idx) = unused_voice_idx {
                    self.voices[unused_voice_idx].process_event(event);
                    self.voice_assignments[unused_voice_idx] = Some(key);
                }
            }
            SynthInputEvent::KeyUp { key } => {
                // TODO use least-recently used algorithm here so new voices dont clobber the
                // release of current voices
                // alternatively, let voices signal when theyre "done"
                let voice_idx = self.voice_assignments.iter().position(|k| *k == Some(key));
                if let Some(voice_idx) = voice_idx {
                    self.voices[voice_idx].process_event(event);
                    self.voice_assignments[voice_idx] = None;
                }
            }
        }
    }
}

impl Iterator for Patch {
    type Item = f32;

    fn next(&mut self) -> Option<Self::Item> {
        // get all events in the queue
        while let Ok(event) = self.event_rx.try_recv() {
            self.handle_event(event);
        }
        Some(
            self.voices.iter_mut().filter_map(|v| v.next()).sum::<f32>() / self.voices.len() as f32,
        )
    }
}

impl Source for Patch {
    fn current_frame_len(&self) -> Option<usize> {
        None
    }

    fn channels(&self) -> u16 {
        2
    }

    fn sample_rate(&self) -> u32 {
        SAMPLE_RATE
    }

    fn total_duration(&self) -> Option<std::time::Duration> {
        None
    }
}
