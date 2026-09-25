<!--
Sync Impact Report:
- Version change: 1.0.0 -> 1.0.1
- Modified principles:
  - II. Structural Stability & Convention Fidelity (NON-NEGOTIABLE): Explicitly reinforced "no restructuring" rule per latest directive.
  - III. Core Functional Pillars (File Manager, Archive Manager, Universal File Viewer): Reinforced Phase 3 (Universal File Viewer) focus.
- Added sections: None
- Removed sections: None
- Follow-up TODOs: None
-->

# Naze Files Constitution

## Core Principles

### I. Android Native & Modern Architecture (Kotlin & Jetpack Compose)
Naze Files MUST be built strictly as a native Android application using idiomatic Kotlin and Jetpack Compose with Material 3.
- All new user interface components MUST use Jetpack Compose; XML-based view hierarchies are prohibited for UI screens and dialogs.
- State management MUST adhere to clean MVVM (Model-View-ViewModel) patterns powered by Kotlin Coroutines and Kotlin Flows (`StateFlow`, `SharedFlow`).
- The application MUST support edge-to-edge rendering, reactive UI theming (Dark mode by default with blue + purple identity tokens), and lifecycle-aware coroutine scoping.
- Architecture layers MUST maintain clear separation between UI presentation (`ui/`), domain/data persistence (`data/`), media playback services (`media/`), and standalone utilities (`util/`).

*Rationale*: Clean MVVM separation with native Kotlin and declarative Compose ensures long-term maintainability, deterministic state synchronization, optimal rendering performance, and deep integration with Android platform capabilities.

### II. Structural Stability & Convention Fidelity (NON-NEGOTIABLE)
Engineers MUST preserve the established code style, architectural boundaries, and modular structure of the repository.
- Unsolicited restructuring, re-architecting, multi-module splitting, or gratuitous package movements are STRICTLY PROHIBITED without explicit request.
- All new or modified code MUST conform to existing project naming conventions, file organizations, and pattern styles already established across `com.naze.files.*`.
- External third-party libraries MUST NOT be introduced without explicit approval; established libraries already in `app/build.gradle.kts` (e.g., Coil, Media3/ExoPlayer, DataStore Preferences, DocumentFile) MUST be prioritized for respective capabilities.

*Rationale*: Consistency and structural predictability prevent codebase fragmentation, maintain reviewer context, and safeguard against regressions introduced by unnecessary structural churn.

### III. Core Functional Pillars (File Manager, Archive Manager, Universal File Viewer)
Application capabilities MUST anchor to the three foundational pillars, with current prioritized focus on Phase 3 (Universal File Viewer):
- **File Manager**: Provides real file system traversal, metadata inspection, breadcrumb navigation, sorting, list/grid view modes, and complete file lifecycle operations (copy, move, rename, delete, trash lifecycle).
- **Archive Manager**: Delivers safe ZIP archive inspection, streamed extraction, and archive creation without loading entire archives into memory.
- **Universal File Viewer (Phase 3 Focus)**: Centralized routing via `ViewerRouter` MUST route files deterministically to specialized, high-performance viewers (Images via Coil, Text/Code with syntax highlighting and binary protection, PDF via `PdfDocumentLoader`, Audio via Media3 with background playback, Video via Media3, APK via system package installer). Files lacking dedicated in-app renderers MUST route gracefully to `UnsupportedViewerScreen` providing File Information and system "Open With" triggers rather than failing silently or corrupting binary output.

*Rationale*: Naze Files is defined by its ability to reliably browse, unpack, and view files across heterogeneous formats with robust error handling and zero silent failures.

### IV. Safe I/O, Streamed Operations & Memory Economy
All file and storage operations MUST prioritize data safety, non-blocking asynchronous execution, and constrained memory consumption:
- Operations MUST interact with authentic storage representations (`java.io.File`, Storage Access Framework `DocumentFile`, or `ContentResolver` streams); synthetic or fabricated disk data is strictly forbidden.
- File copying, moving, decompression, and text reading MUST use buffered streaming (e.g., fixed-size byte buffers); full-file in-memory buffering is prohibited.
- All disk and network I/O MUST execute off the main thread on `Dispatchers.IO`.
- Operations MUST implement cooperative cancellation (e.g., checking `ensureActive()` or tracking coroutine job cancellation) and provide real-time `OperationProgress` feedback.
- File naming conflicts MUST be handled interactively through explicit conflict resolution strategies (Replace, Keep Both / Rename, Skip) via `ConflictResolver`.

*Rationale*: Unbounded buffering and uncoordinated disk operations risk `OutOfMemoryError` crashes, UI thread freezing (ANRs), and accidental data loss during large file transfers.

