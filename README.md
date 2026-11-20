# WinsPhoto Studio

**WinsPhoto Studio** is an offline-first native Android photo editor with the long-term goal of becoming a capable **Photoshop Mini for Android**. It is built with Kotlin and Jetpack Compose.

## Product vision

Build a mobile image editor with a unified workflow: canvas editing, non-destructive operations, layers and masks, selections, retouching, color correction, compositing, typography, effects, AI tools, project recovery, and high-quality export.

The roadmap below is the source of truth for continuing development. Update checkboxes only after implementation is committed and verified. Do not mark build, tests, device testing, or release tasks complete without evidence.

## Current implemented tools (not all production-verified)

- RGB/luminance histogram displayed in the Adjustment panel with bounded regular-grid sampling for large images (implementation committed; verification pending)

- Photo import and responsive editor preview
- Crop presets: Original, Free, 1:1, 4:3, 3:2, 16:9, 9:16
- Free crop size and position, rotate, flip, resize, preview and apply (replayable Crop/Transform operation)
- Basic adjustments: brightness, contrast, exposure, saturation, hue, temperature, tint, RGB balance, highlights, shadows, whites, blacks, vibrance, clarity and sharpen (per-layer non-destructive operation stack)
- RGB/composite Levels with input/output and gamma controls (replayable non-destructive per-layer operation)
- Selective HSL color ranges (Red, Orange, Yellow, Green, Cyan, Blue, Purple, Magenta; replayable per-layer operation)
- RGB and per-channel Curves with draggable control points (replayable non-destructive per-layer operation)
- Layer manager: add transparent/photo layers, duplicate, rename, visibility, opacity, reorder, blend modes, lock/alpha-lock, contiguous groups/group opacity/group blend modes, clipping-to-below, mask creation/inversion/painting
- Brush and eraser with size, opacity, hardness and straight-line controls
- Rectangle, ellipse, polygon, color-range and contiguous magic-wand selections; crop/mask/copy-to-layer/apply-as-layer-mask, invert, expand/contract and feather controls; selection masks now respect source transparency for color-range and magic-wand workflows; morphology and feathering optimized with separable/sliding-window passes to reduce per-pixel work (regression tests authored, verification deferred)
- Saved selection masks: capture/save current selection with a name from the Selection tool; app-private PNG/JSON storage supports reload/rename/delete (management UI now supports load/reuse, rename and delete; storage writes hardened with rollback; build verification pending)
- Text and shape overlays with font families, text-file import, bold/italic/alignment/outline/shadow/rotation/tracking/line spacing, fill/outline shape mode and size controls
- Linear/radial gradient layers with angle, color stops and opacity
- Filter presets with adjustable strength; separate blur, motion blur, vignette and grain effects panel; offline .cube LUT import (1D/3D)
- Retouch: blur, pixelate, clone stamp, spot-heal, dodge/burn and smudge; interpolated strokes, chronological application, soft brush falloff, undo-stroke and clear controls; UI now uses the shared Core normalized stroke model with validation and JSON codec; renderer moved into Core; replayable Retouch operation now stores strokes, strength and clone/smudge offsets in JSON and renders through the shared Core processor; Retouch is now live-integrated through MainActivity as a replayable per-layer operation; verification deferred
- Color-key background removal with sample, tolerance and feather
- On-device AI person/background removal using bundled ML Kit Selfie Segmentation
- Local project save/open/rename/duplicate/delete with editable layer stack, visibility, opacity, blend mode, lock, masks, group metadata, selected layer, original sources and per-layer operation metadata; unsaved-change guard now protects Back, Replace/import and opening another project (Save routes to Project manager; after saving, repeat the action; verification deferred)
- JPEG and PNG export; JPEG quality and white/black flattening background
- Bounded undo/redo state history (8 states, memory-conscious) preserving layer stack, selected layer and canvas; History panel can restore prior states
- Non-destructive operation stack panel: enable/disable, reorder, remove and reset Basic Adjust, Levels, Curves, HSL, Crop/Transform, Effects, LUT and Retouch operations; Basic Adjust operations can now be edited in-place (14 parameter sliders, stable operation ID/order; build verification pending)
- Private autosave snapshots and startup session recovery prompt
- Canvas resize independent of image resize (now replayable with dimensions, anchor and background); perspective correction (now replayable as a non-destructive operation with normalized corner parameters) and arbitrary straighten rotation
- Background replacement with solid, gradient or photo background (replayable operation; photo source stored as bounded PNG Base64 project data)
- Memory-conscious image import (preview/editor canvas currently capped to 2048 px maximum dimension); source-dimension export can replay the retained imported source layer and its replayable operations at native resolution, then composite other layers; falls back to preview resampling when source replay is unavailable, guarded to 24 MP

