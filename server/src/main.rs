use bytes::{Buf, BytesMut};
use cpal::traits::{DeviceTrait, HostTrait, StreamTrait};
use ringbuf::{HeapRb, traits::*};
use std::io;
use tokio::net::UdpSocket;

const PC_PORT: u16 = 12345;
const HEADER_SIZE: usize = 8;
const PAYLOAD_SIZE: usize = 1400; // 1400 bytes of i16 PCM data

#[tokio::main]
async fn main() -> io::Result<()> {
    // 1. Setup Audio Output
    let host = cpal::default_host();
    let device = host
        .default_output_device()
        .expect("Failed to find default output device");
    println!(
        "Output device: {}",
        device.name().unwrap_or("unknown".to_string())
    );

    let config = cpal::StreamConfig {
        channels: 2,
        sample_rate: cpal::SampleRate(48000),
        buffer_size: cpal::BufferSize::Default,
    };

    // Ring Buffer: Producer (UDP thread) -> Consumer (Audio Callback)
    // Capacity 48000 samples * 2 channels * 0.2s = ~19200 samples
    let ring = HeapRb::<i16>::new(48000 * 2);
    let (mut producer, mut consumer) = ring.split();

    let err_fn = |err| eprintln!("an error occurred on stream: {}", err);

    let stream = device
        .build_output_stream(
            &config,
            move |data: &mut [f32], _: &cpal::OutputCallbackInfo| {
                // Audio Callback
                for sample in data.iter_mut() {
                    if let Some(s) = consumer.try_pop() {
                        // Convert i16 to f32 range [-1.0, 1.0]
                        *sample = s as f32 / i16::MAX as f32;
                    } else {
                        *sample = 0.0; // Underrun silence
                    }
                }
            },
            err_fn,
            None,
        )
        .expect("Failed to build output stream");

    stream.play().expect("Failed to play stream");

    // 2. Setup UDP Socket
    let socket = UdpSocket::bind(format!("0.0.0.0:{}", PC_PORT)).await?;
    println!("Listening for audio on {}", socket.local_addr()?);

    let mut buf = BytesMut::with_capacity(HEADER_SIZE + PAYLOAD_SIZE);

    loop {
        let len = socket.recv_buf(&mut buf).await?;

        if len >= HEADER_SIZE {
            let seq_id = buf.get_i32();
            let timestamp = buf.get_i32();
            let mut pcm_data = buf.copy_to_bytes(len - HEADER_SIZE);

            // Convert raw little-endian bytes to i16 samples
            while pcm_data.remaining() >= 2 {
                let sample = pcm_data.get_i16_le(); // Android AudioRecord produces Little Endian
                let _ = producer.try_push(sample);
            }

            println!(
                "Received seq={}, ts={}, samples={}",
                seq_id,
                timestamp,
                (len - HEADER_SIZE) / 2
            );
        }
        buf.clear();
    }
}
