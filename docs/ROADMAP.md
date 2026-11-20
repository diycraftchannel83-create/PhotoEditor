# WinsPhoto Studio — Roadmap

## Current continuation status

**Feature implementation pass is ongoing. Build, CI, lint and device verification are intentionally deferred until the user asks to finish the feature pass.** Checkboxes in this document represent implementation presence only unless explicitly labeled verified.

## Phase 0 — Engineering foundation
- [x] Android Kotlin/Compose scaffold, SAF import/export, offline-first foundation
- [x] CI workflow configured
- [x] Source dependency/license inventory documented (resolved transitive licenses and vulnerability scan remain release audit items)
- [x] Region-decoded tile preview assembler with bounded output and cancellation (integrated as import fallback; device verification deferred)
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
- [ ] Full crop/transform correctness and undo regression tests (authored tests, verification pending) (test files authored; verification pending)
- [x] Source-level repair of Text & Shapes multiline and line-spacing declarations (build deferred)
- [x] Align non-destructive Transform replay with TransformProcessor API (build deferred)
- [x] Disk-backed original source cache foundation (copy original without decoding, transactional cache replacement, cache status surfaced; regression tests authored)
- [x] Add source-resolution replay foundation that scales Transform/Perspective/CanvasSize dimension parameters while retaining normalized geometry; 24 MP safety guards added; tests authored (verification deferred)
- [x] Integrate native source-layer replay with source-dimension JPEG/PNG export; reconstruct the retained imported source layer, replay its operations, composite editor layers at source size; preview-resampling fallback remains for destructive or non-replayable edits (tests authored; verification deferred)

## Phase 2 — Color and tonal editing
- [x] Basic adjustment processor and UI (brightness, contrast, exposure, saturation, hue, temperature, tint, RGB balance, highlights/shadows, whites/blacks, vibrance, clarity, sharpen)
- [x] Levels composite/RGB processor and UI
- [x] Curves composite/RGB processor and UI
- [x] Selective HSL
- [x] RGB/luminance histogram processor and UI
- [x] Unit tests authored for basic adjustment, levels, curves, HSL and histogram (CI verification pending)
- [x] Histogram large-image sampling bounded to 1,000,000 samples; regression test authored (verification pending)
- [x] In-place parameter editing for Basic Adjust, Levels, Curves, HSL and Effects operation-stack entries (dialogs update same operation ID without changing order; implementation committed, verification deferred)
- [x] Extend in-place editing to Transform/Crop, Perspective, Canvas Size, Background Replace, Retouch and LUT (source implementation; verification deferred)
- [ ] Color-management consistency
- [x] Layer-tree adjustment compositor reduces pixel-array memory by processing bounded 128-row strips; supports cancellation at layer/strip boundaries and tests authored (verification deferred)
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
- [x] Operation-stack UI now edits Basic Adjust values on existing entries in-place (ID/order preserved)
- [x] Extend operation-stack parameter editor to remaining replayable types: Crop, Transform, Levels, Curves, HSL, Effect, LUT, Retouch, Perspective, Canvas Size and Background Replace; responsive action scrolling and background palette presets added (source implementation; build/test verification deferred)
- [x] Filter screen commits selected preset and strength as replayable Basic Adjust operation (source implementation; verification deferred)
- [x] Effects screen commits effect kind, strength and angle as replayable Effect operation (source implementation; verification deferred)
- [x] Effect operation can be enabled/disabled from operation stack and replays from retained source
- [x] Imported .cube LUT text is retained in a replayable LUT operation, with parse validation and stack toggle
- [x] Selection-mask output now commits as a replayable, serialized per-layer Selection Mask and inverted Selection Cut operations with enable/disable and in-place invert editing, PNG payload/dimension validation and 24 MP safety limit; regression tests authored (verification deferred)
- [x] Copy Layer and Cut-to-Layer selection actions now create source-retaining layers with replayable keep/invert masks (tests authored; verification deferred)
- [x] Selection Crop ratio presets (Free, 1:1, 4:3, 16:9, 3:2), select-all/clear shortcuts, and horizontally scrollable shape/action controls (implementation committed; verification deferred)
- [ ] Remaining selection/tool operation integration
- [x] Retouch operation model added to NonDestructiveEditStack with normalized stroke JSON, strength/offset parameters, enable/disable/replay and source-preserving render path (test authored; verification pending)
- [x] Retouch UI returns stroke parameters and MainActivity commits it as a replayable per-layer operation (build/device verification pending)
- [x] Core retouch mode/stroke model with normalized points, validation and JSON codec
- [x] Retouch UI migrated to shared Core stroke model
- [x] Bitmap retouch renderer moved into Core tools and UI delegates to shared processor (operation replay/persistence pending)
- [x] Filter presets now preserve parameter values in operation stack (source implementation; verification deferred)
- [x] Blur, motion blur, vignette and grain parameters are serializable and replayable through Effect operation
- [x] Layer-preserving bounded undo/redo snapshots and History panel restore
- [x] Named operation states save/restore/remove API with regression tests authored (verification pending)
- [x] Bounded (8 snapshot) undo/redo in the operation-stack screen for parameter edits, enable/disable, reorder, remove and reset; resets when the incoming layer-operation state changes (source implementation; verification deferred)
- [ ] Parameter-level operation history across the main editor and full editable-state recovery (operation-stack local history and global Apply/Undo are present; unified timeline remains)
- [ ] True live adjustment layers integrated into editor layer UI, project persistence and render-cache invalidation

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
- [ ] Nested groups (layer-stack Undo/Redo is implemented; bounded to 8 editor snapshots, verification deferred)
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
- [x] Selection mask processors account for source alpha in color-range and magic-wand selections
- [x] Selection expand/contract morphology and feather blur optimized with separable/sliding-window processing; edge regression tests authored (verification deferred)
- [ ] Refine edge, paste selection as layers and regression verification

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
- [ ] AI job progress, cancel, capability and error UX audit (progress spinner, cancel action and error display exist for person segmentation; broader audit remains)

