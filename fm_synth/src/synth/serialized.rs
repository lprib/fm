use super::{adsr::Adsr, mixer::Mixer, sinosc::SinOsc, ProgramState};
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

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct InPort {
    mult: f64,
    bias: f64,
    #[serde(skip_serializing_if = "Option::is_none")]
    link: Option<usize>,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
#[serde(rename_all = "lowercase")]
pub struct OutPort {
    #[serde(skip_serializing_if = "Option::is_none")]
    link: Option<usize>,
}

impl InPort {
    #[inline]
    pub fn read(&self, state: &ProgramState) -> f64 {
        match self.link {
            None => self.bias,
            Some(i) => state.links[i] * self.mult + self.bias
        }
    }
}

impl Default for InPort {
    fn default() -> Self {
        Self { mult: 0.0, bias: 0.0, link: None }
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

impl Default for OutPort {
    fn default() -> Self {
        Self { link: None }
    }
}

/// Define a node type with inputs, outputs, and fields. This automatically implements Deserialize.
/// Syntax:
/// ```
/// node_definition! {
///     #[OptionalAttribute1]
///     #[OptionalAttribute2]
///     NodeName(input1, input2 => output1, output2) {
///         pub additionalField1: Type,
///         additionalField2: Type
///     }
/// }
/// ```
/// The braced block with additional fields is optional.
macro_rules! node_definition {
    (
        $(#[$attribute:meta])*
        $structName:ident( $($inputName:ident),* => $($outputName:ident),*) $( {
            $(
            $fieldVisibility:vis $fieldName:ident: $fieldType:ty
            ),+
            // accept possible trailing comma
            $(,)?
        } )?
    ) => {
        use serde::{Deserialize, de::Deserializer};

        $(#[$attribute])*
        pub struct $structName {
            $( $inputName: InPort, )*
            $( $outputName: OutPort, )*
            $($(
                $fieldVisibility $fieldName: $fieldType,
            )*)?
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
