# WinsPhoto Studio

Professional, offline-first Android photo editor — Photoshop Mini concept.

## Goals
- Non-destructive editing and project workflow
- GPU-accelerated canvas and image processing
- Layers, masks, blend modes, and adjustment layers
- Professional adjustments: Levels, Curves, RGB channels, histogram, color grading
- AI subject segmentation and background removal, prioritizing on-device models
- Crop, resize, transform, selection, retouch, text, shapes, filters, and export
- Core editing works offline

## Planned stack
- Kotlin, Android Native, Jetpack Compose
- Modular editor core
- GPU rendering pipeline (start with a supported GLES path; evaluate Vulkan for future)
- Room for local project metadata
- ONNX Runtime Mobile / TensorFlow Lite for on-device AI
- SAF for import/export

## Development status
Repository initialized for project planning and implementation. No application code has been implemented yet.

See [Architecture](docs/ARCHITECTURE.md) and [Roadmap](docs/ROADMAP.md).
