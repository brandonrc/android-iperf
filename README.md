# iPerf3 for Android

A native Android client for [iperf3](https://iperf.fr/) network bandwidth testing. Run TCP and UDP speed tests against any iperf3 server directly from your phone.

<p align="center">
  <img src="docs/demo.gif" alt="iPerf3 Android Demo" width="300"/>
</p>

## Features

- **Speed Testing** — Run TCP/UDP bandwidth tests with a real-time speedometer gauge
- **Server Mode** — Turn your device into an iperf3 server to accept incoming tests
- **Test History** — Track all past results with detailed metrics stored locally
- **Share Reports** — Export results as formatted text or CSV via the system share sheet
- **Dark Mode** — Full dark theme support, follows system or manual toggle
- **Advanced Options** — Parallel streams, reverse mode, bidirectional, bandwidth limiting, custom duration

## Screenshots

| Test Screen | History | Server Mode | Settings |
|:-:|:-:|:-:|:-:|
| Speedometer gauge with live bandwidth | Past results with quality scores | Start/stop server with connection log | Defaults, appearance, data management |

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17+
- Android SDK 34+
- An iperf3 server to test against (e.g. `iperf3 -s` on any machine)

### Build

```bash
git clone git@github.com:brandonrc/android-iperf.git
cd android-iperf
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Run an iperf3 server

On any machine on your network:

```bash
iperf3 -s
```

Then enter that machine's IP address in the app and tap Start.

## Architecture

```
app/src/main/kotlin/com/iperf3/android/
├── data/                    # Data layer
│   ├── repository/          # Repository implementations
│   ├── service/             # Foreground test service
│   ├── source/local/        # Room database, DAOs, entities
│   └── source/remote/       # iperf3 protocol, sockets
├── di/                      # Hilt dependency injection modules
├── domain/                  # Domain layer
│   ├── model/               # TestResult, TestConfiguration, etc.
│   ├── repository/          # Repository interfaces
│   └── util/                # Report generation
└── presentation/            # Presentation layer
    ├── ui/component/        # SpeedGauge, ResultCard
    ├── ui/navigation/       # Bottom nav, NavHost
    ├── ui/screen/           # Test, Server, History, Settings
    ├── ui/theme/            # Material3 theme, colors
    └── viewmodel/           # ViewModels for each screen
```

**Stack:** Kotlin, Jetpack Compose, Material3, Hilt, Room, DataStore, Coroutines/Flow, WorkManager

## Tech Details

- **Protocol**: Pure Kotlin iperf3 protocol implementation (no native binaries)
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Build**: AGP 8.5.2, Kotlin 1.9.24, Gradle 8.7

## Metrics Tracked

| Metric | TCP | UDP |
|--------|:---:|:---:|
| Bandwidth (avg/min/max) | ✓ | ✓ |
| Data transferred | ✓ | ✓ |
| Retransmits | ✓ | — |
| Jitter | — | ✓ |
| Packet loss | — | ✓ |
| Quality score | ✓ | ✓ |

## License

This project is not affiliated with or endorsed by the official iperf3 project.
