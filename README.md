# The Great Library of Arts

A quiet, offline-first museum for your pocket. **The Great Library of Arts** gathers public-domain and openly licensed works from eleven museum APIs into one carefully normalized library, then gives you slow, private, on-device ways to look at them.

There is no account, no advertising SDK, no analytics SDK and no server of ours in the viewing path. Artwork images and metadata come directly from the holding museums, or from signed offline packs you choose to install.

<p align="center">
  <em>Kotlin · Jetpack Compose · Material 3 · Clean Architecture · GPLv3</em>
</p>

---

## What is here

### One library, eleven museums

| Source | API | Notes |
| --- | --- | --- |
| The Metropolitan Museum of Art | Collection API | Degrades gracefully when the endpoint is unavailable (it currently returns HTTP 410) |
| Art Institute of Chicago | Art Institute API | Public domain filter |
| Rijksmuseum | Rijksmuseum API | Key optional; source disabled without one |
| Cleveland Museum of Art | Open Access API | CC0 filter |
| Smithsonian | Open Access API | Key optional |
| National Gallery of Art | NGA API | Key optional |
| Europeana | Search API | Key optional |
| Harvard Art Museums | API | Key optional |
| Paris Musées | Collections API | Key optional |
| Getty | Museum API | Key optional |
| Wikimedia Commons | MediaWiki API | Rate-limit aware; uses thumbnail endpoints |

Every record is normalized into one `Artwork` model: title, artist, date, genre, period, movement, medium, dimensions, museum, room/location, palette, image tiles, description, licence, credit line, provenance, audio, related works and annotations. **Licence, credit and provenance are shown on every artwork screen** — there is no unlabelled image in the library.

The first install ships with a hand-curated seed collection and learning content, so Home, Explore, the Journal, Story Tours, Art School and the games all work with no connection.

### Home

- A daily hero artwork, with a slow fade and a reason for being there.
- Genre rows, **Continue exploring**, **Because you viewed**, **On this day** and **Trending**.
- An on-device recommender with exponential interest decay (45-day half-life), an explicit **discovery reserve**, muted interests, pinning, and a plain-language explanation for every recommendation.
- **Taste Profile** screen: see what the library has noticed, adjust discovery, mute or pin interests, pause learning, and reset everything. Nothing leaves the device.

### Explore

- Search across title, artist, description, tags and moods using Room FTS5, with filters for genre, period, museum, medium, mood, subject, colour and sort order.
- Museum world map drawn locally — no tile server, no network.
- **Time Machine** by century; **Colour Search** over extracted palettes; **Hidden Gems**; **Artist Connections**; **Museum Visit** with permission-aware nearby museums and offline floor guides.
- Similar Art and artist graphs are computed from local metadata and palette distance. No embedding service is contacted.

### Looking closely

- Deep-zoom viewer with pan/zoom and tile-aware images.
- **Brushstroke**, **Slow Looking**, **Hidden Details**, **X-ray / infrared / restoration** layers where a pack supplies them.
- Frame Preview and Scale Check.
- Narration with Media3, plus an on-device `TextToSpeech` fallback.
- Share cards rendered locally, and a private Journal with search, tags, moods and a timeline.
- PDF virtual-exhibition export, collection export/import, encrypted backup/restore.

### Learn and play

Story Tours, Art School lessons, a glossary, artist biographies, spaced-repetition flashcards, Draw This, Detail Hunt, a daily quiz, Guess the Artist, Spot the Fake, Compare mode, mood discovery, offline Scan a Painting and Kids Mode.

### Your Eyes First

`EyesFirstEngine` produces a careful, non-authoritative reading of a work from a rule-based description engine. If a local model pack is installed and enabled, it is used; otherwise the rule-based path is deterministic and fully offline. Voice dictation writes into the Journal through the platform speech recogniser.

### System surfaces

- Daily artwork notification via WorkManager, with quiet hours and no streak shaming.
- Glance home-screen widget, Quick Settings tile, app shortcuts, parallax live wallpaper, Daydream/screensaver.
- Cast receiver options for Google TV / Chromecast.
- Adaptive bottom bar / navigation rail, predictive back, spring motion and haptics, reduced-motion and high-contrast modes.
- A Wear OS companion that shows a work of the day.

