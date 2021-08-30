use std::f64::consts::PI;

use rodio::{buffer::SamplesBuffer, OutputStream, Sink};

fn main() {
    let p = Program {
        osc1: Osc {
            mult: 1.0,
            a: 0.0,
            d: 0.0,
            s: 1.0,
            r: 0.2,
        },
        osc2: Osc {
            mult: 1.0,
            a: 0.0,
            d: 1.0,
            s: 0.0,
            r: 0.0,
        },
        mod_amount: 5.5,
    };

    let sample_rate = 44100;
    let samples: Vec<_> = (1..2 * sample_rate)
        .map(|n| n as f64 / sample_rate as f64)
        .map(|t| p.query(t))
        .map(|n| n as f32)
        .collect();
    let buf = SamplesBuffer::new(1, sample_rate, samples);

    let (_stream, handle) = OutputStream::try_default().unwrap();
    let sink = Sink::try_new(&handle).unwrap();
    sink.append(buf);
    sink.sleep_until_end();
}

fn sine(freq: f64, phase: f64, t: f64) -> f64 {
    (2.0 * PI * freq * t + phase).sin()
}

fn adsr(a: f64, d: f64, s: f64, r: f64, note_len: f64, t: f64) -> f64 {
    if t <= 0.0 {
        0.0
    } else if t <= a {
        t / a
    } else if t <= a + d {
        1.0 - ((t - a) / d * (1.0 - s))
    } else if t <= note_len {
        s
    } else if t <= note_len + r {
        s - ((t - note_len) / r * s)
    } else {
        0.0
    }
}

struct Osc {
    mult: f64,
    a: f64,
    d: f64,
    s: f64,
    r: f64,
}

impl Osc {
    pub fn query(&self, note_len: f64, freq: f64, modulation: f64, t: f64) -> f64 {
        adsr(self.a, self.d, self.s, self.r, note_len, t) * sine(freq * self.mult, modulation, t)
    }
}

struct Program {
    osc1: Osc,
    osc2: Osc,
    mod_amount: f64,
}

impl Program {
    pub fn query(&self, t: f64) -> f64 {
        let out = self.mod_amount * self.osc2.query(1.0, 440.0, 0.0, t);
        self.osc1.query(1.0, 440.0, out, t)
    }
}
