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
