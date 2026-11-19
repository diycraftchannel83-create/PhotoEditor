# WinsPhoto Studio — Architecture

## Principles
1. Offline-first; current tools do not require backend APIs.
2. Imported originals are not overwritten.
3. Preview/apply tool workflow; bounded bitmap undo/redo.
4. Modular Kotlin processors and Compose screens.
5. Memory-conscious preview proxy capped near 2048px maximum dimension.
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
NonDestructiveEditStack retains the original bitmap and ordered operation objects. Basic Adjust, Levels, Curves, HSL, Crop/Transform, Perspective, Effects, LUT and Retouch operations are replayable and JSON serializable; Perspective preserves normalized quadrilateral corners and output dimensions, while Retouch preserves normalized stroke data plus strength and offsets. Operations can be enabled/disabled, replaced and removed. Retouch regression coverage is authored but unexecuted. ProjectStore has a separate original-image plus operation JSON persistence path.

**Integration limitation:** the operation stack is now integrated for selected-layer adjustment and retouch workflows, and the UI supports enable/disable, reorder, remove and reset. `NonDestructiveEditorSession` remains a source-preserving controller with preview/final rendering, JSON export and bounded operation-level undo/redo. Parameter editing inside existing operations, true standalone adjustment layers, full-resolution export, nested groups and remaining tool integration are still pending. Build/device verification remains deferred.

## Layers, masks and groups
EditorLayer stores bitmap, visibility, opacity, blend mode, lock, alpha-lock, optional grayscale mask and group metadata, group opacity and group blend mode. Mask pixels use white to reveal and black to hide; mask alpha is respected. Mask painting and inversion controls are available in the layer panel. Alpha-lock retains the original layer alpha during selected-layer edits. Contiguous groups have shared group name/opacity metadata and render through an offscreen group composite. Nested groups, clipping masks and group effects are not implemented yet.

## Project persistence and recovery
Editable bitmap projects save layer PNGs plus JSON metadata, including masks, lock/alpha-lock and group metadata. Original+operation projects save the imported bitmap separately from operation JSON. Recovery snapshots are stored in app-private files and the editor offers a startup recovery prompt.

Recovery and project storage have not been build/runtime verified in this feature pass. The editable-project loader now validates canonical asset paths to prevent traversal and rejects missing/corrupt required assets rather than silently opening a partial project. Format migrations, autosave races and full operation state still require audit.

## Processing
- CPU reference processors are used for current adjustment, LUT, selection, retouch and effect tools.
- Preview processing uses background coroutine dispatch where tool screens implement it.
- GPU/tile rendering is deferred until correctness and profiling.
- ML Kit Selfie Segmentation is bundled/on-device; it primarily segments people, not arbitrary objects.

## History
Bitmap undo/redo remains bounded to five composite snapshots to limit memory. Undo/redo restores a simplified flattened layer state; complete layer-tree history and operation-level commands are future work.

## Export
JPEG/PNG export uses Android document destinations. JPEG can flatten transparency to a chosen white/black background. Current export is based on the editor preview canvas; full-resolution export, color profile, EXIF policy and large-image memory strategy remain incomplete.

## Verification status
No build, CI, lint or tests were run during this feature pass, by request. New tests were authored but are pending execution. Source-level audit is ongoing.
