# WinsPhoto Studio Roadmap

## Phase 0 — Repository and engineering foundation
- [x] Repository initialized and architecture documented
- [x] Android Kotlin/Compose scaffold
- [x] CI workflow configured (verification intentionally deferred until feature pass completes)
- [ ] Dependency/license inventory and final audit

## Phase 1 — Editor canvas and transform
- [x] SAF photo import
- [x] Preview canvas and pinch zoom/pan
- [x] Crop presets (Original, Free, 1:1, 4:3, 3:2, 16:9, 9:16)
- [x] Free crop size and position
- [x] Rotate 90° increments and flip H/V
- [x] Resize and transform preview/apply
- [x] JPEG/PNG export, JPEG quality and transparent-canvas background options

## Phase 2 — Tonal and color tools
- [x] Adjustment UI and CPU preview renderer
- [x] Brightness, contrast, exposure, saturation, hue
- [x] Temperature, tint and RGB balance
- [x] Highlights, shadows, whites, blacks, vibrance, clarity and sharpen
- [x] Composite/RGB Levels with input, gamma and output controls
- [x] Composite and RGB Curves with draggable control points
- [x] RGB and luminance histogram UI/processor (CI verification pending)

## Phase 3 — Layer engine
- [x] Bitmap layer stack
- [x] Import photo layer, add transparent layer
- [x] Duplicate, delete, rename, select and reorder
- [x] Visibility and opacity
- [x] Blend modes (Normal, Multiply, Screen, Overlay, Darken, Lighten, Add, Difference)
- [ ] Groups, layer masks and true adjustment layers
- [x] Layer PNG assets and metadata project save/open (tool adjustments are baked into layer bitmaps)

## Phase 4 — Selections and retouching
- [x] Rectangle and ellipse selection
- [x] Crop selection and alpha mask selection
- [x] Brush and eraser with size/color controls
- [x] Blur and pixelate retouch
- [x] Clone stamp with source offset
- [ ] Healing, dodge/burn, advanced mask refinement

## Phase 5 — AI and background tools
- [x] Bundled on-device ML Kit person segmentation/background removal
- [x] Threshold/edge softness controls
- [x] Color-key background removal with sampling/tolerance/feather
- [ ] General-object segmentation, subject selection and background replacement
- [ ] AI object removal/upscale model evaluation

## Phase 6 — Creative tools
- [x] Text overlay
- [x] Rectangle, ellipse and line overlays
- [x] Filter presets (Vivid, Warm, Cool, Vintage, Cinematic, Soft, Mono, Fade)
- [x] Linear/radial gradient layers
- [ ] Fonts, stroke/fill controls, LUT support, layer effects

## Phase 7 — Project and production hardening
- [x] Local project save/open with editable bitmap layers and layer metadata
- [x] Bounded undo history for applied edits
- [ ] Live adjustment-layer parameter persistence, masks, groups and operation-level history
- [ ] Autosave/recovery and crash recovery
- [ ] Large-image, memory and device compatibility testing
- [ ] Accessibility and localization
- [ ] Metadata/export quality audit
- [ ] **Final build, CI verification, compile fixes and release audit (deferred by request)**
## Release candidate work — November 2026

- [x] Investigate latest GitHub Actions compile failure
- [x] Fix HSL processor missing import
- [x] Fix ML Kit segmentation result API usage
- [x] Fix crop/transform slider syntax
- [x] Fix retouch stroke loop syntax
- [x] Fix text/shape Compose syntax
- [x] Add launcher icon vector asset
- [x] Disable automatic app backup pending privacy/data audit
- [x] Expand CI to run unit-test task, lint, debug APK and unsigned release APK builds
- [ ] Verify CI passes and resolve any remaining compile/lint issues
- [x] Add initial Robolectric tests for HSL identity, alpha preservation and saturation adjustment
- [ ] Expand automated tests to other image processors and project persistence
- [ ] Device-level functional regression test
- [ ] Release signing and Android App Bundle (AAB)
- [ ] Privacy policy and Play Store declarations
