// Vendored from termux/termux-app · terminal-view (Apache-2.0)
plugins {
    id("com.android.library")
}

android {
    namespace = "com.termux.view"
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
    api(project(":terminal-emulator"))
    implementation("androidx.annotation:annotation:1.8.2")
}
