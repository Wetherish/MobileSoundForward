# MobileSoundForward

MobileSoundForward is a lightweight tool designed to stream system audio from your Android device directly to your PC server with minimal latency. Ideally suited for casting audio while keeping video on your mobile device, or for unified audio management.

## Features

-   **Low Latency**: Uses UDP for fast audio transmission.
-   **High Quality**: Supports 48kHz, 16-bit PCM stereo audio.
-   **Lightweight**: Minimal overhead on both the mobile client and the desktop server.
-   **Cross-Platform Support**: Mobile client built with Kotlin Multiplatform (Android/iOS) and server built in Rust.

## Getting Started

### Prerequisites

-   **Android Device**: Running Android 10 or later (for internal audio capture support).
-   **PC**: To run the server (Linux/Windows/macOS) with Rust installed.
-   **Network**: Both devices must be on the same local network.

### Server (Receiver)

The server receives the audio packets and plays them. It is written in Rust.

1.  Navigate to the server directory:
    ```bash
    cd server
    ```
2.  Run the server:
    ```bash
    cargo run
    ```
    *Note: You can also use the `make desktop` command from the root directory to run the server.*

### Mobile App (Sender)

The mobile application captures system audio and sends it to the server.

1.  Open the `mobile` directory in Android Studio.
2.  Build and run the `composeApp` on your Android device.
3.  Enter your PC's IP address in the app and start streaming.

## Technical Details

-   **Sample Rate**: 48000 Hz
-   **Channels**: 2 (Stereo)
-   **Bit Depth**: 16-bit PCM
-   **Protocol**: Custom UDP packet structure
    -   Header: Sequence Number (4 bytes), Timestamp (4 bytes)
    -   Payload: PCM Audio Data (1400 bytes)

## License

[MIT License](LICENSE)
