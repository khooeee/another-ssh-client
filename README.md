# ssher — SSH client for Daylight DC-1

Native Kotlin / Jetpack Compose SSH client aimed at Daylight's greyscale Live Paper display.
Password auth via pure Java [sshj](https://github.com/hierynomus/sshj). Terminal rendering via
vendored Termux `terminal-view` / `terminal-emulator` (Apache-2.0). Sideload the APK (no Play Store).

## Features (v0.2)

- Saved hosts (name, host, port, user)
- Optional Keystore-encrypted saved password per host
- Real VT/xterm terminal (char-at-a-time input, cursor control, TUIs / coding agents)
- Window resize forwarded to the remote PTY
- Greyscale “paper” color scheme for Live Paper
- Pinch-to-zoom font size

## Architecture

- **sshj** — SSH connect, auth, shell, window-change
- **Termux terminal-*** — VT emulator + `TerminalView` only (no local Termux shell / OpenSSH)

## Build

Requires JDK 17+ and Android SDK (platform 34).

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew :app:assembleDebug
```

APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Sideload on DC-1

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Notes

- Host keys are currently accepted without pinning (tighten later).
- Passwords use AES-256-GCM + Android Keystore; app backup is disabled.
- Terminal modules are vendored from termux/termux-app (via a transport-agnostic session adapted like moke).
