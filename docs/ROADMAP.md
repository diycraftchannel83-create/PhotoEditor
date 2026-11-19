# WinsPhoto Studio — Roadmap

## Current continuation status

**Feature implementation pass is ongoing. Build, CI, lint and device verification are intentionally deferred until the user asks to finish the feature pass.** Checkboxes in this document represent implementation presence only unless explicitly labeled verified.

## Phase 0 — Engineering foundation
- [x] Android Kotlin/Compose scaffold, SAF import/export, offline-first foundation
- [x] CI workflow configured
- [ ] Dependency/license inventory
- [ ] Build, lint and CI verification (deferred)
- [ ] Device regression and baseline performance audit (deferred)

## Phase 1 — Canvas and transform
- [x] Preview canvas, zoom and pan
- [x] Crop presets/free crop, rotate 90°, flips, resize preview/apply
- [x] Arbitrary straighten rotation control
- [x] Perspective correction model and UI
- [x] Perspective correction integrated as replayable operation with normalized corner quad, output dimensions, JSON round-trip, toggle and renderer; regression test authored (verification deferred)
- [x] Canvas-size processor and independent canvas-size UI with anchors/background
- [x] Canvas Size tool integrated as replayable operation (width/height, anchor and background serialized; test authored, verification deferred)
- [x] Background replacement integrated as replayable operation (solid/gradient/photo mode and colors serialized; photo background PNG encoded into bounded project payload; test authored, verification deferred)
- [ ] Full crop/transform correctness and undo regression tests (test files authored; verification pending)
- [x] Source-level repair of Text & Shapes multiline and line-spacing declarations (build deferred)
- [x] Align non-destructive Transform replay with TransformProcessor API (build deferred)
- [x] Disk-backed original source cache foundation (copy original without decoding, transactional cache replacement, cache status surfaced; regression tests authored)\n- [x] Add source-resolution replay foundation that scales Transform/Perspective/CanvasSize dimension parameters while retaining normalized geometry; tests authored (verification deferred)\n- [ ] Integrate full-resolution source/render pipeline into project/layer-aware export (preview currently capped at ~2048px; original retained, but export still resamples preview)

## Phase 2 — Color and tonal editing
- [x] Basic adjustment processor and UI (brightness, contrast, exposure, saturation, hue, temperature, tint, RGB balance, highlights/shadows, whites/blacks, vibrance, clarity, sharpen)
- [x] Levels composite/RGB processor and UI
- [x] Curves composite/RGB processor and UI
- [x] Selective HSL
- [x] RGB/luminance histogram processor and UI
- [x] Unit tests authored for basic adjustment, levels, curves, HSL and histogram (CI verification pending)
- [x] Histogram large-image sampling bounded to 1,000,000 samples; regression test authored (verification pending)
- [ ] Preset parameter editing/reset and color-management consistency
- [ ] Live parameter adjustment layers and preview cache

## Phase 3 — Non-destructive editing foundation
- [x] Replayable edit-operation model (Basic Adjust, Levels, Curves, HSL, Crop and Transform)
- [x] Enable/disable, replace and remove operations
- [x] JSON serialization/deserialization and replay tests authored
- [x] Operation-stack ordering, toggle, replace/remove and clear tests authored (verification pending)
- [x] Invalid crop JSON is rejected safely; operation replacement preserves stable IDs (tests authored, verification pending)
- [x] Persist original bitmap separately from operation JSON in a project-store foundation
- [x] Operation-project round-trip tests authored (original preservation, IDs/parameters, disabled ops, crop replay; verification pending)
- [x] Add source-preserving operation-session controller with bounded undo/redo, render preview/final and JSON export
- [x] Session controller regression tests authored for append, undo/redo, source isolation and redo-branch clearing (verification pending)
- [x] Add LayerOperationRenderer utility to reconstruct replayable layers from preserved source + operation metadata (tests authored; verification pending)
- [x] Layer compositor now replays operation metadata for individual and grouped layers (regression test authored; verification pending)
- [x] Operation-replay compositor test authored to catch stale rendered bitmap usage (verification pending)
- [x] Integrate Basic Adjustments, Levels, Curves and HSL parameter callbacks into MainActivity operation stack (verification pending)
- [x] Crop/Transform screen now returns normalized operation parameters and commits non-destructively as a flattened replayable layer (verification pending)
- [ ] Integrate remaining tools and full operation history into live editor
- [x] Filter screen commits selected preset and strength as replayable Basic Adjust operation (source implementation; verification deferred)
- [x] Effects screen commits effect kind, strength and angle as replayable Effect operation (source implementation; verification deferred)
- [x] Effect operation can be enabled/disabled from operation stack and replays from retained source
- [x] Imported .cube LUT text is retained in a replayable LUT operation, with parse validation and stack toggle
- [ ] Add selection and remaining tool operations to stack
- [x] Retouch operation model added to NonDestructiveEditStack with normalized stroke JSON, strength/offset parameters, enable/disable/replay and source-preserving render path (test authored; verification pending)
- [x] Retouch UI returns stroke parameters and MainActivity commits it as a replayable per-layer operation (build/device verification pending)
- [x] Core retouch mode/stroke model with normalized points, validation and JSON codec
- [x] Retouch UI migrated to shared Core stroke model
- [x] Bitmap retouch renderer moved into Core tools and UI delegates to shared processor (operation replay/persistence pending)
- [x] Filter presets now preserve parameter values in operation stack (source implementation; verification deferred)
- [x] Blur, motion blur, vignette and grain parameters are serializable and replayable through Effect operation
- [x] Layer-preserving bounded undo/redo snapshots and History panel restore
- [x] Named operation states save/restore/remove API with regression tests authored (verification pending)
- [ ] Parameter-level operation history
- [ ] True live adjustment layers and render-cache invalidation

