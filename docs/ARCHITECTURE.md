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

Crop/selection/canvas-wide edits may flatten the composite and rebuild a simplified layer stack. Layer tools preserve bitmap-layer metadata where available. Basic Adjust, Levels, Curves, HSL, Crop/Transform, Effects, LUT, Retouch, Perspective, Canvas Size, Background Replace and Selection Mask edits can be retained as per-layer replayable operations and managed through the Operation Stack panel.

## Non-destructive foundation
NonDestructiveEditStack retains the original bitmap and ordered operation objects. Selection Mask and inverted Selection Cut operations store a bounded PNG-encoded mask payload in Base64 (maximum 24 MP, 24 MB encoded payload), validate encoded PNG dimensions, replay against the current operation result, serialize with project operation metadata, support stack enable/disable and expose an in-place invert toggle. Operation Stack editing has an independent bounded eight-snapshot Undo/Redo list for edits, toggles, reorder, removal and reset. Basic Adjust, Levels, Curves, HSL, Crop/Transform, Perspective, Canvas Size, Background Replacement, Effects, LUT and Retouch operations are replayable and JSON serializable; Perspective preserves normalized quadrilateral corners and output dimensions; Canvas Size retains dimensions, anchor and background color; Background Replacement retains solid/gradient/photo parameters and embeds photo backgrounds as bounded PNG Base64; Retouch preserves normalized stroke data plus strength and offsets. Operations can be enabled/disabled, replaced and removed. Retouch and Selection Mask regression coverage is authored but unexecuted. ProjectStore has a separate original-image plus operation JSON persistence path.

**Integration limitation:** the operation stack is now integrated for selected-layer adjustment and retouch workflows, and the UI supports enable/disable, reorder, remove and reset. `NonDestructiveEditorSession` remains a source-preserving controller with preview/final rendering, JSON export and bounded operation-level undo/redo. In-place parameter editors now cover Basic Adjust, Levels, Curves, HSL, Crop, Transform, Effects, LUT, Retouch, Perspective, Canvas Size and Background Replace while preserving IDs/order. Remaining limitations include true standalone adjustment layers, complete operation-level history/recovery, full-resolution export, nested groups and further tool integration. Background-replacement stack editor can change mode and select from a small preset palette; photo data remains unchanged. Stack action controls are horizontally scrollable to prevent narrow-screen clipping. Applying Mask or Cut from the Selection screen now appends a replayable operation to the selected layer's operation list while retaining the source bitmap; Copy Layer and Cut-to-Layer create source-retaining layers with keep/invert mask operations. Rendering is performed in the existing background-render workflow. If a Cut mask exceeds storage safety limits, the workflow falls back to the existing destructive cut result and reports the fallback. Operation Stack Undo/Redo covers the screen's changes but does not yet merge with the editor's main History panel. Build/device verification remains deferred.

## Layers, masks and groups
EditorLayer stores bitmap, visibility, opacity, blend mode, lock, alpha-lock, optional grayscale mask and group metadata, group opacity and group blend mode. Mask pixels use white to reveal and black to hide; mask alpha is respected. Mask painting and inversion controls are available in the layer panel. Alpha-lock retains the original layer alpha during selected-layer edits. Contiguous groups have shared group name/opacity metadata and render through an offscreen group composite. Nested groups, clipping masks and group effects are not implemented yet.

## Project persistence and recovery
Editable bitmap projects save layer PNGs plus JSON metadata, including masks, lock/alpha-lock and group metadata. Original+operation projects save the imported bitmap separately from operation JSON. Recovery snapshots are stored in app-private files and the editor offers a startup recovery prompt. The editor tracks dirty state from recorded edit actions and guards Back, Replace/import, and opening a different saved project with Save/Discard/Cancel. Save routes to the Project manager; after saving, users repeat the interrupted navigation action. This guard is implementation-level only and remains unverified.

Project and recovery loaders now resolve canonical paths under their app-owned folders, reject traversal paths, validate supported schemas, and safely skip unavailable/corrupt optional bitmaps or fail closed for invalid manifests. Snapshot and operation-project asset loading also use guarded path resolution. Security regression tests for project asset traversal were authored; build/runtime verification remains pending. Format migrations, autosave races, unsaved-change guard coverage for system navigation and automatic save-and-continue, and full operation state still require audit.

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

