use bytes::{Buf, BufMut, Bytes, BytesMut};
use tokio_util::codec;

// Assumes buf has enough space to store the string and length prefix
pub fn pack_str(string: &str, buf: &mut BytesMut) {
    buf.put_u16(string.len() as u16);
    buf.put(string.as_bytes());
}

// TODO: Proper error handling
pub fn unpack_string(data: &[u8]) -> Option<String> {
    if data.len() < 2 {
        // Don't have length prefix
        return None;
    }

    let mut len_buf = [0u8; 2];
    len_buf.copy_from_slice(&data[0..2]);
    let len = u16::from_be_bytes(len_buf) as usize;
    if data.len() < 2 + len {
        return None;
    }

    let utf8_data = data[2..(2 + len)].to_vec();
    match String::from_utf8(utf8_data) {
        Ok(str) => Some(str),
        Err(_) => None,
    }
}

#[derive(Clone, Debug)]
pub struct Message {
    pub name: String,
    pub data: Bytes,
}

impl Message {
    pub fn pack(mut self) -> BytesMut {
        let mut buf = BytesMut::with_capacity(6 + self.name.len() + self.data.len());
        pack_str(&self.name, &mut buf);
        buf.put_i32(self.data.len() as i32);
        buf.put(&mut self.data);
        buf
    }
}

pub struct MessageDecoder;

impl codec::Decoder for MessageDecoder {
    type Item = Message;
    type Error = std::io::Error;

    fn decode(&mut self, src: &mut bytes::BytesMut) -> Result<Option<Self::Item>, Self::Error> {
        if src.len() < 2 {
            // Have not received type length prefix yet
            return Ok(None);
        }

        let mut name_len_bytes = [0u8; 2];
        name_len_bytes.copy_from_slice(&src[..2]);
        let name_len = u16::from_be_bytes(name_len_bytes) as usize;

        if src.len() < 2 + name_len + 4 {
            // Have not received name and data length yet
            src.reserve(2 + name_len + 4 - src.len());
            return Ok(None);
        }

        let mut data_len_bytes = [0u8; 4];
        data_len_bytes.copy_from_slice(&src[(2 + name_len)..(6 + name_len)]);
        let data_len = i32::from_be_bytes(data_len_bytes) as usize;

        if src.len() < 6 + name_len + data_len {
            // Have not received data yet
            src.reserve(6 + name_len + data_len - src.len());
            return Ok(None);
        }

        // If we reach here, we have the full message data

        let name_data = src[2..2 + name_len].to_vec();
        let data = BytesMut::from(&src[6 + name_len..6 + name_len + data_len]);
        src.advance(6 + name_len + data_len);

        let name = match String::from_utf8(name_data) {
            Ok(string) => Ok(string),
            Err(decode_err) => Err(std::io::Error::new(
                std::io::ErrorKind::InvalidData,
                decode_err,
            )),
        }?;

        Ok(Some(Message {
            name,
            data: data.into(),
        }))
    }
}