## Phase 4 — Layers, masks and compositing
- [x] Bitmap/photo/transparent layers
- [x] Add, duplicate, delete, rename, select and reorder
- [x] Visibility, opacity and blend modes
- [x] Layer lock and alpha-lock state with editor guard/pixel-alpha processor
- [x] Grayscale layer masks, mask inversion, mask persistence and mask painting UI
- [x] Group metadata, contiguous grouping UI, group visibility/opacity, group-aware compositor
- [x] Project and recovery metadata persistence for masks, locks and groups
- [x] Clip-to-below alpha clipping and metadata persistence
- [x] Group blend-mode controls and metadata persistence
- [ ] Nested groups and full layer-stack undo/redo
- [ ] True adjustment layers, layer effects and group/mask integration with non-destructive history

## Phase 5 — Selection
- [x] Rectangle and ellipse selection
- [x] Polygon selection
- [x] Color-range selection
- [x] Contiguous magic-wand selection
- [x] Crop, alpha-mask and copy-selection-as-layer and apply-as-layer-mask output
- [x] Invert, expand/contract and feather controls
- [x] App-private saved-selection mask storage API (named masks, load/rename/delete, PNG assets and JSON metadata)
- [x] Selection UI can capture/save the current processed mask with a user-provided name
- [x] Saved-mask browse/load/rename/delete UI and apply loaded mask to selection workflow
- [x] Saved-selection UI integrated with selection preview, crop/mask/copy/cut/cut-to-layer output
- [x] Cut and cut-to-layer selection actions
- [x] Selection mask processors account for source alpha in color-range and magic-wand selections\n- [x] Selection expand/contract morphology and feather blur optimized with separable/sliding-window processing; edge regression tests authored (verification deferred)\n- [ ] Refine edge, paste selection as layers and regression verification

## Phase 6 — Retouch and paint
- [x] Brush and eraser with size, opacity, hardness and straight-line controls UI
- [x] Blur, pixelate and clone-stamp foundations
- [x] Spot-heal, dodge/burn and smudge processors (processors implemented; source verification pending)
- [x] Spot-heal, dodge/burn and smudge UI modes integrated
- [x] UI-independent normalized retouch stroke model and JSON encode/decode foundation
- [ ] Healing/retouch on masks, pressure/hardware support, stabilized strokes, brush presets/hardness/spacing
- [ ] Validate clone source workflow and high-resolution brush performance

## Phase 7 — Typography and design
- [x] Text and basic shape overlay UI
- [x] Text bold, italic, alignment, fill/outline and stroke-width controls
- [x] Rectangle, ellipse and line overlays
- [x] Linear/radial gradients
- [x] System font-family picker and text shadow controls
- [x] Shape fill/outline and width/height controls
- [x] Text and shape rotation
- [x] Text tracking and multiline line-spacing controls
- [ ] Font import and gradient text
- [ ] Custom paths, stickers/assets, transform handles and reusable layer styles

## Phase 8 — Filters and effects
- [x] Filter presets and strength slider
- [x] Gaussian blur, motion blur, vignette and grain processors
- [x] Effects UI integrated into editor
- [x] Common 1D/3D .cube LUT parser and preview/apply UI
- [ ] LUT/effects as non-destructive effect layers; additional effect types and processor performance verification

