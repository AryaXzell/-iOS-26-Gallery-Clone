# Gallery — iOS 26 Photos Native Android Clone

A native Android clone of the **iOS 26 Photos** application built with **Jetpack Compose**, **Material 3**, **Media3 ExoPlayer**, and a custom **Liquid Glass Design System**.

---

## Features and Architecture

### Liquid Glass Design System
* **Three Precision Tiers**:
  * **Controls Glass** (`18dp` radius, `14dp` blur): Floating action blobs, icon buttons, pill switches.
  * **Standard Glass** (`26dp` radius, `28dp` blur): Modal sheets, info drawers, section containers.
  * **Utility Glass** (`16dp` radius, `10dp` blur): Auto-hiding full-screen toolbars and overlay badges.
* **Specular Edge Highlights**: 0.5dp top-light specular borders with ambient shadow gradients.
* **Accessibility Settings**: Real-time toggles for **Reduce Transparency** (solid surface fallback) and **Reduce Motion** (instant transitions).

---

### Core Application Modules

```
com.aryaxzell.gallery/
├── MainActivity.kt                  # Root screen switcher and system navigation
├── core/
│   ├── common/                      # CompositionLocal settings and theme utilities
│   ├── data/
│   │   ├── MediaItem.kt             # Domain models (Photo, Video, EXIF, Edit adjustments)
│   │   ├── MediaRepository.kt       # MediaStore querying and Room synchronization
│   │   └── db/                      # Room database (Albums, Searches, Favorites, Edits)
│   └── designsystem/
│       ├── GlassTokens.kt           # Radius, blur, and alpha specifications
│       ├── GlassSurface.kt          # Liquid glass surface composable and specular shader
│       └── GlassBlobTabBar.kt       # iOS 26 bottom-left floating blob switcher
└── feature/
    ├── library/                     # Edge-to-edge chronological photo grid and filters
    ├── collections/                 # 8 modular sections (Recent Days, People & Pets, Albums)
    ├── search/                      # Real-time search, category tiles and search history
    ├── detail/                      # Fullscreen detail viewer, EXIF sheet and Media3 video player
    ├── edit/                        # Adjustments, color filters, crop/rotate and markup canvas
    ├── memories/                    # Ken Burns cinematic slideshow player
    └── settings/                    # Accessibility and display preferences
```

---

### Key Capabilities
1. **Library**:
   - Edge-to-edge chronological photo grid grouped with sticky month headers.
   - Density switcher toggle (3-column vs 5-column grid).
   - Filter sheet modal (Only Edited, Hide Screenshots).
   - Multi-selection action bar (Batch Share, Favorite, Delete).

2. **Collections**:
   - 8 structured sections: **Recent Days**, **People & Pets**, **Pinned Collections**, **Memories**, **Trips**, **Albums** (with custom album creation modal), **Media Types**, and **Utilities**.
   - Collapsible sections with animated chevron state indicators.

3. **Search**:
   - Natural language search query matching against titles, locations, dates, and categories.
   - Quick filter chips (All, Photos, Videos, Screenshots, Favorites).
   - Search history management with quick clear.

4. **Detail & Video Player**:
   - Pinch-to-zoom and pan gesture support.
   - Auto-hiding Utility glass toolbars on single tap.
   - Full EXIF inspector (Camera model, lens, ISO, shutter, aperture, resolution, GPS).
   - **Media3 ExoPlayer**: Centered glass playback controls, mute button, and fine-grain scrub gestures.
   - Spatial 3D parallax tilt toggle.

5. **Non-Destructive Photo Editor**:
   - **Adjust**: Exposure, Brilliance, Highlights, Contrast, Brightness, Saturation, Warmth.
   - **Filters**: Original, Vivid, Dramatic, Mono, Silvertone, Noir with intensity tuning.
   - **Transform**: 90° rotation, horizontal flip, and selectable crop aspect ratios (Original, Square, 16:9, 4:3, 3:2).
   - **Markup**: Freehand drawing canvas with color palette and undo.
   - Touch-and-hold canvas to compare against original.

6. **Memories Player**:
   - Fullscreen Ken Burns pan-and-zoom animation loop with soundtrack indicator.

---

## Tech Stack and Dependencies

* **Language**: Kotlin 2.0+
* **UI Toolkit**: Jetpack Compose (BOM 2024+)
* **Design System**: Material Design 3 (M3)
* **Local Persistence**: Room Database (`androidx.room`) with KSP
* **Image Loading**: Coil Compose 2.7.0
* **Media Playback**: Media3 ExoPlayer 1.4.1
* **Testing**: Robolectric (JVM local tests) and Roborazzi (Screenshot regression tests)

---

## Building the Project

### Prerequisites
* **JDK 17** or higher
* **Android SDK 36** (Compile SDK: 36)
* Android Studio Ladybug / Meerkat or Gradle CLI

### 1. Build Standard Debug APK
```bash
./gradlew assembleDebug
```
*Output location*: `app/build/outputs/apk/debug/app-debug.apk`

### 2. Build 3 Split Architectures + Universal APK
To generate distinct APKs optimized per architecture:
```bash
./gradlew assembleDebug -PsplitApks
```
This produces 3 types of APKs:
* **Universal APK**: Supports all CPU architectures (ARM and x86).
* **64-bit APK (`arm64-v8a`)**: Optimized for modern Android smartphones (smaller download size).
* **32-bit APK (`armeabi-v7a`)**: Optimized for legacy Android devices.
* **x86 / x86_64 APKs**: For emulators and ChromeOS devices.

### 3. Run Unit and Screenshot Tests
```bash
./gradlew :app:testDebugUnitTest
```

---

## GitHub Actions CI/CD Workflow

The repository includes an automated workflow at `.github/workflows/build-apks.yml` that triggers on every commit, tag, or manual dispatch.

### Automated Workflow Pipeline:
1. **Environment Setup**: Provisions JDK 17 and Android SDK build tools.
2. **Multi-Architecture Build**: Executes `./gradlew assembleDebug -PsplitApks` (or `assembleRelease` on tag release).
3. **Artifact Categorization**:
   * `Gallery-Universal-APK`: Universal APK installable on all hardware.
   * `Gallery-64bit-arm64-APK`: 64-bit ARM APK (`arm64-v8a`).
   * `Gallery-32bit-arm-APK`: 32-bit ARM APK (`armeabi-v7a`).
   * `Gallery-All-Architectures-APKs`: Complete package of all split builds.
4. **GitHub Releases Integration**: Automatically creates a GitHub Release when pushing a version tag (e.g. `v1.0.0`) and attaches all 3 APK variants.

### Configuring Release Signing Secrets
To successfully build and sign release APKs on version tag pushes, you must configure the following **GitHub Actions Secrets** in your repository settings (**Settings > Secrets and variables > Actions**):

* **`RELEASE_KEYSTORE_BASE64`**: The base64-encoded string of your release `.jks` keystore file.
  * To generate this on macOS/Linux: `base64 -i my-upload-key.jks | tr -d '\n'`
  * To generate on Windows (PowerShell): `[Convert]::ToBase64String([IO.File]::ReadAllBytes("my-upload-key.jks"))`
* **`KEYSTORE_PASSWORD`**: The store password configured for your release keystore.
* **`KEY_PASSWORD`**: The key password configured for the private key in the release keystore.

When these secrets are set, the workflow will automatically decode the keystore and pass the credentials to Gradle during release builds, ensuring signed split and universal APKs are created and attached to the GitHub Release. If these secrets are not configured, the workflow will fallback gracefully or fail standard signing validation on release builds.

---

## License

This project is licensed under the MIT License - see the LICENSE file for details.