## Phase 10 — Project, autosave and recovery
- [x] Project save/open for editable bitmap layer stacks
- [x] Persist visibility, opacity, blend mode, selected layer, masks, locks and group metadata
- [x] Per-layer non-destructive Basic Adjustment, Levels, Curves, HSL, Crop/Transform, Effects and LUT operations live-integrated; operation metadata and original layer sources persist in projects/recovery
- [x] Private recovery snapshots and startup recovery/discard prompt
- [x] Schema version bumps and atomic manifest writes; optional-field backward-compatible loading
- [x] Editable-project loader validates canonical project-folder and asset paths, rejects traversal, limits supported schema versions, and safely handles missing/corrupt bitmap assets; snapshot and operation-project asset loading hardened (tests authored; verification pending)
- [x] Backward-compatible optional-field loading and schema-version rejection; corruption, unsupported-schema and traversal regression tests authored (verification deferred)
- [x] Duplicate, rename and delete APIs are implemented; lifecycle tests authored (verification pending)
- [x] Non-destructive operation-project save/load/replay regression tests authored (verification pending)
- [x] Save-as UI (save current with a new project name)
- [x] Recent/saved project list with open, rename, duplicate, delete (existing ProjectScreen; refresh-on-action)
- [x] Basic unsaved-changes guard for editor Back, Replace/import and opening another saved project; Save routes to Project manager, Discard continues and Cancel blocks (build/UI verification pending)
- [x] Basic Adjustment operation stack is live-integrated and per-layer operation metadata is saved in projects/recovery
- [x] Levels, Curves, HSL, Transform, Effects and LUT live operation integration and per-layer persistence
- [x] Operation stack toggle, reorder, remove and reset UI
- [x] Parameter editing inside existing replayable operations (Basic Adjust and remaining stack types; source implementation, verification deferred)
- [ ] Full editable-state recovery, parameter-level operation history

## Phase 11 — Render/export and performance
- [x] JPEG/PNG export, JPEG quality and background flattening controls
- [x] Preview proxy limit to reduce memory pressure
- [x] Bounds-first shared image decoder for main import and photo layers; rejects invalid/extreme dimensions and samples to 2048px preview; regression tests authored (verification deferred)
- [x] Disk-backed source cache and bounded original decode foundation (24 MP cap; tests authored, verification deferred)
- [x] Native source-layer replay path for source-dimension JPEG/PNG export; replayable operations scaled to source dimensions with 24 MP guards; destructive/non-replayable fallback remains (tests authored, verification pending)
- [ ] Tile/region processing, memory-aware caches, background progress and cancellation
- [x] Export metadata policy defined: source EXIF/location metadata is not copied into bitmap-generated exports and the export UI states this
- [ ] Color profile consistency and export dimension tests
- [ ] Benchmark low-RAM devices and large images

## Phase 12 — UX, accessibility and localization
- [x] Categorized tool navigation (Canvas/Color/Layers/Design/AI); responsive panels and touch target audit remain
- [ ] Tool reset and consistent apply/cancel/progress/error states
- [x] Before/after comparison (source implementation; verification pending)
- [ ] Accessibility labels, focus order, contrast and scalable text
- [ ] Indonesian and English localization; audit all UI strings
- [ ] Tablet/landscape layout and external keyboard support