## Roadmap — Photoshop Mini

> **Continuation rule:** Work in small feature batches. Before coding, inspect the current implementation and roadmap. Preserve existing behavior, avoid duplicate files/classes, and integrate new tools into the shared editor workflow. Commit directly to `main` only when requested/approved. After each batch, update this README and the detailed `docs/ROADMAP.md`. Keep build/test status factual.

### Phase 0 — Engineering foundation & verification
- [x] Android Kotlin/Compose project scaffold
- [x] Repository architecture documentation
- [x] CI workflow configured
- [x] SAF image import foundation
- [ ] Dependency and license inventory
- [ ] Verify current main branch and latest CI run; resolve build/lint/test failures
- [ ] Establish baseline unit/instrumentation test coverage and record verified status

### Phase 1 — Core canvas, crop & transform
- [x] Canvas preview, zoom and pan foundation
- [x] Crop aspect-ratio presets and free crop
- [x] Rotate and flip
- [x] Resize controls and preview/apply workflow
- [ ] Verify crop/transform correctness, edge cases, aspect ratios and undo behavior
- [ ] Improve straighten/perspective correction tools, including corner dragging ergonomics and verification (perspective operation replay integration implemented; verification deferred)
- [x] Canvas-size tool (independent dimensions, anchors/background, replayable operation; tests authored, verification deferred)

### Phase 2 — Color & tonal editing
- [x] Brightness, contrast, exposure, saturation and hue
- [x] Temperature, tint and RGB balance
- [x] Highlights, shadows, whites, blacks, vibrance, clarity and sharpen
- [x] Levels (composite/RGB)
- [x] Curves (composite/RGB)
- [x] Selective HSL
- [x] Histogram (RGB and luminance; bounded sampling avoids full-image pixel-array allocation; CI verification pending)
- [ ] Validate adjustment math, clipping, alpha preservation and performance
- [ ] Improve live preview responsiveness and controls
- [ ] Add presets with editable/resettable parameter values
- [ ] Color-management audit (working space, display and export consistency)

### Phase 3 — Non-destructive editing engine
- [ ] Store edit operations/parameters instead of baking every adjustment immediately
- [ ] Non-destructive crop/transform and tonal adjustment pipeline
- [ ] True adjustment layers with editable parameters
- [x] Layer-preserving bounded undo/redo snapshots and History panel
- [ ] Parameter-level operation history
- [x] History panel restore and redo support
- [ ] Named states
- [ ] Define rendering pipeline and cache invalidation strategy
- [ ] Ensure original imported asset remains untouched throughout editing
- [ ] Add tests for operation serialization, ordering and replay

### Phase 4 — Layers, masks & compositing
- [x] Photo and transparent bitmap layers
- [x] Add, duplicate, delete, rename, select and reorder layers
- [x] Visibility and opacity
- [x] Initial blend modes
- [x] Layer metadata and bitmap project persistence
- [x] Contiguous layer groups/folders with group visibility and opacity
- [x] Layer masks (create, invert, paint, remove)
- [x] Mask painting with brush/eraser controls
- [ ] True adjustment layers
- [ ] Expand blend modes and validate compositing math
- [x] Layer lock, alpha lock and clip-to-below masks
- [ ] Layer effects (shadow, glow, stroke, bevel where feasible)
- [x] Layer panel thumbnails and multi-select/group controls

### Phase 5 — Selection tools
- [x] Rectangle and ellipse selection foundation
- [x] Crop and alpha-mask selection workflows
- [x] Freehand lasso selection
- [x] Polygon lasso
- [x] Magic wand / contiguous color selection
- [x] Color-range selection
- [x] Feather, expand and contract controls
- [x] Invert, save and load selections (saved selection store/UI; verification deferred)
- [ ] Selection overlay and edge visualization
- [x] Copy/cut selection as a new layer (copy/cut/cut-to-layer actions implemented; paste workflow still needs polish)

