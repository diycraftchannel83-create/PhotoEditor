# WinsPhoto Studio — Architecture

## Principles
1. Offline-first; implemented tools require no backend.
2. Imported originals are not overwritten.
3. Preview before Apply; edit actions are undoable at canvas level.
4. Modular processing and composable tool screens.
5. Memory-aware preview canvas (current import proxy max dimension 2048).
6. Build/CI verification is deferred until the feature pass is complete.

## Current implementation structure
- app: Android lifecycle, Compose navigation/workspace, SAF import/export
- core-model: project/layer/adjustment models
- core-editor: bitmap layer stack and composite renderer
- core-storage: local project snapshot manifest/assets
- core-history: command history foundation plus bounded canvas bitmap undo
- core-adjustments: CPU Basic Adjustments, Levels and Curves reference processors
- ui/tools: Crop/Transform, Adjustments, Levels, Curves, Layers, Brush/Eraser, Selection, Text/Shapes, Filters, Retouch, Background Removal, AI Background Removal, Project Manager

## Current render pipeline
Imported bitmap/proxy -> editor layers -> selected-layer tool processing or composite-canvas tool -> composite preview -> JPEG/PNG export.

Layer operations include visibility, opacity, ordering, duplication, rename, image/empty layer insertion, and blend modes. The current editor uses a flattened canvas snapshot after canvas-wide crop/selection operations and keeps a hidden previous-composite layer for recovery. Many adjustment/retouch tools currently apply directly to the selected bitmap layer rather than creating fully parameterized adjustment layers.

## Project persistence
Project Manager saves and reopens flattened PNG canvas snapshots with JSON metadata. Full editable project serialization (all source assets, layers, masks, blend/adjustment parameters and history) remains unfinished.

## History
Canvas Undo is bounded to five composite snapshots to limit memory. The command-history model remains a foundation for future operation-level history.

## AI
Bundled ML Kit Selfie Segmentation runs on-device for person/background separation. It is not a general object segmenter. Manual color-key removal is available for solid/high-contrast backgrounds.

## Performance and limitations
- Import proxy max dimension is 2048 to reduce processing memory.
- CPU processors run on background dispatchers for previews.
- Bitmap processing can still be memory intensive; full performance audit and testing remain deferred.
- Preview caches and render-job cancellation need further refinement.
- Export currently writes PNG/JPEG to Android's CreateDocument destinations.
- No original files are overwritten.