## Phase 13 — Production/release readiness (deferred)
- [ ] Permissions, SAF and privacy policy audit (manifest currently has no explicit INTERNET permission; SAF behavior/privacy still requires verification)
- [x] Source dependency/license inventory documented in `docs/DEPENDENCIES.md`; resolved transitive licenses and vulnerability scan remain release tasks
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
- [x] **No build, CI, lint or test execution during this feature pass by request.**
- [x] Unsaved editor state guard added to Back, Replace/import and opening saved projects; Save routes to Project manager and the action must be repeated after save; Discard executes the pending action; Cancel aborts (implementation committed, verification deferred).
- [ ] Final source-level integration audit remains before verification.


## Recent implementation batch — export sizing
- [x] Capture original import dimensions during bounds-only decode.
- [x] Optional source-dimension export sizing for PNG/JPEG, with preview-size default.
- [x] Resampling renderer and 24 MP pixel-count guard.
- [x] Regression tests authored for dimension choice, identity path and oversize rejection.
- [x] Native-resolution source-layer replay integrated into JPEG/PNG source-dimension export when the imported source layer and replayable operations are retained; otherwise preview resampling fallback (tests authored, verification pending).
- [x] Build/test verification intentionally deferred until feature pass completion.

## Selection crop integration
- [x] Selection crop bounding box converted to normalized replayable Transform operation and committed through operation stack
- [x] Replay regression test authored for normalized crop transform (build/test verification deferred)

## History integration update
- [x] Operation Stack Apply now adds a main-editor history snapshot and descriptive action label for Add/Remove/Edit or multi-operation changes (source implementation; verification deferred)
- [ ] Merge parameter-level undo/redo inside Operation Stack into global editor history timeline (operation-stack edits are checkpointed on Apply; fine-grained timeline merge remains)

## Compositing safety
- [x] Layer compositor applies a 24 MP guard before allocating output bitmap; oversized allocation regression test authored (verification deferred)
- [ ] Continue bitmap ownership audit for all branches; renderer registry and processor cleanup implemented, failure-injection tests/verification remain


## Compositor lifecycle audit continuation
- [x] Add explicit bitmap ownership/recycling checklist and branch audit matrix (`docs/COMPOSITOR_AUDIT.md`).
- [x] Inspect compositor source branch-by-branch and apply source-level ownership cleanup; failure-injection tests and verification remain
- [ ] Add exception/failure-injection tests and verify later (caller-owned bitmap preservation tests are authored)


## Compositor lifecycle continuation
- [x] Protect Difference blend scaled temporary cleanup with `finally`.
- [x] Add regression test for caller-owned source/mask bitmap survival across repeated renders.
- [x] Add renderer-owned intermediate bitmap identity registry and exception cleanup; processor-level cleanup added (failure injection and verification pending)

- [x] LayerMaskProcessor now validates source, mask and combined-mask pixel counts against 24 MP before pixel-array allocations (verification deferred).

## Allocation/ownership audit continuation
- [x] Mask inversion and white-mask creation now enforce 24 MP safety ceiling before allocations.
- [x] Clip-to-below processor validates both input dimensions and cleans up scaled temporary in `finally`.
- [x] Clip-alpha input-ownership regression test added; tests/build remain deferred.
- [ ] Add renderer failure-injection tests and later verification.

## Layer tree audit
- [x] 24 MP output limit and maximum nesting depth (32) applied before recursion/allocation.
- [x] Child intermediate disposal and output exception cleanup use `finally`/catch paths.
- [x] Regression tests for oversize and excessive nesting authored; verification deferred.
- [x] Source-level mask/clip/group intermediates are tracked and cleanup guarded; failure-injection tests and verification remain

## Layer compositor exception cleanup
- [x] Renderer-owned intermediate bitmap identity registry tracks replay/mask/clip/opacity/group allocations.
- [x] Outer `finally` disposes tracked intermediates on success and failure; output is recycled on exception.
- [ ] Add processor failure-injection tests and later verification.
- [x] Build/test/CI intentionally remain deferred during feature pass.


### Processor failure-safety follow-up
- Hardened mask and clip-alpha processors against allocation/pixel-transfer failures, including owned bitmap cleanup and preserving caller-owned mask inputs.
- Next implementation focus: failure-path tests and renderer/group ownership cases. Build/test/CI remain deferred.

### Opacity intermediate hardening
- Added exception-safe cleanup for opacity-generated bitmaps and regression coverage that mask combine/invert preserve caller-owned inputs. No build or tests executed.

### Global command history hardening
- Command history now fails safely: failed undo/redo retains the command for retry, execute failures do not enter history, and invalid capacity is rejected. Regression tests added but not executed.