### Phase 6 — Retouching & paint
- [x] Brush and eraser with size, opacity, hardness and straight-line controls
- [x] Blur and pixelate
- [x] Clone stamp foundation
- [ ] Validate clone source selection/offset workflow and brush quality
- [x] Spot healing brush
- [x] Dodge and burn modes
- [x] Smudge tool
- [ ] More brush presets, hardness, spacing and pressure support where hardware permits
- [ ] Stabilized strokes and high-resolution brush rendering
- [ ] Retouch tools support masks and non-destructive workflows

### Phase 7 — Text, shapes & design
- [x] Basic text and shape overlays
- [x] Linear and radial gradient layers
- [x] System font family picker
- [ ] Font import/management
- [x] Text editing controls: size, weight, italic, alignment
- [x] Tracking and line spacing
- [x] Text outline and shadow
- [ ] Text gradient fill
- [ ] Shape controls: fill, outline, stroke width, rounded corners
- [ ] Custom paths and additional shapes
- [ ] Sticker/overlay asset browser
- [ ] Transform handles for text and graphic layers
- [ ] Layer effects and reusable styles

### Phase 8 — Filters & effects
- [x] Initial filter presets
- [ ] Filter strength and parameter editing
- [ ] LUT import/support (e.g. cube format after compatibility review)
- [ ] Gaussian blur and other blur types
- [ ] Motion blur
- [ ] Glow, vignette, grain and chromatic effects
- [ ] Distortion/warp tools
- [ ] Effect layers and non-destructive effect stack
- [ ] Preview thumbnails and effect performance testing

### Phase 9 — AI & background tools
- [x] Color-key background removal
- [x] On-device person segmentation/background removal
- [ ] Edge refinement, hair/detail preservation
- [ ] Subject selection integrated into selection/layer workflow
- [x] Background replacement with image/color/gradient (replayable operation; verification deferred)
- [ ] Object removal model feasibility and offline model-size evaluation
- [ ] Image enhancement and upscaling model feasibility
- [ ] AI tool progress, cancel, error handling and device capability checks
- [ ] Clearly disclose on-device vs online processing if online models are ever added

### Phase 10 — Project files, autosave & recovery
- [x] Local project save/open foundation
- [x] Editable bitmap layer stack and metadata persistence
- [ ] Save all live adjustment, mask, group and operation parameters
- [x] Project schema versioning and atomic manifest writes
- [ ] Full project migration system
- [x] Autosave and recovery snapshots
- [x] Startup session recovery
- [x] Basic unsaved-changes protection for Back, Replace/import and opening saved projects; Save opens Project manager, Discard continues, Cancel blocks (implementation committed; build/UI verification pending)
- [x] Recent projects and project management (open/list/rename/duplicate/delete/save-as flows implemented; verification deferred)
- [x] Duplicate/save-as/rename/delete project flows (implementation present; verification deferred)
- [ ] Validate missing/corrupt assets and recover gracefully

### Phase 11 — High-resolution rendering, performance & export
- [x] Retained original-source cache and native source-layer replay export foundation (24 MP guard; fallback for destructive/non-replayable edits)
- [x] Native source-dimension JPEG/PNG export replay for retained imported source layer (fallback resampling remains)
- [ ] Memory-aware tile/region rendering for large images
- [ ] Background processing and progress reporting
- [ ] Cancellation and safe failure handling
- [ ] Export PNG/JPEG quality, transparency and color options
- [ ] Export dimensions, metadata/EXIF policy and file naming
- [ ] Benchmark on low-RAM devices and large source images
- [ ] Evaluate GPU acceleration only after correctness and profiling

### Phase 12 — UX, accessibility & localization
- [ ] Consistent tool navigation and categorized tool panels
- [ ] Contextual tool options and apply/cancel/reset patterns
- [ ] Before/after preview
- [ ] Touch-friendly canvas controls and handles
- [ ] Keyboard support where relevant (external keyboard)
- [ ] Accessibility labels, focus order, contrast and scalable text
- [ ] Indonesian localization
- [ ] English localization
- [ ] Audit all UI strings for localization readiness

### Phase 13 — Privacy, security & release readiness
- [ ] Permissions and SAF access audit (request only what is necessary)
- [ ] Privacy policy and data disclosure audit
- [ ] Backup/export/project storage policy review
- [ ] Dependency, license and vulnerability audit
- [ ] No unintended network activity for offline tools
- [ ] Automated unit tests for processors, compositing, masks and persistence
- [ ] Instrumentation/UI regression tests for critical workflows
- [ ] Manual device regression test matrix (Android 8/API 26 through current)
- [ ] Low-memory, rotation, background/foreground and crash tests
- [ ] CI green for tests, lint, debug and release builds
- [ ] Release signing configuration secured
- [ ] Generate and verify production AAB
- [ ] Play Console listing, screenshots, privacy and app-content declarations
- [ ] Closed testing and feedback-driven fixes
- [ ] Production rollout plan and staged release

