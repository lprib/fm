use crate::synth::voice::ProgramState;

pub trait DspNode {
    fn next_sample(&mut self, state: &mut ProgramState);
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

        // ResolvedInputs is a struct with values corresponding to all InputPorts
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
            /// Read all input ports and store their results in self.resolved
            fn resolve_inputs(&mut self, state: &ProgramState) {
                use crate::synth::port::Port;
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