### Recovery transaction hardening
- Snapshot replacement now includes rollback for both snapshot and manifest failures. Added successive-save and 24 MP guard regression tests. Restoring a recovered session now sets dirty state to prevent accidental loss when leaving without saving. Verification deferred.

### Named operation states UI and persistence
- Named states are now available in the Operation Stack panel: save current stack, restore, and delete. States persist per layer in app-private JSON, with path checks, collision-resistant file names, operation validation and a 50-state cap. Tests authored; verification deferred.

### Categorized tool navigation
- Editor toolbar now groups tools into Canvas, Color, Layers, Design and AI categories with horizontal scrolling for compact screens. Touch-target, localization and full accessibility audits remain open.

### Project/recovery migration coverage
- [x] Added authored regression tests for schema-one legacy snapshot project loading and legacy recovery metadata defaults; tests remain unexecuted.
- [x] Added project/recovery corruption, unsupported-schema and path-traversal tests; verification deferred.


### Background removal safety
- [x] AI segmentation and color-key output enforce 24 MP limits and recycle generated output on pixel-transfer failure.
- [x] Both mask workflows now multiply generated alpha with existing source alpha, preserving transparent source pixels.
- [ ] Edge/hair quality and object segmentation remain feature/verification tasks.


### Export bitmap ownership
- [x] JPEG flatten and JPEG/PNG export intermediate bitmaps now clean up in `finally` paths; flatten and layer-fit allocations enforce 24 MP and recycle outputs on draw failures (source implementation; verification pending).


### Before/after comparison
- [x] Added before/after preview toggle that renders the retained source layer with replayable operations/mask removed, preserves other layers, renders on demand in background, and disables edit/history controls while comparison is active (source implementation; verification pending).

- [x] Before/After comparison includes on-demand background rendering, stale-result rejection, error feedback and editor-action gating (source implementation; verification pending).


### Export metadata policy
- [x] Edited JPEG/PNG exports are generated from bitmap content without copying source EXIF/location metadata; export UI states this policy. Dimension/metadata tests remain pending.


### Project duplication safety
- [x] Project duplication now validates source folder and name bounds and removes partial destination folders on failure; regression test authored (verification pending).


## Source compile repair — 20 November 2026

This section records source changes committed to `main` after inspecting the latest available CI failure. It does not claim that the fixes have passed compilation or tests.

- [x] Repair the MainActivity selection action dispatcher and exhaustively handle COPY_LAYER, MASK, CUT, CUT_TO_LAYER and CROP; keep crop committed through the existing Transform operation path.
- [x] Correct Cube LUT interpolation arithmetic from mixed Int/Float expressions and bound LUT text/image allocation.
- [x] Correct Gaussian-blur kernel arithmetic and enforce the 24 MP working-buffer ceiling for CPU effect processors.
- [x] Remove the inaccessible Canvas bitmap property from legacy Difference blending; pass the owned destination bitmap explicitly for root and group compositing.
- [x] Add the missing BitmapFactory import and bounds-first / 24 MP rejection when decoding persisted project assets.
- [x] Correct retouch spot-heal Long-to-Int color packing and add a renderer pixel budget.
- [x] Repair magic-wand neighbour branches and selection-feather sliding-window syntax; apply a consistent 24 MP guard across selection-mask allocation paths.
- [x] Fix BrushScreen preview point references to the current stroke.
- [x] Remove the stale GradientScreen source reference and consolidate preview cleanup into one lifecycle effect.
- [x] Correct OperationStackScreen Compose annotations for selection-mask and LUT edit dialogs.
- [ ] Review the full latest CI output and address any newly exposed compiler, unit-test or lint issues. Do not treat the pre-fix CI failure as resolved until a green run exists.
- [ ] Run Android unit tests, lint, debug APK, release APK and unsigned AAB verification after the feature pass is explicitly complete.
- [ ] Perform device-level interaction, large-image, memory, export and project round-trip testing.
- [ ] Complete remaining roadmap phases: full adjustment layer architecture, tiled renderer/cache/cancellation, advanced brush/font/shape tools, AI object segmentation/removal, global recovery/history merge, localization/accessibility/tablet support, dependency/SBOM/license/CVE review, privacy/offline audit and production release validation.

## Integrated source repair follow-up — 20 November 2026

Additional corrections after inspecting the CI job log and current source:

- [x] Correct the `EffectProcessor` package declaration to match `core.adjustments` and unblock the existing `EffectSettings` / `EffectsScreen` imports.
- [x] Normalize ARGB alpha-bit masks to signed `Int` masks in CubeLut, EffectProcessor, LayerMaskProcessor, RetouchProcessors and RetouchRenderer.
- [x] Consolidate EffectsScreen preview disposal and ensure it never recycles the caller-owned source bitmap.
- [ ] Confirm a successful CI run on the exact latest `main` commit. Previous runs were superseded/cancelled or used earlier commit snapshots.
- [ ] Resolve any further compiler errors, test failures and lint diagnostics surfaced by the current run.


