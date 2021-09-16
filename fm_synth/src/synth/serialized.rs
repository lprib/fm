use serde::Deserialize;

use super::{adsr::Adsr, mixer::Mixer, sinosc::SinOsc};

#[derive(Deserialize, Debug)]
pub struct PatchDefinition {
    pub nodes: Vec<DspNodeEnum>,
    pub io: IO,
}

#[derive(Deserialize, Debug)]
#[serde(rename_all = "lowercase")]
#[serde(tag = "type")]
pub enum DspNodeEnum {
    Adsr(Adsr),
    SinOsc(SinOsc),
    Mixer(Mixer),
}

#[derive(Deserialize, Default, Clone, Debug)]
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

