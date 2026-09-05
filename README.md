# Naze Files

Professional Android file manager — file browser, archive manager, and
universal file viewer in one app. Dark mode by default, blue + purple
identity.

## Status: Phase 6 of 6 — Android Integration (complete)

All six phases are now in place. Phase 6 wires Naze Files into the rest of
the Android system and adds the app-level features the spec's Home,
Settings, and Recent/Categories sections describe:

- ✅ **Home screen** — storage usage bar (instant, via `StatFs`), category
  shortcuts (Images/Videos/Audio/Documents/Archives/APKs/Code/Other), and
  Quick Access (Recent, Favorites, Downloads, Documents, Pictures, Music,
  Movies) - this is now the real landing screen, not the file browser
- ✅ **Settings** — theme (Dark/Light/System), default view mode, default
  sort, folders-first, show hidden files, show file extensions, confirm
  before delete - all persisted via DataStore and actually wired into the
  file browser's behavior, not decorative toggles
- ✅ **Recent files** — a capped (50), persisted history recorded every time
  a file is genuinely opened in a viewer; flags entries that no longer exist
- ✅ **Categories** — tap a category on Home to see every matching file
  across the whole storage root (background, cancellable recursive scan)
- ✅ **Storage Analyzer** — used/free space plus a real per-category size
  breakdown, computed on a background thread
- ✅ **Android file associations (Open With, receiving)** — Naze Files now
  appears in the system chooser for PDF, images, audio, video, text, ZIP,
  JSON, and XML - a type-specific intent filter, not a wildcard, so it never
  shows up for files it can't actually open
- ✅ **Share receiving** — Gallery/Browser/any app → Share → Naze Files
  → a real "Save to Naze Files" screen with a folder picker, backed by
  genuine `ContentResolver` stream copying (handles both `ACTION_SEND` and
  `ACTION_SEND_MULTIPLE`)
- ✅ **Real thumbnails** — image and video files show actual decoded
  thumbnails (via Coil, including video-frame extraction) in both list and
  grid view, not generic icons

### Known, honestly-documented gaps

- "Add files to an existing ZIP" still has no UI entry point (Phase 5 note,
  unchanged - the repository method works, there's just no button for it)
- Office document preview (.doc/.docx/.xls/.xlsx/.ppt/.pptx) still routes to
  Open With - no free, legally bundleable renderer exists for those formats
- PDF text search remains unavailable - `PdfRenderer` has no text-extraction
  API
- App lock / biometric lock was scoped out of Settings - it wasn't in this
  phase's realistic budget, so it's simply not offered rather than a fake
  toggle
- This project has been built and reviewed carefully, but **has not been
  compiled or run on a device** in this environment (no Android SDK, no
  network access here) - see "Testing" below before treating it as
  production-ready

## Requirements

- Android Studio Koala (2024.1.1) or newer
- JDK 17
- Android SDK 35 (compileSdk/targetSdk), minSdk 26

## Building locally

> **Note:** this repository does not include `gradlew` / `gradle/wrapper/gradle-wrapper.jar`.
> That jar is a binary fetched from Gradle's servers, and it wasn't generated
> here to avoid committing an unverified binary. Opening the project in
> Android Studio regenerates the wrapper automatically. From the command
> line, generate it once with a local Gradle install:
>
> ```bash
> gradle wrapper --gradle-version 8.7
> ```
>
> After that, build as usual:

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

## Building via GitHub Actions

Every push triggers `.github/workflows/build-apk.yml`, which checks out the
repo, sets up JDK 17 and the Android SDK, provisions Gradle 8.7 directly via
`gradle/actions/setup-gradle` (so the missing wrapper jar isn't needed in
CI), builds a debug APK, and uploads it as a workflow artifact. No keystore,
password, or API key is stored in this repository; release signing (when
added) will use GitHub Secrets.

## Testing

This project was written and reviewed carefully - every file was checked
for brace/paren balance and known-easy-to-miss import mistakes (two real
missing-import bugs from earlier phases were caught and fixed during the
Phase 5 and Phase 6 reviews; see git history / prior phase notes) - but it
has **not been compiled or run** in this environment, since there is no
Android SDK or emulator available here and network access is disabled.
Before relying on this as production-ready, please run through the spec's
own testing checklist (section 46-47 of the original brief): build the
project, fix any compile errors Android Studio surfaces, then manually test
storage permissions, file operations, large files, archive extraction,
media playback, PDF rendering, Android intents (Open With / Share, both
directions), dark/light mode, and low-memory situations on a real device or
emulator. The GitHub Actions workflow will at least confirm it compiles on
every push.

## Architecture

```
ui/          Compose screens and components, organized by feature
ui/home/     Home screen (storage usage, categories, quick access)
ui/settings/ Settings screen, backed by SettingsRepository (DataStore)
ui/recent/   Recent files screen + ViewModel
ui/category/ Category browser (recursive scan by MIME category)
ui/storage/  Storage Analyzer (StatFs + background category breakdown)
ui/incoming/ Save-incoming screen for Share-receive
ui/viewer/   Image, text/code, PDF, unsupported-type viewers + File Info dialog
ui/archive/  Archive (ZIP) viewer + shared folder-picker dialog
media/       PlaybackService (MediaSessionService) + AudioPlayerController
data/model/  Plain data classes (FileItem, SortOrder, ViewMode)
data/storage/StorageAccessManager — permission state + real storage roots
data/repository/ FileRepository — real java.io.File reads on Dispatchers.IO
data/operations/ FileOperationsRepository — streamed copy/move/rename/create,
                  cancellable, conflict-aware
data/trash/  TrashRepository — per-volume .naze_trash + JSON index
data/favorites/ FavoritesRepository — DataStore-backed favorite paths
data/recent/ RecentFilesRepository — DataStore-backed, capped recent history
data/settings/ SettingsRepository — DataStore-backed app preferences
data/viewer/ ViewerRouter — decides which viewer opens for a given file;
             PdfDocumentLoader wraps android.graphics.pdf.PdfRenderer
data/archive/ ArchiveRepository — ZIP browse/extract/create/add/delete via
              java.util.zip
util/        Formatting, filename validation, icon-mapping, share/open-with
             intents, binary detection, syntax highlighting, safe text I/O,
             content:// URI resolution for incoming files
```

All six planned phases are complete. Future work beyond the original scope
would mean revisiting the "Known, honestly-documented gaps" above.