**Selection Crop integration:** the selection mask's non-zero bounding rectangle is normalized into an `EditOperation.Transform` and committed via `commitOperation` (flattened canvas output but replayable operation/source retained). A replay regression test was authored; verification is deferred.

Operation Stack Apply now records a main-editor history snapshot with descriptive labels derived from operation diffs (Add/Remove/Edit or stack summary). Screen-local undo/redo remains separate until the global history timeline is unified; verification deferred.

**Compositor allocation safety:** `LayerCompositeRenderer` enforces a 24 MP output limit before allocating the output bitmap. A regression test covers rejection above the limit; verification is deferred. Temporary bitmap ownership/lifecycle audit remains open.


## Compositor ownership audit
A branch-specific bitmap ownership/recycling checklist is tracked in `docs/COMPOSITOR_AUDIT.md`. It documents expected ownership rules and regression scenarios, but is not a completed source audit. Source fetch/search limitations prevented safe code modifications in this continuation. Verification remains deferred.


**Compositor ownership update:** Difference blending now releases the scaled source bitmap in a `finally` block. A regression test asserts caller-owned layer and mask inputs remain unrecycled across repeated renders. Exception cleanup for all renderer branches remains incomplete and verification is deferred.


**Mask allocation safety:** LayerMaskProcessor validates source, mask and combined-mask dimensions against a 24 MP pixel budget before allocating pixel arrays. Verification remains deferred.

**Mask and clipping safety update:** LayerMaskProcessor now also guards inversion and white-mask creation at 24 MP. ClipAlphaProcessor checks source and base dimensions before large arrays, and its scaled base bitmap is disposed through `finally`. A regression test covers clip output and caller input ownership. These are source-level changes only; no tests/build were run.

**Layer tree safeguards:** LayerTreeRenderer now enforces a 24 MP canvas budget and a nesting depth limit of 32. Each rendered child is disposed in `finally`; output is recycled if a branch fails. Regression tests are authored but unexecuted.

**Compositor exception cleanup:** LayerCompositeRenderer now maintains an identity-based registry of generated replay, mask, clipping, opacity and group bitmaps. The registry disposes intermediates in `finally`; the output bitmap is recycled when rendering throws. This is source-level only. Processor-internal exception paths and failure-injection verification remain open.


### Bitmap processor exception ownership
- Mask processing outputs are now protected by `try/catch` cleanup, including multiply-combined masks; internally scaled masks use `finally` cleanup and caller inputs are preserved.
- Clip-alpha output cleanup is guarded against setPixels failures.
- Tests are not run pending the user's instruction to defer verification.

### Opacity intermediate ownership
- `LayerCompositeRenderer.applyAlphaOpacity` now guards generated output against pixel-transfer failure and recycles it before rethrowing. Caller-owned layer bitmaps remain untouched.

### Command history transactional behavior
- `CommandHistory` only moves a command after the reverse/apply callback succeeds; failed undo/redo retains the entry on the current stack. Capacity must be positive. This is separate from the editor's bitmap snapshot history.

### Recovery transaction and unsaved-state behavior
- `RecoveryStore.save` stages new assets and manifest, swaps with backups, and rolls back snapshot/manifest on failed installation; temporary artifacts are cleaned. Recovery canvas size is bounded to 24 MP. Restored sessions are marked dirty and participate in unsaved-change protection.

### Named operation state persistence
- `NamedOperationStateStore` persists replayable operation arrays per layer ID in app-private storage. It validates schema and layer identity, bounds names/operations/state count, uses collision-resistant SHA-256-derived filenames and supports save/list/load/remove. Operation Stack panel exposes Save state, States, Restore and Delete.

### Persistence and editor-history safeguards
- Main editor snapshot undo/redo is bounded consistently to 8 states.
- Editable project creation enforces 1–100 layers, 24 MP canvas, and project-name length limits; partial project folders are deleted if save fails.
- Project rename/duplicate/delete lifecycle APIs validate project folders/IDs against app-private canonical root.
- Corrupt manifests, unsupported schemas, and asset traversal tests are authored but not executed.


### Tool navigation structure
- The main editor tool strip is grouped into Canvas, Color, Layers, Design and AI categories, retaining horizontal scrolling. This is a navigation foundation, not a complete responsive/accessibility audit.