### V. Resilient Permissions, Intents & System Integration
Naze Files MUST integrate smoothly with the Android ecosystem while handling varying permission models and external entry points:
- Storage access MUST handle runtime permissions gracefully across supported Android API levels (API 26 through API 35), honoring `MANAGE_EXTERNAL_STORAGE` when granted and providing clear, actionable user explanations when permissions are withheld.
- System integration points—including `ACTION_VIEW` (Open With), `ACTION_SEND` / `ACTION_SEND_MULTIPLE` (Share target with folder picker), and `MediaSessionService` (background audio with notifications)—MUST conform strictly to Android intent contracts and file path/URI resolution standards.
- Inbound `content://` URIs MUST be safely resolved and streamed using `ContentResolver` without assuming direct `java.io.File` accessibility.

*Rationale*: A file management and viewing tool is useless if blocked by permission barriers or unable to interface with standard Android share and open-with ecosystems.

## Architectural & Technology Standards

Naze Files enforces standard technologies and constraints across the codebase:
- **Language & Runtime**: Kotlin (targeting JVM 17), Android SDK compileSdk 35, targetSdk 35, minSdk 26.
- **UI Framework**: Jetpack Compose using Material 3 design tokens. Dark theme is the primary default, styled with the signature blue + purple identity palette. The UI MUST support dynamic theme modes (Dark, Light, System Default) driven by DataStore settings.
- **Persistence**: Lightweight preferences and user state (recent files, favorites, app settings, trash index) MUST be stored using `androidx.datastore:datastore-preferences` or structured local JSON metadata files where appropriate.
- **Media & Rendering**: Image decoding and video thumbnails MUST utilize Coil (`coil-compose`, `coil-video`, `coil-gif`). Audio and video playback MUST utilize AndroidX Media3 (`media3-exoplayer`, `media3-session`, `media3-ui`). PDF rendering MUST wrap Android's native `android.graphics.pdf.PdfRenderer`.
- **Defensive Error Handling**: Missing paths, permission denials, corrupted archives, or unreadable binaries MUST trigger explicit user-facing feedback (dialogs, snackbars, or dedicated error states) rather than silent empty views.

## Quality, Testing & Verification Gates

All contributions to Naze Files MUST satisfy verification gates prior to integration:
- **Compiler & Static Analysis**: Source code MUST compile cleanly under the project's Kotlin and Gradle configurations without unresolved symbols, missing imports, or deprecated API suppressions.
- **Automated Testing**: Unit tests MUST cover business logic, file format detection (`BinaryDetector`), URI resolution utilities, filename validation, and ViewModel state transitions under `app/src/test/`.
- **Manual Verification Protocol**: Changes touching core functional pillars MUST be validated against the testing matrix:
  1. Storage permission grant and denial transitions.
  2. Large file operations (streamed copy/move, cancellation responsiveness, conflict resolution).
  3. Archive operations (browsing nested ZIP folders, streaming extraction, ZIP creation).
  4. Universal viewer fidelity (image zooming, syntax-highlighted code reading, PDF page navigation, audio background playback service, video playback controls, binary fallback).
  5. Intent round-tripping (`ACTION_VIEW`, `ACTION_SEND` receiving, Open With launcher).
- **CI Build Gate**: All changes MUST pass the automated GitHub Actions pipeline (`build-apk.yml`), ensuring compilation and debug APK packaging remain green.

## Governance

This constitution serves as the foundational authority governing technical architecture, code quality, and workflow practices for Naze Files.
- **Supremacy**: The principles and standards specified herein supersede ad-hoc development preferences and informal practices.
- **Amendment Procedure**: Amendments to this constitution require:
  1. A formal proposal documenting the motivation, impact analysis, and affected principles.
  2. Review and consensus approval by project maintainers.
  3. A corresponding semantic version bump and updated `LAST_AMENDED_DATE`.
- **Versioning Policy**: The constitution follows Semantic Versioning (`MAJOR.MINOR.PATCH`):
  - `MAJOR`: Fundamental architectural shifts, deprecation or removal of core principles, or breaking workflow changes.
  - `MINOR`: Addition of new core principles, material expansion of existing guidance, or introduction of new functional phases.
  - `PATCH`: Clarifications, terminology refinements, grammatical corrections, and non-semantic adjustments.
- **Compliance Review**: All Pull Requests and code modifications MUST be audited against these constitutional principles. Any deviation MUST be documented and explicitly justified.

**Version**: 1.0.1 | **Ratified**: 2026-09-25 | **Last Amended**: 2026-09-25
