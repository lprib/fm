use serde::Deserialize;

use crate::synth::voice::ProgramState;

#[derive(Deserialize, Clone, Debug, Default)]
pub struct InPort {
    mult: f64,
    bias: f64,
    #[serde(skip_serializing_if = "Option::is_none")]
    link: Option<usize>,
}

#[derive(Deserialize, Clone, Debug, Default)]
#[serde(rename_all = "lowercase")]
pub struct OutPort {
    #[serde(skip_serializing_if = "Option::is_none")]
    link: Option<usize>,
}

pub trait Port {
    fn read(&self, state: &ProgramState) -> f64;
}

impl Port for InPort {
    #[inline]
    fn read(&self, state: &ProgramState) -> f64 {
        match self.link {
            None => self.bias,
            Some(i) => state.links[i] * self.mult + self.bias,
        }
    }
}

impl Port for OutPort {
    #[inline]
    fn read(&self, state: &ProgramState) -> f64 {
        match self.link {
            Some(idx) => state.links[idx],
            None => Default::default(),
        }
    }
}

impl OutPort {
    #[inline]
    pub fn write(&self, val: f64, state: &mut ProgramState) {
        if let Some(i) = self.link {
            state.links[i] = val
        }
    }
}
