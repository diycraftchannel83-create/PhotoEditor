# WinsPhoto Studio — Architecture

## Principles
1. Offline-first; current tools do not require backend APIs.
2. Imported originals are not overwritten.
3. Preview/apply tool workflow; bounded bitmap undo/redo.
4. Modular Kotlin processors and Compose screens.
5. Bounds-first import decoding; preview proxy capped near 2048px maximum dimension, source dimensions retained as metadata, invalid/extreme bounds rejected.
6. Build, CI, lint and device verification are deferred until the feature pass is complete.

## Structure
- app: Android lifecycle, Compose editor workspace, SAF import/export and tool routing
- core/adjustments: Basic Adjustments, Levels, Curves, HSL, Histogram, Cube LUT and effects processors
- core/editor: EditorLayer metadata, layer masks, alpha-lock processor, grouped compositing
- core/history: command-history foundation and replayable serialized EditOperation stack
- core/tools: crop/transform model, canvas resize, perspective, selection masks, retouch processors
- core/storage: bitmap project manifests with per-layer original sources and operation metadata, private recovery snapshots
- ui/tools: crop/transform, canvas size, perspective, adjustments, levels, curves, HSL, filters, effects, LUT, layers/masks/groups, selection, retouch, text/shapes, gradients, background removal/replacement, project manager

## Current workflow
Imported preview bitmap -> bitmap layers -> selected-layer or composite tool -> composite preview -> JPEG/PNG export.

Crop/selection/canvas-wide edits may flatten the composite and rebuild a simplified layer stack. Layer tools preserve bitmap-layer metadata where available. Basic Adjust, Levels, Curves, HSL, Crop/Transform, Effects, LUT and Retouch edits can be retained as per-layer replayable operations and managed through the Operation Stack panel.

## Non-destructive foundation
NonDestructiveEditStack retains the original bitmap and ordered operation objects. Basic Adjust, Levels, Curves, HSL, Crop/Transform, Perspective, Canvas Size, Background Replacement, Effects, LUT and Retouch operations are replayable and JSON serializable; Perspective preserves normalized quadrilateral corners and output dimensions; Canvas Size retains dimensions, anchor and background color; Background Replacement retains solid/gradient/photo parameters and embeds photo backgrounds as bounded PNG Base64; Retouch preserves normalized stroke data plus strength and offsets. Operations can be enabled/disabled, replaced and removed. Retouch regression coverage is authored but unexecuted. ProjectStore has a separate original-image plus operation JSON persistence path.

**Integration limitation:** the operation stack is now integrated for selected-layer adjustment and retouch workflows, and the UI supports enable/disable, reorder, remove and reset. `NonDestructiveEditorSession` remains a source-preserving controller with preview/final rendering, JSON export and bounded operation-level undo/redo. Parameter editing inside existing operations, true standalone adjustment layers, full-resolution export, nested groups and remaining tool integration are still pending. Build/device verification remains deferred.

## Layers, masks and groups
EditorLayer stores bitmap, visibility, opacity, blend mode, lock, alpha-lock, optional grayscale mask and group metadata, group opacity and group blend mode. Mask pixels use white to reveal and black to hide; mask alpha is respected. Mask painting and inversion controls are available in the layer panel. Alpha-lock retains the original layer alpha during selected-layer edits. Contiguous groups have shared group name/opacity metadata and render through an offscreen group composite. Nested groups, clipping masks and group effects are not implemented yet.

## Project persistence and recovery
Editable bitmap projects save layer PNGs plus JSON metadata, including masks, lock/alpha-lock and group metadata. Original+operation projects save the imported bitmap separately from operation JSON. Recovery snapshots are stored in app-private files and the editor offers a startup recovery prompt.

Project and recovery loaders now resolve canonical paths under their app-owned folders, reject traversal paths, validate supported schemas, and safely skip unavailable/corrupt optional bitmaps or fail closed for invalid manifests. Snapshot and operation-project asset loading also use guarded path resolution. Security regression tests for project asset traversal were authored; build/runtime verification remains pending. Format migrations, autosave races and full operation state still require audit.

## Processing
- CPU reference processors are used for current adjustment, LUT, selection, retouch and effect tools.
- Preview processing uses background coroutine dispatch where tool screens implement it.
- GPU/tile rendering is deferred until correctness and profiling.
- ML Kit Selfie Segmentation is bundled/on-device; it primarily segments people, not arbitrary objects.

## History
Bitmap undo/redo remains bounded to five composite snapshots to limit memory. Undo/redo restores a simplified flattened layer state; complete layer-tree history and operation-level commands are future work.

## Export
JPEG/PNG export uses Android document destinations. JPEG can flatten transparency to a chosen white/black background. An optional source-dimension resampling mode now records import dimensions and resamples the edited preview to those dimensions, guarded at 24 megapixels. This is not native-resolution edit replay and cannot recover detail lost in the 2048px preview. A disk-backed original source cache is integrated with source-dimension export. NativeSourceLayerExport decodes the cached source, replays retained operations on the imported source layer using FullResolutionEditReplay (dimension-bound Transform/Perspective/CanvasSize parameters are scaled; normalized geometry remains unchanged), and composites other layers at source size. JPEG/PNG export uses native replay when that source layer remains replayable; destructive edits or unavailable source replay fall back to preview resampling. Replay/export is capped at 24 MP. Regression tests are authored but unexecuted. Color profile, EXIF policy, tile/region rendering and high-resolution operation replay remain incomplete; regression tests are authored but verification is deferred.

## Verification status
No build, CI, lint or tests were run during this feature pass, by request. New tests were authored but are pending execution. Source-level audit is ongoing.