## Latest continuation handoff — 20 November 2026

### Source changes completed in this pass
- [x] Repair SelectionScreen result dispatcher in MainActivity and restore exhaustive handling for Copy Layer, Mask, Cut, Cut-to-Layer and Crop.
- [x] Fix compile-time numeric expressions and ARGB color packing in CubeLut, EffectProcessor, LayerMaskProcessor and retouch processors.
- [x] Restore EffectProcessor package namespace used by EffectSettings and EffectsScreen.
- [x] Replace unsupported Canvas bitmap access for legacy Difference blend with explicit backing-bitmap parameters for root and group composites.
- [x] Add the missing BitmapFactory reference and bounds-first asset decode guard to ProjectStore.
- [x] Correct selection Magic Wand neighbors/feather, brush preview points, gradient/effect preview lifecycle and Operation Stack Compose annotations.
- [x] Add/retain 24 MP guards in LUT, CPU effects, retouch, selection and project asset loading paths.
- [x] Update README, Architecture, Compositor Audit, Dependency Inventory and this roadmap.

### Verification status — do not mark as complete yet
- [ ] Confirm a green Android CI run for the exact latest `main` SHA.
- [ ] Unit tests and Robolectric suite passed.
- [ ] Android lint passed with all actionable warnings reviewed.
- [ ] Debug APK and unsigned release APK/AAB created and attached as CI artifacts.
- [ ] Manual device test on supported Android versions, including import/export, crop, masks, selection, layer groups, blend modes, undo/redo, project save/reopen/recovery, large images and memory pressure.
- [ ] No regressions or leaks across repeated effect/selection/retouch preview cancellation and apply.

### Remaining feature roadmap, ordered by dependency
1. **Rendering foundation:** introduce a render scheduler, immutable edit snapshots, cancellation/progress, bounded cache and tile/region processing; profile large images and make 24 MP limits user-visible rather than silently failing.
2. **Layer/history model:** true adjustment layers with editable parameters and masks; nested groups and group effects; one operation-based history timeline that includes layer-tree mutations; durable recovery of the complete editable session.
3. **Selection and brush:** refine edge-aware hair/object masks (without claiming ML Kit selfie segmentation selects arbitrary objects), refine/expand/contract/feather/paste-selection workflows, brush hardness/spacing/pressure/stylus support and mask painting UX.
4. **Typography and design:** font import/management, richer text styling and gradients, vector/custom path shapes, bezier controls, stickers/assets, layer styles and transform handles.
5. **AI tools:** generic object segmentation/removal, edge refinement, inpainting, quality controls and clear offline/model availability status. Existing ML Kit person segmentation remains person-focused.
6. **Color/export:** color profiles and wide-gamut handling, explicit EXIF policy testing, alpha/transparency behavior, metadata audit and robust full-resolution/tiled replay.
7. **Quality/accessibility:** Indonesian and English localization first, then supported locales; semantic labels, contrast, keyboard/shortcut navigation, tablet/foldable/responsive layouts and accessibility checks.
8. **Security/release:** verify backup/export/share boundaries, storage permission model, crash/recovery behavior, dependency graph, SBOM, license notices, CVE scan, ML Kit terms, privacy statement, app icon/name/screenshots, Play target API/content rating/data safety and signed release configuration.

### CI observations
Latest observed automatic workflow run: #664 (`26866feeea5b`) is `in_progress` with conclusion `pending`. This run was created by the repository's push workflow; it is not a locally executed build. Earlier runs were cancelled because newer commits superseded them. Check the exact latest run above before continuing build-related repairs.

**Continuation rule:** keep the roadmap honest. Source implementation, source audit, CI compilation, automated tests, device verification and production readiness are separate acceptance gates. Do not tick a verification gate without evidence.



## Continued work — 20 November 2026 (render foundation + CI test repair)

### Implemented source in this continuation
- [x] Add `core/render/BitmapRenderScheduler.kt`: serialized preview work queue, per-request cancellation signal, bounded bitmap LRU cache, caller-owned bitmap copies, cache clearing, and close lifecycle.
- [x] Propagate cancellation signals when `cancelAll()` / `close()` cancel render work.
- [x] Add Robolectric tests for cache ownership and cache memory bounds.
- [x] Repair CI-discovered malformed literal newline escapes in `FullResolutionEditReplayTest.kt` and `SelectionMaskProcessorTest.kt`.
- [x] Replace unsupported AndroidJUnit4 imports/runner in local unit tests with the configured Robolectric runner in `FullResolutionEditReplayTest.kt` and `NativeSourceLayerExportTest.kt`.