## Recommended next work order

1. Verify latest CI and fix any compile/lint/test failures.
2. Test Crop & Transform thoroughly and fix defects.
3. Add test coverage for image processors and project persistence.
4. Implement Histogram.
5. Strengthen adjustment preview, reset and undo/redo.
6. Build the non-destructive operation/history engine.
7. Implement layer masks and groups.
8. Expand selection and retouch tools.
9. Improve typography, shapes, effects and filters.
10. Implement full-resolution render/export and memory optimization.
11. Complete AI tools, project recovery and UX/localization.
12. Run final production audit, closed testing and release.

## Technology

- Kotlin, Android Native, Jetpack Compose
- Android SDK 35, minimum Android 8.0 (API 26)
- ML Kit Selfie Segmentation (bundled on-device model)
- Offline-first processing for implemented tools; no server/API required by current features

## Recent continuation work

- Histogram processing now samples large images on a regular grid with a maximum of 1,000,000 examined pixels, avoiding a full-image `IntArray` allocation. A regression test was added for large-image sample bounds. Build/test verification remains deferred.

- Added regression tests for edit-stack append/insert/reorder, enable/disable, replace/remove, clear and render-after-clear behavior. Tests authored; not executed because build/test verification is deferred.

- Added project lifecycle regression tests covering snapshot save/load, rename, duplicate, list and delete, plus blank-name validation. Verification pending.
- Added operation-project round-trip tests for original-source preservation, parameter/operation IDs, disabled operations, crop serialization and replay after reload. Verification pending.

## Recent continuation work

- Project/recovery persistence hardening: canonical path validation prevents project and recovery manifest asset traversal; supported schema checks and safe bitmap decoding protect editable projects, snapshots, operation-project loads and recovery. Path-traversal regression tests authored, verification pending.

## Known limitations / items to verify

- Feature checkboxes describe implementation presence, not production validation.
- Current editor preview/import canvas is capped at approximately 2048 px maximum dimension; full-resolution rendering/export is not yet complete.
- Some adjustments are baked into layer bitmaps rather than stored as live adjustment layers.
- Crop and selection operations may use composite workflows; verify preservation of original/layers before claiming fully non-destructive behavior.
- Undo/history is currently bounded and does not yet represent a full operation-level editable history.
- AI segmentation is primarily person/selfie oriented; color-key removal works best with simple backgrounds.
- Advanced mask painting, groups, healing, dodge/burn, full typography controls, GPU/tile rendering, autosave/recovery, and comprehensive testing remain incomplete.
- Build and CI status must be checked against actual GitHub Actions runs; never assume green.

## Build

Build verification and release readiness must be tracked honestly. Check the latest GitHub Actions workflow and its artifacts for actual results.

## Project documentation

- [Detailed roadmap](docs/ROADMAP.md)
- [Architecture](docs/ARCHITECTURE.md)


### Export sizing continuation
- Import records the source image dimensions from bounds-only decoding.
- PNG/JPEG export has an optional source-dimension resampling switch, while preview dimensions remain the default.
- Resampling is capped at 24 MP to reduce memory risk; test coverage authored, not executed.
- This is output-size support, not full-resolution processing: native-resolution source/edit replay remains a future task.


### Import memory-safety continuation
- Added a bounds-first preview decoder shared by main-image and photo-layer import.
- Decode retains source dimensions separately, samples down to a maximum preview dimension of 2048, rejects invalid/extreme source bounds, and uses ARGB_8888 for alpha fidelity.
- Sample-size and invalid-bounds regression tests authored; build/test verification remains deferred.


### Original-source cache continuation
- Original imports are now copied into an app-private disk cache before preview decode; cache replacement uses temp/backup rollback to avoid losing the prior cached source on failure.
- Export dialog reports whether the original is retained and its cached file size. Import remains usable when cache copy fails.
- Added a bounded source-cache decode foundation with a 24 MP pixel budget and tests for sampling calculations (authored, not run).
- Full-resolution replay is not yet wired into export: normalized operation parameters still require per-operation dimension audit and source-resolution replay integration.
