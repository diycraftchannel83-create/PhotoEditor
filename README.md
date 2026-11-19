# WinsPhoto Studio

**WinsPhoto Studio** is an offline-first native Android photo editor project (Photoshop Mini concept) built with Kotlin and Jetpack Compose.

## Implemented editor tools
- Photo import and responsive editor preview
- Crop presets: Original, Free, 1:1, 4:3, 3:2, 16:9, 9:16
- Free crop size and position, rotate, flip, resize, preview and apply
- Basic adjustments: brightness, contrast, exposure, saturation, hue, temperature, tint, RGB balance, highlights, shadows, whites, blacks, vibrance, clarity and sharpen
- RGB/composite Levels with input/output and gamma controls
- Selective HSL color ranges (Red, Orange, Yellow, Green, Cyan, Blue, Purple, Magenta)
- RGB and per-channel Curves with draggable control points
- Layer manager: add transparent/photo layers, duplicate, rename, visibility, opacity, reorder, blend modes
- Brush and eraser
- Rectangle/ellipse selection with crop or mask
- Text and basic shape overlays
- Filter presets: Vivid, Warm, Cool, Vintage, Cinematic, Soft, Mono and Fade
- Retouch: blur, pixelate and clone stamp
- Color-key background removal with sample, tolerance and feather
- On-device AI person/background removal using bundled ML Kit Selfie Segmentation
- Local project save/open with editable layer stack, visibility, opacity, blend mode, selected layer and PNG assets
- JPEG and PNG export; JPEG quality and white/black flattening background
- Bounded undo history for applied canvas edits
- Memory-conscious image import (preview/editor canvas currently capped to 2048 px maximum dimension)

## Technology
- Kotlin, Android Native, Jetpack Compose
- Android SDK 35, minimum Android 8.0 (API 26)
- ML Kit Selfie Segmentation (bundled on-device model)
- Offline-first processing; no server/API required for implemented tools

## Important limitations before production
- The project is still in active implementation; the build/CI verification is intentionally deferred until the requested feature pass is complete.
- Layer adjustment tools currently render/apply to the selected layer bitmap; crop and selection operations use the composite canvas and preserve a hidden previous-composite layer.
- Project files preserve bitmap layers and layer metadata; adjustment tool parameters are baked into layer bitmaps rather than saved as live adjustment layers.
- AI segmentation is optimized for people/selfies; color-key removal handles simple solid backgrounds.
- Full-resolution RAW/large-image pipeline, advanced mask painting, GPU acceleration, true clone sampling UI, typography controls, and comprehensive testing remain future work.

## Build
Build verification is intentionally postponed until the implementation pass is finished. GitHub Actions workflow remains configured for a debug APK build and artifact upload.

See [docs/ROADMAP.md](docs/ROADMAP.md) and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
