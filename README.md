# WinsPhoto Studio

Professional, offline-first Android photo editor — Photoshop Mini.

## Current implementation
- Project metadata and layer model foundation
- Command-based undo/redo history foundation
- Crop/transform/aspect-ratio models
- Basic adjustment values and CPU reference processor
- RGB/per-channel Levels data and CPU reference processor
- Local project asset-copy and JSON manifest storage foundation
- Android Native scaffold (Kotlin + Jetpack Compose)
- Home screen
- Photo import through Android document picker
- Image preview workspace
- Replace imported photo
- Dark professional UI
- GitHub Actions debug build workflow

## Vision
A full-featured non-destructive photo editor with crop/resize, transforms, levels, curves, layers, masks, blend modes, AI background removal, selection tools, retouching, text/shapes, filters and high-quality export.

## Technology
- Kotlin, Android Native, Jetpack Compose
- Android SDK 35, minimum Android 8.0 (API 26)
- Gradle build
- On-device AI planned (model/license evaluation required)
- Offline-first

See docs/ROADMAP.md for implementation plan.