### Newly observed CI issue and repair status
- [x] CI run #666 reached `compileDebugUnitTestKotlin` and failed because three tests referenced `androidx.test.ext.junit.runners.AndroidJUnit4` not included in current test dependencies; two tests also contained literal `\
` sequences instead of actual source newlines. The source corrections above are committed.
- [ ] Confirm a green run on the exact latest commit. The current source additions have not yet been proven by compilation or test execution.
- [ ] Review whether the render scheduler should be integrated with editor preview UI in a later dedicated pass; this class is a foundation, not yet a global render-engine replacement.

### Remaining work from rendering foundation
- [ ] Integrate scheduler into actual preview/edit surfaces with stable render keys, cancellation on tool change, and UI lifecycle ownership.
- [ ] Add render progress/telemetry and immutable render snapshots.
- [ ] Implement tile/region rendering and multi-stage preview/full-resolution pipeline; current 24 MP safety caps remain in place.
- [ ] Add deterministic test coverage for cancellation, close, cache eviction order, and render failures.



## Adjustment-layer renderer foundation — 20 November 2026

- [x] Add `LayerNode.AdjustmentNode` model for pixel-color adjustments (Basic, Levels, Curves, HSL, Effects, LUT).
- [x] Apply adjustment nodes in order to the accumulated layer-tree composite, so the node affects layers below it instead of rendering as a separate overlay.
- [x] Support adjustment opacity and optional grayscale/alpha mask blending.
- [x] Reject geometry operations on adjustment layers and reject output dimension changes during adjustment rendering.
- [x] Add Robolectric tests for adjustment output, source preservation and black-mask behavior.
- [ ] Integrate AdjustmentNode into layer panel, persistence/project serialization, add/edit dialogs and global history; current model/renderer/test support is foundation-only and is not yet a user-visible feature.
- [ ] Test operation order, opacity blending, mask scaling, invalid masks, transparent source behavior, nested groups and memory cleanup on device/CI.



## CI queue policy update
- [x] Update `.github/workflows/android-ci.yml` to set `cancel-in-progress: false`, so pushes queue their verification rather than cancelling the run already in progress. This is intended to make the final verification signal reliable during active development.
- [ ] Observe queued runs and confirm the latest code reaches unit tests, lint and APK/AAB steps.


## Continuation — render scheduler regression coverage
- [x] Add cancellation-signal propagation regression test for the serialized preview scheduler.
- [x] Add cache-clear/invalidation regression test verifying a subsequent request rerenders rather than returning stale cached pixels.
- [ ] Run CI verification for these tests (deferred; no build run initiated manually).
- [ ] Integrate scheduler with editor preview screens and versioned cache keys (next implementation target).


## Continuation — adjustment preview scheduler integration
- [x] Adjustment preview now uses the serialized bitmap render scheduler instead of directly running an unmanaged preview render.
- [x] Preview requests use a parameter/source-derived cache key and are queued on a dedicated scheduler.
- [x] Preview requests cancel on coroutine cancellation and the screen closes the scheduler on disposal.
- [ ] Verify UI lifecycle, cancellation behavior and compile/CI (deferred to CI/manual verification).
- [ ] Apply the same scheduler integration to remaining preview-producing tools; audit source bitmap immutability and cache-key completeness.


## CI failure follow-up
- [x] Investigated the queued scheduler test CI failures: both Robolectric test sources were missing the JUnit `RunWith` import.
- [x] Added the missing imports in NativeSourceLayerExportTest and FullResolutionEditReplayTest.
- [ ] Re-run CI and inspect test results; build verification remains pending.


## Continuation — additional preview tools use the scheduler
- [x] Effects screen preview now runs through the serialized scheduler with parameterized keys and cancellation on preview lifecycle changes.
- [x] Filter screen preview now runs through the scheduler; removed duplicate preview disposal effect and guards recycling.
- [ ] Verify CI compilation and tests for scheduler-integrated screens (pending).
- [ ] Continue applying scheduler/cancellation to preview tools with expensive operations; add processor-level cancellation checkpoints where supported.


## Continuation — remaining color preview tools on render queue
- [x] Curves preview integrated with serialized scheduler, parameter key and cancellation.
- [x] Levels preview integrated with serialized scheduler, parameter key and cancellation.
- [x] HSL color-range preview integrated with serialized scheduler, parameter key and cancellation.
- [x] LUT import preview integrated with serialized scheduler and cache keyed by source/LUT text.
- [ ] CI compile/test verification for these four integrations remains pending.
- [ ] Add cancellation checkpoints inside processors' per-pixel loops; currently scheduler cancellation is checked before/after processor calls, so in-progress pixel loops may not stop immediately.


## Continuation — geometry and retouch previews scheduled
- [x] Crop/Transform preview uses render scheduler and parameter cache key.
- [x] Perspective preview uses render scheduler and cache key from normalized quad parameters.
- [x] Retouch preview uses scheduler; active and committed stroke snapshots are keyed to preview settings.
- [ ] CI verification pending.
- [ ] Canvas-size tool still applies directly without a visual preview; next follow-up should add an async preview and safe output-size guard.


## Canvas-size preview continuation — 20 November 2026
- [x] Canvas Size renders a scheduled, cache-keyed preview for dimension, anchor and background changes; cancellation and screen disposal close the preview scheduler.
- [x] Apply uses an owned bitmap copy of the preview, avoiding transfer/recycle ownership conflicts.
- [x] Guard canvas output at 24 MP and 8192 px per side; invalid sizes show a warning and disable Apply.
- [ ] Verify compile/preview behavior in CI and on device (deferred during feature pass).
- [ ] Processor-level cancellation checkpoints and tiled/full-resolution canvas resize remain future rendering tasks.


## Color processor cancellation continuation — 20 November 2026
- [x] Basic Adjustment, Levels, Curves and HSL pixel processors accept optional Android cancellation signals and check cancellation at row boundaries.
- [x] Preview screens pass scheduler signals into those processors so active renders can stop during pixel processing rather than only before/after.
- [x] Processors clean up allocated output bitmaps when cancellation/errors occur; Basic Adjustment delays output allocation until after its pixel loop.
- [x] Added Robolectric source tests for pre-cancelled processor calls and source-pixel preservation.
- [ ] CI/build/test confirmation pending; these tests and changes are not yet verified by execution.
- [ ] Extend cancellation to blur/effects, retouch, perspective/transform and segmentation pipelines; optimize row-check overhead and use region/tile rendering for large originals.


## Effect, retouch and perspective cancellation — 20 November 2026
- [x] Effect processors (Gaussian blur, motion blur, vignette and grain) support row-level cancellation signals.
- [x] Effects preview passes scheduler cancellation into processor loops.
- [x] Retouch rendering and its blur/pixelate helpers check cancellation during row/stroke work; dodge-burn and smudge processors also support cancellation.
- [x] Retouch preview now passes scheduler cancellation through the complete processing pipeline.
- [x] Perspective warp checks cancellation and rejects output above 24 MP before allocation; its preview passes cancellation into the processor.
- [x] Added cancellation regression tests for effects, retouch processors and perspective transform safety.
- [ ] CI/build/test verification remains pending; no successful build is claimed for these commits.
- [ ] Remaining render optimization: reduce temporary arrays and add tiled/region rendering, especially for full-resolution edits.


## Selection preview rendering continuation — 20 November 2026
- [x] Selection preview now uses the serialized render scheduler with parameter-complete cache keys, cancellation, timeout handling and bitmap disposal.
- [x] Magic Wand, color range, polygon, rectangle, ellipse, invert, expand/contract and feather mask operations accept cancellation signals and check during expensive traversal/row loops.
- [x] Selection overlay preview passes cancellation into mask construction and refinement; empty polygon/freehand selections skip preview rendering.
- [x] Added cancellation regression coverage for selection mask operations.
- [ ] CI/build/test execution remains pending; changes are not yet verified by an Android build.
- [ ] Continue cancellation and memory profiling for selection apply/export paths, not only preview.

- [x] Selection mask overlay compositing also checks cancellation by row and cleans scaled/output bitmap ownership on interruption.


## Tiled preview import continuation — 20 November 2026

- [x] RegionPreviewAssembler builds a bounded preview by decoding source rectangles in tiles and composing them to the sampled output.
- [x] Import path uses the region/tile assembler as a fallback if the bounds-first preview decoder returns no bitmap.
- [x] Tile/output sizing is factored into a safety-checked helper; regression tests cover sampling, custom dimensions and invalid input.
- [ ] Region-preview seam/orientation tests, cancellation UI propagation and device profiling remain pending.
- [ ] Tile-aware editing/effect/export replay remains separate work; the import fallback does not imply full tiled editing.


## Cancellable tile-processing primitive — 20 November 2026

- [x] Add bounded per-tile processor: creates a temporary source tile, passes region geometry and cancellation to processor, enforces identical output dimensions, and transfers ownership of output tiles.
- [x] Exception/cancellation cleanup recycles input tiles and any untransferred output; previously completed output tiles are cleaned if the batch fails.
- [x] Add safe stitching helper with caller-configurable pixel limit and cancellation checkpoints.
- [x] Add regression tests for geometry edge tiles, source preservation, stitching, oversize rejection and pre-cancelled work (execution deferred).
- [ ] Integrate this primitive into selected CPU effects/retouch and full-resolution export replay. Current utility is a foundation; no existing editor tool is tile-backed yet.


## Halo-aware tile processing — 20 November 2026

- [x] Add `BitmapHaloTileProcessor`: non-overlapping core tiles are processed with an expanded source region (halo) clamped to source bounds; only the core output is retained, so neighboring tiles can avoid filter seams when halo size covers the kernel radius.
- [x] Enforce processor output dimension preservation, cancellation checkpoints and cleanup of temporary/input/output ownership on cancellation or failure.
- [x] Add seam regression test comparing a radius-1 horizontal neighborhood processor across tile boundaries against whole-image output; test edge-clamped expanded geometry and pre-cancellation (execution deferred).
- [ ] Integrate halo tiles into selected neighborhood-dependent effects and full-resolution export replay; choose halo from each filter's kernel/radius and verify seams/performance on device.
- [ ] CI compilation/unit-test verification remains deferred.


## Tiled Basic Adjust processing foundation — 20 November 2026

- [x] Add `TiledBasicAdjustmentProcessor.applyTiles` to run Basic Adjust per halo-aware core tile, retaining tile outputs for downstream consumers rather than allocating a full flattened output immediately.
- [x] Use a 1-pixel halo when Sharpen is enabled (the current sharpen reference neighborhood is radius 1); other Basic Adjust operations use pointwise processing and no halo.
- [x] Add equivalence regression test comparing tiled output with whole-image Basic Adjust, including sharpen at tile boundaries (test authored; execution deferred).
- [ ] Connect tile-list outputs to editor preview/full-resolution export consumers; current `flatten` helper remains subject to the 24 MP output ceiling.
- [ ] CI/test verification and large-image profiling remain deferred.


## Tiled flat-layer compositor — 20 November 2026

- [x] Add `TiledLayerCompositeRenderer.renderTiles` for bounded-memory compositing of canvas-aligned flat bitmap layers, including visibility, opacity, blend mode and layer masks.
- [x] Add bounded stitch helper and tile ownership cleanup.
- [x] Reject groups, clipping-to-below and operation stacks explicitly to prevent silent visual mismatches while those cases are not tiled/replayed.
- [x] Add tests comparing tiled output against the existing full compositor for flat aligned layers and transparent out-of-bounds source regions (authored; not executed).
- [ ] Integrate into export and expand to replayable operations, groups, clipping, scaled layer geometry and difference blend parity.
- [ ] Build/test/device verification remains deferred.


## Tiled flat-layer compositor — 20 November 2026

- [x] Add `TiledLayerCompositeRenderer.renderTiles` for bounded-memory compositing of canvas-aligned flat bitmap layers, including visibility, opacity, blend mode and layer masks.
- [x] Add bounded stitch helper and tile ownership cleanup.
- [x] Reject groups, clipping-to-below and operation stacks explicitly to prevent silent visual mismatches while those cases are not tiled/replayed.
- [x] Add tests comparing tiled output against the existing full compositor for flat aligned layers and transparent out-of-bounds source regions (authored; not executed).
- [ ] Integrate into export and expand to replayable operations, groups, clipping, scaled layer geometry and difference blend parity.
- [ ] Build/test/device verification remains deferred.


## Streaming tiled PNG export foundation — 20 November 2026

- [x] Add `TiledPngEncoder` writing PNG signature/IHDR/IDAT/IEND and CRC32 directly to an `OutputStream`, avoiding full-canvas flattening.
- [x] Encode RGBA scanlines from sorted tile coverage; reject canvas gaps, overlaps, invalid tile sizes and out-of-bounds geometry.
- [x] Add adapters for basic processed tiles, halo tiles and tiled layer-composite output.
- [x] Add decode round-trip test and missing-coverage test (authored; not executed).
- [ ] Connect the encoder to the Android export UI/SAF output flow and provide tile-row streaming rather than retaining all rendered tiles in memory.
- [ ] Build/test/device verification remains deferred.


## PNG export integration — 20 November 2026

- [x] Integrate tiled flat-layer compositing into normal-size PNG export path in MainActivity when the current layer stack is within supported flat/unclipped/no-operation scope.
- [x] Stream tile output through `TiledPngEncoder` directly to the SAF output stream; recycle tile bitmaps afterward.
- [x] Keep existing PNG export path as fallback for unsupported layer stacks and keep source-dimension/native replay branch unchanged.
- [ ] Native full-resolution source export remains limited to the existing renderer; large-source region/tile replay is not yet integrated.
- [ ] Build, tests, UI/device verification remain deferred.
