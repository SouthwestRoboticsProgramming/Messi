// TODO: Event logging (to messages and file)
// TODO: Clean up

use std::{collections::HashSet, error::Error, io::ErrorKind, time::Duration};

use bytes::Bytes;
use futures_util::StreamExt;
use messenger_protocol as protocol;
use protocol::Message;
use tokio::{
    io::{AsyncReadExt, AsyncWriteExt},
    net::{tcp::OwnedReadHalf, TcpListener, TcpStream},
    sync::broadcast,
};
use tokio_util::codec;

const MAX_QUEUED_MESSAGES: usize = 256;

struct HandlerSet {
    exact_names: HashSet<String>,
    prefix: HashSet<String>,
}

impl HandlerSet {
    fn new() -> Self {
        Self {
            exact_names: HashSet::new(),
            prefix: HashSet::new(),
        }
    }

    fn listen(&mut self, name: String) {
        if name.starts_with("*") {
            self.prefix.insert(name[1..].to_string());
        } else {
            self.exact_names.insert(name);
        }
    }

    fn unlisten(&mut self, name: String) {
        if name.starts_with("*") {
            self.prefix.remove(&name[1..]);
        } else {
            self.exact_names.remove(&name);
        }
    }

    fn is_listening(&self, name: &String) -> bool {
        if self.exact_names.contains(name) {
            return true;
        }

        for prefix in &self.prefix {
            if name.starts_with(prefix) {
                return true;
            }
        }

        false
    }
}

async fn read_name(read_half: &mut OwnedReadHalf) -> Result<String, Box<dyn Error>> {
    let mut name_len_buf = [0u8; 2];
    read_half.read_exact(&mut name_len_buf).await?;

    let mut name_buf = vec![0u8; u16::from_be_bytes(name_len_buf) as usize];
    read_half.read_exact(&mut name_buf).await?;

    Ok(String::from_utf8(name_buf)?)
}

// TODO: Figure out how to include client name in error
async fn handle_client(
    stream: TcpStream,
    broadcast_tx: broadcast::Sender<Message>,
    mut broadcast_rx: broadcast::Receiver<Message>,
) -> Result<(), Box<dyn Error>> {
    let (mut read_half, mut write_half) = stream.into_split();

    let packed_heartbeat = Message {
        name: "_Heartbeat".to_string(),
        data: Bytes::new(),
    }
    .pack();

    let client_name = read_name(&mut read_half).await?;
    println!("Got name: {}", client_name);

    let mut message_read = codec::FramedRead::new(read_half, protocol::MessageDecoder);
    let mut handlers = HandlerSet::new();

    loop {
        tokio::select! {
            result = broadcast_rx.recv() => match result {
                Ok(msg) => {
                    if handlers.is_listening(&msg.name) {
                        write_half.write_all(&msg.pack()).await?;
                    }
                }
                Err(e) => match e {
                    broadcast::error::RecvError::Lagged(_) => {
                        eprintln!("Message dropped for {}", client_name);
                    }
                    broadcast::error::RecvError::Closed => {
                        return Err(Box::new(e));
                    }
                }
            },
            result = message_read.next() => match result {
                Some(res) => {
                    let msg = res?;
                    match msg.name.as_str() {
                        "_Heartbeat" => {
                            // Respond with matching heartbeat
                            write_half.write_all(&packed_heartbeat).await?;
                        }
                        "_Listen" => if let Some(name) = protocol::unpack_string(&msg.data) {
                            println!("Client {} listening to {}", client_name, name);
                            handlers.listen(name);
                        }
                        "_Unlisten" => if let Some(name) = protocol::unpack_string(&msg.data) {
                            println!("Client {} no longer listening to {}", client_name, name);
                            handlers.unlisten(name);
                        }
                        "_Disconnect" => {
                            println!("Client {} disconnected", client_name);
                            return Ok(());
                        }
                        _ => {broadcast_tx.send(msg)?;}
                    }
                }
                None => return Err(Box::new(std::io::Error::from(ErrorKind::BrokenPipe)))
            },
            _ = tokio::time::sleep(Duration::from_secs_f32(5.0)) => {
                return Err(Box::new(std::io::Error::from(ErrorKind::TimedOut)));
            }
        }
    }
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn Error>> {
    println!("Opening port 5805 for Messenger");
    let listener = TcpListener::bind(("0.0.0.0", 5805)).await?;

    let (broadcast_tx, mut _broadcast_rx) = broadcast::channel(MAX_QUEUED_MESSAGES);

    // tokio::spawn(async move {
    //     loop {
    //         println!("Message: {:?}", broadcast_rx.recv().await);
    //     }
    // });

    // TODO: Respond to Messenger:GetClients

    println!("Listening for incoming connections");

    loop {
        match listener.accept().await {
            Ok((client_stream, _)) => {
                println!("Client has connected");
                let client_tx = broadcast_tx.clone();
                let client_rx = client_tx.subscribe();
                tokio::spawn(async move {
                    if let Err(e) = handle_client(client_stream, client_tx, client_rx).await {
                        eprintln!("Client failed with error: {}", e);
                    }
                });
            }
            Err(e) => {
                eprintln!("Error accepting incoming connection: {}", e);
            }
        }
    }
}
