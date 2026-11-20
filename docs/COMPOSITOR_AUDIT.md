# Compositor bitmap ownership audit checklist

## Status
Source audit is in progress. This checklist defines explicit ownership expectations for `LayerCompositeRenderer`; it is **not** a verification report. Build, tests, lint and device checks remain deferred.

## Ownership rules to apply and verify
- Every bitmap created by a renderer branch is either returned/transferred to the caller, retained as a required input for later work, or recycled exactly once after its last read.
- Never recycle caller-owned layer bitmaps, original-source bitmaps, cached previews, or masks supplied by the caller.
- When an operation renderer returns the same instance as its input, do not recycle it as a temporary.
- Masked, clipped, group, blend and difference intermediate bitmaps require explicit ownership flags or equivalent identity-safe lifecycle handling.
- Replacing an intermediate must release the prior owned intermediate only after the replacement has finished reading it.
- Exception paths must release owned temporaries without recycling the final output that is being transferred.
- All allocation paths must respect the compositor's 24 MP output guard and avoid creating unbounded secondary canvases.
- Avoid alias-based double recycling: identity comparisons are required where processor outputs may alias inputs.

## Branch audit matrix
| Path | Ownership questions |
|---|---|
| Replayable layer operation | Is replay output owned? Can replay return input? Is source retained? |
| Mask | Is masked output owned? Is source preserved? Is mask caller-owned? |
| Clip-to-below | Is clipped result owned? Is prior-alpha input preserved? |
| Blend mode | Is blend result a new bitmap? Are both operands still needed? |
| Difference blend | Is scaled temporary released on all paths? |
| Group | Are group output, previous alpha and intermediate children disposed safely? |
| Failure/exception | Are temporary allocations released and final/caller inputs protected? |

## Regression tests to add before verification
- Input layer bitmaps remain unrecycled after a successful composite.
- Input masks remain usable after compositing.
- Intermediate bitmaps are recycled on success when no longer needed.
- Exception path cleans temporary allocations while preserving caller-owned inputs.
- Identity-returning replay processor does not trigger accidental recycling.
- Repeated composite calls do not mutate/recycle the inputs from earlier calls.

## Next implementation pass
Once the current renderer source is available for fresh inspection, apply ownership fixes branch-by-branch, add tests, then update this audit with exact code paths. Do not mark this checklist complete until source inspection and verification have evidence.


## Implemented ownership safeguards (source-level, not verified)
- Difference blend scaled temporary is recycled in a `finally` block, including pixel-read failure paths.
- Test added for repeated compositing preserving caller-owned source and mask bitmaps and their pixel data.
- Remaining: output/group/intermediate exception-path ownership across every branch; run tests only after feature pass.


## Mask allocation guard
LayerMaskProcessor now checks source/mask/combined dimensions using Long pixel-count calculations and rejects work above 24 MP before large pixel-array allocations. Source-level change only; tests/build remain pending.

## Processor-level follow-up safeguards
- LayerMaskProcessor `invert` and `createWhiteMask` reject allocation requests above 24 MP before bitmap/pixel-array allocation.
- ClipAlphaProcessor validates source and base pixel counts, and scaled base bitmap cleanup is in `finally` around pixel extraction.
- Regression test checks clip result and verifies source/base inputs remain intact.
- These changes are source-level only and not verified by test execution. Full compositor exception cleanup remains open.

## Layer tree renderer follow-up
- Added 24 MP render canvas guard and max nesting depth 32.
- Child intermediate bitmap is recycled in `finally`; output bitmap is recycled on exceptions.
- Regression tests cover oversize rejection and depth rejection; not run.
- Remaining exception ownership audit: LayerCompositeRenderer mask/clip/group branches.

## LayerCompositeRenderer ownership registry
- Uses identity-based tracking for renderer-created replay/mask/clip/opacity/group intermediates.
- Tracked intermediates are disposed in outer `finally`, even when an operation throws.
- Output is recycled on failure and transferred to caller on success.
- Still open: allocations created inside a processor before it returns, failure injection coverage, and verification.


## Processor exception-safety additions
- `LayerMaskProcessor` recycles output on failure for apply/invert/combine/white-mask creation and cleans scaled intermediates in `finally`.
- `ClipAlphaProcessor` recycles its output on failure and guarantees scaled base cleanup.
- Caller-owned input bitmaps are not recycled by these processors.
- Verification remains pending (no build, tests, or CI run).

### Opacity failure path
- The compositor's generated opacity bitmap now recycles on pixel-transfer failure. Added a regression test verifying combine/invert preserve caller-owned masks. Tests not executed.

### Command history resilience
- Undo/redo stack transitions now occur after successful command execution; failed actions leave history recoverable. Capacity validation and failure regression tests added; no verification run.

### Recovery safety audit updates
- Recovery snapshot and manifest replacement now have rollback paths; snapshot size guard and successive-save tests were added. Restored session state is considered unsaved so the user is prompted before navigation away. Tests/build remain unexecuted.

### Named operation-state implementation
- Persistent named operation stacks are now exposed through the Operation Stack panel, with save/restore/delete and per-layer app-private storage. Bounds and persistence tests were authored but not executed.

### Latest compositor and persistence pass
- Editor history limit aligned to 8 snapshots for Undo and Redo.
- Project save/lifecycle hardening: save bounds, cleanup of partial folder on failure, canonical folder validation, safe traversal-ID deletion.
- Failure-injection coverage still needed; tests are authored only, and verification remains deferred.
