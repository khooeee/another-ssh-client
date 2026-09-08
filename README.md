# Another SSH Client

Native Kotlin / Jetpack Compose SSH client aimed at Daylight DC-1's greyscale Live Paper display.

## Features

- Multiple concurrent sessions
- Customize start directory per host
- Hosts & tabs can be reordered by dragging
- Keyboard driven
  - Ctrl+Tab / Ctrl+Shift+Tab to cycle sessions
  - Ctrl+Shift+N to open a new session
  - Ctrl+Shift+R to rename the current session
- Pinch-to-zoom font size

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

## License

Another SSH Client app code is [MIT](LICENSE).

Vendored `terminal-emulator` / `terminal-view` modules are **Apache-2.0** (from [termux/termux-app](https://github.com/termux/termux-app), originally Android Terminal Emulator).
