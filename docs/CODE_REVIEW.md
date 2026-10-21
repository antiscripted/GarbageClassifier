# Code review — 21 October 2026

Reviewed the Kotlin camera controller/analyzer/detector, decoder, geometry, recognition filter/tracker, overlay and activity; both XML layouts, strings, manifest, Gradle configuration and license flow. Existing architecture and approved wording are retained.

## Fixed: expired results could reuse old tracking state

The UI discards results after 900 ms, while the tracker clears itself after a 1,500 ms frame gap. Previously, a frame arriving between those limits could revive a previously locked classification even though the UI had already expired it. `MainActivity.expiry` now calls `camera.resetTracking()` before clearing the result. This increments the generation, invalidates queued callbacks and resets confirmation/tracking on the next analyzed frame.

`expiredResultsInvalidateTrackingBeforeFramesResume` exercises the actual activity expiry runnable and checks both the generation increment and “Object not clear”. Existing tracker tests cover four-second holds, box smoothing, removal, replacement, ambiguous associations, contradiction and interruptions. Execution results are recorded separately in [VALIDATION.md](VALIDATION.md).

## Resource and lifecycle checks

- CameraX uses one analysis worker and keeps only the latest frame. Every ImageProxy is closed in `finally`; converted frame bitmaps are recycled. The interpreter and reusable model bitmap close on that worker after queued inference finishes.
- Camera and tracking generations reject callbacks from previous sessions, mode changes and bin changes. Backgrounding, resize and opening Licenses stop the camera and clear displayed results. Permission denial and recovery retain the existing approved wording.
- Model SHA-256, tensor shape/type and output score validity are checked. The decoder restores letterboxing and sensor rotation before CameraX maps into the preview crop. Unsupported classes have no bin recommendation.
- Confirmation and smoothing have one shared implementation for scanning and bin inspection. The four-second hold has deliberate safety exceptions for replacement, ambiguity, contradictory evidence and camera interruption.
- ONNX candidate tensors, result objects, sessions and options close in the test probe. That runtime and candidate weights are excluded from the scanner APK. The real original TFLite weights remain bundled.

No additional production refactor was justified. Appearance matching remains a small RGB-grid heuristic: similar objects can be confused, and lighting or rotation can break an association. Temporal stability cannot correct a consistently wrong model prediction. Two existing lint warnings (landscape nested weights and a bitmap KTX suggestion) do not affect correctness and were left unchanged.
