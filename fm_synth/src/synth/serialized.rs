use super::{adsr::Adsr, mixer::Mixer, sinosc::SinOsc, voice::ProgramState};
use serde::{Deserialize, Serialize};

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

#[derive(Serialize, Deserialize, Default, Clone, Debug)]
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

#[derive(Serialize, Deserialize, Clone, Debug, Default)]
pub struct InPort {
    mult: f64,
    bias: f64,
    #[serde(skip_serializing_if = "Option::is_none")]
    link: Option<usize>,
}

#[derive(Serialize, Deserialize, Clone, Debug, Default)]
#[serde(rename_all = "lowercase")]
pub struct OutPort {
    #[serde(skip_serializing_if = "Option::is_none")]
    link: Option<usize>,
}

// TODO move to ports module?
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

/// Define a node type with inputs, outputs, and fields. This automatically implements Deserialize.
/// Syntax:
/// ```
/// node_definition! {
///     #[OptionalAttribute1]
///     #[OptionalAttribute2]
///     NodeName(input1, input2 => output1, output2) {
///         #[OptionalAttributeOrDocComment]
///         pub additionalField1: Type,
///         #[OptionalAttributeOrDocComment]
///         additionalField2: Type
///     }
/// }
/// ```
/// The braced block with additional fields is optional.
macro_rules! node_definition {
    (
        $(#[$attribute:meta $($attributeArgs:tt)* ])*
        $structName:ident( $($inputName:ident),* => $($outputName:ident),*) $( {
            $(

            $( #[ $fieldAttribute:meta $($fieldAttributeArgs:tt)* ] )*
            $fieldVisibility:vis $fieldName:ident: $fieldType:ty

            ),+
            // accept possible trailing comma
            $(,)?
        } )?
    ) => {
        use serde::{Deserialize, de::Deserializer};

        #[derive(Default, Debug, Clone)]
        struct ResolvedInputs {
            $( $inputName: f64, )*
        }

        $(#[$attribute $($attributeArgs)* ])*
        pub struct $structName {
            resolved: ResolvedInputs,
            $( $inputName: InPort, )*
            $( $outputName: OutPort, )*
            $($(
                $( #[ $fieldAttribute $($fieldAttributeArgs)* ] )*
                $fieldVisibility $fieldName: $fieldType,
            )*)?
        }

        impl $structName {
            fn resolve_inputs(&mut self, state: &ProgramState) {
                use crate::synth::serialized::Port;
                $(
                    self.resolved.$inputName = self.$inputName.read(state);
                )*
            }
        }

        impl<'de> Deserialize<'de> for $structName {
            fn deserialize<D>(deserializer: D) -> Result<Self, D::Error> where D: Deserializer<'de> {
                #[derive(Deserialize)]
                struct Inputs {
                    $( $inputName: InPort, )*
                }

                #[derive(Deserialize)]
                struct Outputs {
                    $( $outputName: OutPort, )*
                }

                #[derive(Deserialize)]
                struct UnFlattened {
                    inputs: Inputs,
                    outputs: Outputs
                }

                let unflattened = UnFlattened::deserialize(deserializer)?;
                Ok($structName {
                    $( $inputName: unflattened.inputs.$inputName, )*
                    $( $outputName: unflattened.outputs.$outputName, )*
                    .. Default::default()
                })
            }
        }
    }
}
