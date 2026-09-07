# ssher — SSH client for Daylight DC-1

Native Kotlin / Jetpack Compose SSH client aimed at Daylight's greyscale Live Paper display.
Password auth via pure Java [sshj](https://github.com/hierynomus/sshj). Sideload the APK (no Play Store).

## Features (v0.1)

- Saved hosts (name, host, port, user) — passwords are not stored
- Password authentication (optional Keystore-encrypted saved password per host)
- Interactive shell with line input, Enter / Ctrl+C / Ctrl+D
- High-contrast greyscale UI

Passwords are AES-256-GCM encrypted with a key in the Android Keystore (hardware-backed when the device supports it). Host metadata stays in plain DataStore; secrets never do. App backup is disabled so credentials aren't copied off-device. This protects against casual file inspection — not a rooted device or malware running as the app.

## Build

Requires JDK 17+ and Android SDK (platform 34).

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew :app:assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Sideload on DC-1

1. Enable install from unknown sources / ADB debugging on the device.
2. Copy the APK over USB, network, or:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Notes

- Host keys are currently accepted without pinning (convenient for first use; tighten later).
- Terminal is line-oriented with ANSI stripped for paper readability — not a full curses emulator yet.
