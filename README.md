# VEX EDIT STUDIO

Native Android video editor built with Kotlin and Jetpack Compose.

## Project identity

- **Application ID:** `com.vexedit.studio`
- **Minimum Android:** 8.0 (API 26)
- **Compile / Target SDK:** 35
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** Offline-first, repository-based Android app

## VEX design system

- Brand accent: VEX Purple `#A855F7`
- Backgrounds: `#0F0F13` and `#101014`
- Panels: `#1C1B23` (raised panel: `#24222D`)
- Semantic timeline colors: music green `#22C55E`, voice orange `#F59E0B`, SFX cyan `#22D3EE`
- **Corner radius: 6–8 dp** for cards, buttons, chips, and Material shapes.
- Shared Compose components live in `ui/components`; theme tokens live in `ui/theme`.

## Step 03 — Home and navigation

- Native Compose bottom navigation: Home, Projects, Create, Audio, Settings.
- Home dashboard with create-project action, recent projects, and quick access.
- Project library with search and Grid/List layouts.
- Project cards expose open, rename, duplicate, and move-to-trash actions.

## Step 04 — Project management foundation

- Room database stores project metadata locally and works offline.
- Project repository and ViewModel separate UI from data operations.
- Create projects with name, aspect ratio, resolution, and FPS settings.
- Trash supports restore and permanent deletion.
- Project metadata survives app restarts.

### Current implementation boundaries

- Opening a project currently confirms and displays its saved project settings; the video timeline/editor is a later roadmap phase.
- Project media file manifests, internal/external folder selection, JSON and binary project serialization, autosave/recovery, and manual `.vexbackup` export/import are planned follow-up increments. The current Room database stores project metadata only; it does not claim to save edit timelines yet.
- Project resolution/FPS are project targets. Export must validate actual device and codec capabilities.

## Build and test

GitHub Actions installs Gradle, generates the Gradle wrapper, runs unit tests, and assembles the Debug APK.

```bash
./gradlew test
./gradlew assembleDebug
```

Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Development status

- [x] Step 01 — Native Android project foundation and CI
- [x] Step 02 — VEX design system and shared Compose components
- [x] Step 03 — Home dashboard, bottom navigation, and project library
- [x] Step 04 foundation — Room-backed project metadata, create/rename/duplicate/trash/restore/delete
- [ ] Step 04 follow-up — Internal/external project file storage, JSON + binary serialization, autosave/recovery, manual backup archive
- [ ] Step 05 — Media import and library
- [ ] Step 06 — Video playback and preview
- [ ] Step 07 — Multitrack timeline
- [ ] Step 08 — Editing tools and export engine
