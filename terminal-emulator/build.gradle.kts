// Vendored from termux/termux-app · terminal-emulator (Apache-2.0)
// Local PTY/JNI removed; app connects via TerminalTransport (SSH).
plugins {
    id("com.android.library")
}

android {
    namespace = "com.termux.emulator"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("proguard-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.8.2")
}
