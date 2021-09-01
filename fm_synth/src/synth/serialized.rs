use super::{adsr::Adsr, sinosc::SinOsc, DspNode, Program, ProgramState};
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize)]
pub struct ProgramDefinition {
    pub nodes: Vec<DspNodeEnum>,
    pub io: IO,
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
#[serde(tag = "type")]
pub enum DspNodeEnum {
    Adsr(Adsr),
    SinOsc(SinOsc),
}

#[derive(Serialize, Deserialize, Default)]
#[serde(default)]
pub struct IO {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub freq: Option<usize>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub gate: Option<usize>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub lchan: Option<usize>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub rchan: Option<usize>,
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum InPort {
    Link(usize),
    Const(f64),
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum OutPort {
    Link(usize),
    Unused,
}

impl InPort {
    pub fn read(&self, state: &ProgramState) -> f64 {
        match *self {
            Self::Link(i) => state.links[i],
            Self::Const(n) => n,
        }
    }
}

impl Default for InPort {
    fn default() -> Self {
        Self::Const(0.0)
    }
}

impl OutPort {
    pub fn write(&self, val: f64, state: &mut ProgramState) {
        if let OutPort::Link(i) = *self {
            state.links[i] = val
        }
    }
}

impl Default for OutPort {
    fn default() -> Self {
        Self::Unused
    }
}

impl From<ProgramDefinition> for Program {
    fn from(def: ProgramDefinition) -> Self {
        // map enum into trait object
        let dyn_nodes: Vec<Box<dyn DspNode>> = def
            .nodes
            .into_iter()
            .map(|x| -> Box<dyn DspNode> {
                match x {
                    DspNodeEnum::Adsr(x) => Box::new(x) as _,
                    DspNodeEnum::SinOsc(x) => Box::new(x) as _,
                }
            })
            .collect();
        Program {
            state: ProgramState::new(100),
            nodes: dyn_nodes,
            io: def.io,
            sample_rate: 44100,
        }
    }
}
