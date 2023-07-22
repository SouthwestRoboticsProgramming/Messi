mod client {
    use std::error::Error;

    use bytes::BytesMut;
    use messenger_protocol::Message;
    use tokio::net::ToSocketAddrs;

    pub struct MessengerClient<A> {
        addr: A,
        name: String,
        conn: Option<Connection>,
    }

    impl<A> MessengerClient<A> {
        pub fn new(addr: A, name: String) -> Self
        where
            A: ToSocketAddrs,
        {
            Self {
                addr,
                name,
                conn: None,
            }
        }

        pub async fn read_message(&mut self) -> Option<Message> {
            todo!();
        }

        async fn send_message(&mut self, msg: Message) {
            if let Some(conn) = &mut self.conn {
                if let Err(e) = conn.send_message(msg).await {
                    eprintln!("Connection lost: {}", e);
                    self.conn = None;
                }
            }
        }

        pub async fn send(&mut self, name: String) {
            self.prepare(name).send().await;
        }

        pub fn prepare(&mut self, name: String) -> MessageBuilder<A> {
            MessageBuilder {
                client: self,
                name,
                data: BytesMut::new(),
            }
        }

        pub async fn disconnect(self) {
            if let Some(conn) = self.conn {
                conn.disconnect().await;
            }
        }

        pub fn is_connected(&self) -> bool {
            self.conn.is_some()
        }
    }

    struct Connection {}

    impl Connection {
        async fn send_message(&mut self, msg: Message) -> Result<(), Box<dyn Error>> {
            todo!();
        }

        async fn disconnect(self) {
            todo!();
        }
    }

    pub struct MessageBuilder<'client, A> {
        client: &'client mut MessengerClient<A>,
        name: String,
        data: BytesMut,
    }

    impl<A> MessageBuilder<'_, A> {
        // TODO: Data put functions

        pub async fn send(self) {
            self.client
                .send_message(Message {
                    name: self.name,
                    data: self.data.freeze(),
                })
                .await;
        }
    }
}

fn main() {}
