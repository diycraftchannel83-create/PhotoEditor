# WinsPhoto Studio — Architecture

## Principles
1. Offline-first, no mandatory backend.
2. Non-destructive edits; preserve original assets.
3. Modular design and testable processing commands.
4. Responsive preview; full-resolution export.
5. Memory-aware processing for large images.
6. Feature completeness: no fake/inert tool buttons.

## Proposed modules
- app: navigation, Compose UI, app lifecycle
- core-model: project, layer, adjustment, mask and tool models
- core-storage: project serialization, assets, autosave and recovery
- editor-canvas: gestures, viewport, canvas composition
- render-engine: image pipeline, GPU preview, export renderer
- editing-tools: crop, transform, resize, selections, brushes, retouch
- adjustments: basic tonal/color controls, Levels, Curves, channel controls
- layers: stack, groups, opacity, blend modes, masks, effects
- ai-tools: local segmentation, background removal, subject selection, model management
- export: JPEG, PNG, WebP and metadata/quality controls
- feature-home: recent projects, import, templates and AI shortcuts

## Rendering pipeline
Source image -> layers/transforms -> masks -> adjustments -> effects -> composite -> preview/export.

Preview may use proxy resolution and cached GPU textures. Export uses the full-resolution pipeline. Rendering must not mutate the original source.

## Project persistence
Use a project manifest plus asset directory/package. Persist canvas dimensions, source assets, layer order, transforms, blend modes, visibility, masks, adjustments, text/shape data and versioned metadata. Autosave and recovery are planned.

## History
Use command-based undo/redo. Commands include crop, transform, layer operations, adjustment changes, mask edits, filter changes and brush strokes. Avoid full-bitmap snapshots for every action.

## AI
AI models should run on-device when feasible. Background removal should produce a mask with editable refinement controls. Model assets and licenses must be documented before inclusion.

## Performance and safety
- Decode images according to viewport and available memory.
- Use tile/proxy rendering for large images.
- Cancel obsolete processing jobs.
- Never silently overwrite originals.
- Export to a user-selected destination using Android storage APIs.
- Handle process death and low-memory recovery.
