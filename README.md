# VEX EDIT STUDIO

Native Android video editor built with Kotlin and Jetpack Compose.

## Project identity

- **Application ID:** `com.vexedit.studio`
- **Minimum Android:** 8.0 (API 26)
- **Compile / Target SDK:** 35
- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Architecture direction:** Offline-first native Android

## Design foundation

VEX Hybrid combines approachable editing workflows with a multitrack-editor direction and the VEX visual identity.

- Accent: VEX Purple `#A855F7`
- Background: `#0F0F13`
- Panels: `#1C1B23`
- Music indicators: green
- Voice indicators: orange
- SFX indicators: cyan

## Build

The GitHub Actions workflow installs Gradle, generates the Gradle wrapper, runs unit tests, and assembles the Debug APK.

After the wrapper has been generated locally or by CI:

```bash
./gradlew test
./gradlew assembleDebug
```

Debug APK output:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Development roadmap

The project is developed in verified phases. Step 01 establishes the native Android foundation and CI build pipeline. Editing features will be implemented in later steps; this initial foundation is not yet a functional video editor.

## Current status

- [x] Native Android application module
- [x] Kotlin + Jetpack Compose configuration
- [x] VEX dark theme foundation
- [x] GitHub Actions test and Debug APK workflow
- [ ] Media import and playback
- [ ] Multitrack timeline
- [ ] Editing tools and export engine
