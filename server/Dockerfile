
# Dockerfile for MobileSoundForward Server

# Builder stage
FROM rust:bookworm as builder

WORKDIR /usr/src/app

# Install build dependencies for cpal (ALSA)
RUN apt-get update && apt-get install -y pkg-config libasound2-dev && rm -rf /var/lib/apt/lists/*

COPY . .
RUN cargo install --path .

# Runtime stage
FROM debian:bookworm-slim

# Install runtime dependencies for audio
RUN apt-get update && apt-get install -y libasound2 && rm -rf /var/lib/apt/lists/*

COPY --from=builder /usr/local/cargo/bin/server /usr/local/bin/server

# Expose the port defined in main.rs
EXPOSE 12345

# Note: To access host audio, run with:
# docker run --device /dev/snd -p 12345:12345/udp mobile-sound-forward-server
CMD ["server"]
