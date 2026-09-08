# Another SSH Client

Android SSH client aimed at Daylight DC-1's greyscale Live Paper display.

## Features

- Multiple concurrent sessions
- Customize start command per host (e.g. `cd ~/code && tmux a`)
- Hosts & sessions can be reordered by dragging
- Double press the session tab to rename it
- Keyboard driven
  - Ctrl+Tab / Ctrl+Shift+Tab to cycle sessions
  - Ctrl+Shift+N to open a new session
  - Ctrl+Shift+R to rename the current session
  - Ctrl+Shift+V to paste from clipboard
- Extra-keys bar for Esc / Tab / Ctrl / Alt / arrows (shown with the soft keyboard)
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