## Phase 9 — AI and background
- [x] Color-key background removal
- [x] Bundled on-device person segmentation/background removal
- [x] Background replacement UI: solid, gradient and photo
- [ ] Edge/hair refinement and subject-selection integration
- [ ] General-object segmentation, object removal and upscaling feasibility/size evaluation
- [ ] AI job progress, cancel, capability and error UX audit

## Phase 10 — Project, autosave and recovery
- [x] Project save/open for editable bitmap layer stacks
- [x] Persist visibility, opacity, blend mode, selected layer, masks, locks and group metadata
- [x] Per-layer non-destructive Basic Adjustment, Levels, Curves, HSL, Crop/Transform, Effects and LUT operations live-integrated; operation metadata and original layer sources persist in projects/recovery
- [x] Private recovery snapshots and startup recovery/discard prompt
- [x] Schema version bumps and atomic manifest writes; optional-field backward-compatible loading
- [x] Editable-project loader validates canonical project-folder and asset paths, rejects traversal, limits supported schema versions, and safely handles missing/corrupt bitmap assets; snapshot and operation-project asset loading hardened (tests authored; verification pending)
- [ ] Full migrations and project/recovery corruption coverage (asset-path hardening implemented; tests authored, verification pending)
- [x] Duplicate, rename and delete APIs are implemented; lifecycle tests authored (verification pending)
- [x] Non-destructive operation-project save/load/replay regression tests authored (verification pending)
- [x] Save-as UI (save current with a new project name)
- [x] Recent/saved project list with open, rename, duplicate, delete (existing ProjectScreen; refresh-on-action)
- [ ] Unsaved-changes protection
- [x] Basic Adjustment operation stack is live-integrated and per-layer operation metadata is saved in projects/recovery
- [x] Levels, Curves, HSL, Transform, Effects and LUT live operation integration and per-layer persistence
- [x] Operation stack toggle, reorder, remove and reset UI
- [ ] Parameter editing inside existing operations, full editable-state recovery

## Phase 11 — Render/export and performance
- [x] JPEG/PNG export, JPEG quality and background flattening controls
- [x] Preview proxy limit to reduce memory pressure\n- [x] Bounds-first shared image decoder for main import and photo layers; rejects invalid/extreme dimensions and samples to 2048px preview; regression tests authored (verification deferred)
- [x] Disk-backed source cache and bounded original decode foundation (24 MP cap; tests authored, verification deferred)\n- [ ] Full-resolution source/render/export path with operation replay
- [ ] Tile/region processing, memory-aware caches, background progress and cancellation
- [ ] Metadata/EXIF policy, color profile consistency and export dimension tests
- [ ] Benchmark low-RAM devices and large images

## Phase 12 — UX, accessibility and localization
- [ ] Categorized tool navigation, responsive panels and touch target audit
- [ ] Before/after, tool reset, consistent apply/cancel, progress/error states
- [ ] Accessibility labels, focus order, contrast and scalable text
- [ ] Indonesian and English localization; audit all UI strings
- [ ] Tablet/landscape layout and external keyboard support

## Phase 13 — Production/release readiness (deferred)
- [ ] Permissions, SAF and privacy policy audit
- [ ] Dependency/license/vulnerability inventory
- [ ] Offline/network behavior audit
- [ ] Unit/instrumentation/UI regression suite and verification
- [ ] Android API 26 through current device matrix
- [ ] Rotation/background/foreground/low-memory/crash tests
- [ ] CI green: unit tests, lint, debug/release builds
- [ ] Secure release signing and production AAB
- [ ] Play Store assets, privacy/data declarations, closed testing and staged rollout

## November 2026 feature-pass notes
- [x] Fix earlier source-level Compose/Kotlin issues reported by CI (verification not rerun in this pass)
- [x] Crop model tests added
- [x] Histogram tests present, including large-image bounded-sampling case (verification pending)
- [x] Non-destructive operation model and project persistence foundation
- [x] Layer-preserving undo/redo history and History panel
- [x] Mask/group/lock/clipping models, processors and persistence
- [x] Selection, retouch, effect, LUT, perspective, canvas-size, background replacement and recovery feature batches added
- [ ] **No build, CI, lint or test execution during this feature pass by request.**
- [ ] Source-level audit and final feature integration pass still required before build/CI verification.


## Recent implementation batch — export sizing
- [x] Capture original import dimensions during bounds-only decode.
- [x] Optional source-dimension export sizing for PNG/JPEG, with preview-size default.
- [x] Resampling renderer and 24 MP pixel-count guard.
- [x] Regression tests authored for dimension choice, identity path and oversize rejection.
- [ ] Native-resolution edit replay/rendering; current export resamples the 2048px edited preview, so this improves output dimensions but does not restore source-level detail.
- [ ] Build/test verification (deferred).