### Offline packs

Offline packs are a first-class feature, designed for 500 MB–5 GB archives:

- resumable HTTP range downloads;
- mandatory SHA-256 verification, optional ECDSA P-256 signature verification;
- zip-slip and path traversal rejection;
- free-space checks before download and extraction;
- Room indexing after extraction;
- delta-update metadata support;
- deletion that removes pack files and indexed artworks together.

The catalogue in `PackCatalog` points at `https://cdn.libraryofarts.org/packs/`, a placeholder for **your own CDN**. Packs with the placeholder all-zero checksum are deliberately shown as *Not published* and cannot be installed. See [Publishing packs](#publishing-offline-packs).

---

## Architecture

```
app/                     Application, MainActivity, navigation, workers, widget, tile,
                         wallpaper, daydream, cast options, AR wall preview
build-logic/             Convention plugins (application, library, compose, hilt, feature)
core/
  common/                Result/error types, dates, crypto, connectivity, feature flags, dHash
  model/                 Normalized artwork, museum, learning, journal, collection, taste, pack
  designsystem/          Theme, type, shapes, glass surfaces, haptics, bundled fonts
  database/              Room + SQLCipher, FTS5, DAOs, encrypted key handling
  network/               Retrofit + Kotlinx Serialization, eleven museum sources, health tracking
  datastore/             Preferences and typed settings
  data/                  Repositories, seed library, recommendation engine, pack pipeline,
                         backup, PDF/share rendering, scan index, Eyes First engine
  media/                 Media3 playback service and TTS narrator
  ui/                    Shared artwork cards, image loading, screen primitives
feature/
  home/ explore/ artwork/ journal/ taste/ collections/ learn/ games/ settings/
wear/                    Wear OS companion
baselineprofile/         Macrobenchmark baseline-profile generator
```

- **MVVM + unidirectional data flow.** ViewModels expose immutable UI state; screens emit events; repositories own policy.
- **Multi-module with convention plugins.** Version catalog drives every dependency.
- **Hilt** for dependency injection, including Hilt workers.
- **Room + FTS5**, encrypted with SQLCipher. The random database passphrase is wrapped with an Android Keystore key.
- **Retrofit + Kotlinx Serialization**, Coil 3, Paging 3, WorkManager, DataStore, Media3.
- **Stable, serializable models** for caching, packs, backups and collection interchange.
- **No hidden global state.** The recommender, Eyes First engine, query builder and pack verifier are plain classes with unit tests.

### Build toolchain

| Tool | Version |
| --- | --- |
| Android Gradle Plugin | 9.4.1 |
| Kotlin (built-in) | 2.4.21 |
| Compose BOM | 2026.09.00 |
| KSP | 2.3.14 |
| Hilt | 2.60.1 |
| Gradle | 9.8.2 |
| compileSdk / targetSdk | 37 |
| minSdk | 26 |
| Java toolchain | 17+ |

AGP 9's built-in Kotlin support is used; **do not add `org.jetbrains.kotlin.android`**, and do not raise the Compose BOM without also moving to a compatible SDK.

---

## Build and install

### Prerequisites

- JDK 17 or newer (CI uses 21)
- Android SDK Platform 37 and Build-Tools 36+
- An emulator or device running Android 8.0 (API 26) or newer

Create an untracked `local.properties`:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

Then:

```bash
git clone https://github.com/fleurdeur/the-Library-Of-Arts.git
cd the-Library-Of-Arts
./gradlew assembleDebug
```

Install the phone app:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Install the Wear companion:

```bash
./gradlew :wear:assembleDebug
adb install -r wear/build/outputs/apk/debug/wear-debug.apk
```

Useful tasks:

```bash
./gradlew testDebugUnitTest       # unit tests
./gradlew lintDebug               # Android lint
./gradlew assembleDebug           # all debug APKs
./gradlew :app:bundleRelease      # release bundle (needs your signing config)
```

### Museum API keys

No key is committed. Sources that need one are disabled until credentials exist. Provide them in `core/network` through a Hilt provider for `SourceCredentials` (read from your own untracked properties or a secret manager), for example:

```kotlin
@Provides @Singleton
fun credentials(): SourceCredentials = SourceCredentials(
    rijksmuseumKey = BuildConfig.RIJKSMUSEUM_KEY,
    // ...
)
```

Never commit a key, keystore or `local.properties`. The seed library and all offline features work without any key.

### Signing a release

Add your own keystore outside the repository and configure `signingConfigs` in `app/build.gradle.kts`. Keystores and `keystore.properties` are ignored by Git.

---

## Publishing offline packs

A pack is a ZIP containing:

```
manifest.json
artworks.jsonl        one normalized Artwork JSON per line
images/<id>.jpg       image files referenced by localPath
scan-index.json       optional perceptual hashes for offline scan
```

1. Build the archive and compute SHA-256.
2. Optionally sign the manifest bytes with ECDSA P-256; publish the public key.
3. Upload the archive to your CDN and update `PackCatalog` with the real URL, checksum, size and version.
4. The app downloads with `Range`, resumes after interruption, verifies the checksum (and signature when configured), rejects unsafe paths, extracts, then indexes records into Room.
5. Publish a delta manifest to let the app download only changed files.

The pack pipeline lives in `core/data/pack/`; `PackTrust` controls whether signatures are required.

---

## Privacy

- No account, advertising, analytics or attribution SDK.
- Preferences, taste signals, journal, collections and backups stay on the device.
- Museum requests go only to the museum you are browsing; no user identifier is attached.
- Location is requested only on Museum Visit, used once, never stored.
- Camera frames used by Scan and the wall preview are processed locally and discarded.
- Crash reporting is **opt-in**, off by default, and excludes artwork, journal text and location.
- The Journal database is encrypted; the journal can additionally require device authentication.
- Android cloud backup is disabled for app data; use the encrypted export instead.

The in-app privacy dashboard lists these controls.

---

## Testing and quality

- JVM unit tests cover the recommendation engine, seed integrity and encryption round-trips.
- `lintDebug` runs as part of CI.
- `baselineprofile/` contains a Macrobenchmark startup-profile generator; run it on a physical device and copy the generated profile into `app/src/main/`.
- ktlint and detekt configurations are included and run in a non-blocking CI job while existing style is normalized.
- GitHub Actions builds, tests, lints and uploads reports on every push and pull request.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

### Performance notes

- Compose stable types, strong skipping and immutable UI state.
- Coil disk/memory caching, size-aware image requests, and no eager preloading when data saver is on.
- `androidx.profileinstaller` is included for baseline profile installation.
- R8 full mode and resource shrinking for release.
- Lazy lists and paged search everywhere; no full-collection loads.

---

## Known limitations (honest notes)

- The Met endpoint currently answers HTTP 410. The source is isolated behind health tracking and the rest of the library continues to work; the seed collection keeps Home populated.
- Wikimedia Commons enforces rate limits; the app uses thumbnail URLs and backs off rather than hammering the API.
- **AR on the wall** is a camera-and-frame scale preview with an ARCore capability check. Depth-aware plane placement is intentionally marked as a future pack/hardware feature; no fake tracking is shown.
- **Cast** ships with receiver options and a manifest entry; the in-app gallery casting UI is not yet exposed.
- The pack catalogue is a template. Install buttons stay disabled until you publish real archives, so the app never pretends to download something that does not exist.
- Machine translation/localisation scaffolding is present for eleven languages; museum prose remains in the language supplied by the source.

---

## Contributing

Issues and pull requests are welcome. Please keep the following invariants:

1. Every artwork record shows licence, credit and provenance.
2. No tracking, advertising or account requirement is added.
3. No image or journal content leaves the device without an explicit user action.
4. New museum sources implement `MuseumRemoteSource` and are registered in `SourceBindings`.
5. Network failures degrade to offline content; they never block the library.

## Licence

The application source is licensed under the **GNU General Public License v3.0** (GPLv3). See [LICENSE](LICENSE).

Artwork images and metadata are **not** covered by that licence. Each record carries the licence and credit line supplied by its holding institution. Public-domain works remain public domain; openly licensed works remain under their stated licences. Please respect both.

The bundled Inter typeface is licensed under the SIL Open Font License. Noto Color Emoji is licensed under the SIL Open Font License. See the font directories for details.

---

<p align="center"><em>Made for slow looking.</em></p>
