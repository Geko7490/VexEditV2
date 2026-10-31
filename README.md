# VEX EDIT STUDIO

Native Android video editor built with Kotlin and Jetpack Compose.

## Project identity

- **Application ID:** `com.vexedit.studio`
- **Minimum Android:** 8.0 (API 26)
- **Compile / Target SDK:** 35
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture direction:** Offline-first native Android

## VEX design system (Step 02)

- Brand accent: VEX Purple `#A855F7`
- Backgrounds: `#0F0F13` and `#101014`
- Panels: `#1C1B23` (raised panel: `#24222D`)
- Semantic timeline colors: music green `#22C55E`, voice orange `#F59E0B`, SFX cyan `#22D3EE`
- Text hierarchy: primary `#F7F5FA`, secondary `#AAA6B5`
- **Corner radius: 6–8 dp** for cards, buttons, chips, and Material shapes. Android UI dimensions use dp (density-independent pixels), not physical px.
- Shared Compose components live in `ui/components`; theme tokens, shapes, and typography live in `ui/theme`.

### Shared components

- `VexPanel`
- `VexSectionHeader`
- `VexPrimaryButton` / `VexSecondaryButton`
- `VexStatusChip`
- `VexTrackBadge` and `VexTrackKind`
- `VexIconGlyph`

The current launch screen is a design-system catalogue used to exercise these shared primitives. It is not yet the video editor; feature screens and navigation are later roadmap phases.

## Build and test

The GitHub Actions workflow installs Gradle, generates the Gradle wrapper, runs unit tests, and assembles the Debug APK.

```bash
./gradlew test
./gradlew assembleDebug
```

Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Development status

- [x] Step 01 — Native Android project foundation and CI
- [x] Step 02 — VEX Purple design tokens, dark theme, typography, shapes, and shared Compose components
- [ ] Media import and playback
- [ ] Multitrack timeline
- [ ] Editing tools and export engine
