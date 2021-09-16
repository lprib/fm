use crossbeam_channel::Sender;
use serde::Deserialize;
use std::{
    net::{Ipv4Addr, TcpListener},
    thread,
};
use tungstenite::Message;

use crate::synth::serialized::PatchDefinition;

#[derive(Deserialize, Debug)]
#[serde(rename_all = "snake_case")]
pub enum ClientRequest {
    UpdatePatch(PatchDefinition),
    RequestWaveform,
}

pub fn start_websocket_server(sender: Sender<ClientRequest>) {
    thread::spawn(move || {
        let server = TcpListener::bind((Ipv4Addr::LOCALHOST, 8080)).unwrap();
        for stream in server.incoming() {
            let sender = sender.clone();
            thread::spawn(move || {
                let mut websocket = tungstenite::accept(stream.unwrap()).unwrap();

                loop {
                    if let Ok(Message::Text(text)) = websocket.read_message() {
                        let req = serde_json::from_str(&text);
                        if let Ok(req) = req {
                            sender.send(req).unwrap();
                            websocket
                                .write_message(Message::Text(
                                    "successfully recieved request".to_string(),
                                ))
                                .unwrap();
                        } else {
                            websocket
                                .write_message(Message::Text("malformed request".to_string()))
                                .unwrap();
                        }
                    }
                }
            });
        }
    });
}
