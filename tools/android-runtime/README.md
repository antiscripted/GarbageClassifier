# Bundled Android ONNX runtime

ONNX Runtime Android 1.22.0 is reused from Maven Central:
https://repo.maven.apache.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/1.22.0/onnxruntime-android-1.22.0.aar

SHA-256: `04a4617a9c797cf49225595e45b5546081cb34c86ac817581141577d3b7dbfe2`.
The AAR is bundled in the new-model scanner and instrumented tests; the legacy debug/release builds omit it. MIT license and third-party notices are retained and included in the Licenses screen.

The application uses CPU execution with two threads, sequential graph scheduling, graph optimizations and worker spinning disabled. ORT performs CPU capability detection for NEON/dot-product and AVX2 kernels and selects its portable fallback where necessary. The application does not require a GPU or download a runtime.

`Int4CompatibilityTest` checks the level-1/level-4 operator on a small real-weight fixture and loads the full production asset without a full inference call. `NewModelRuntimeTest` performs a full gray-image inference and is intended for a physical-device run. `CandidateRuntimeTest` is the preserved app-private-file diagnostic; it is skipped without explicit candidate arguments. Software-emulator timings are not physical phone measurements.
