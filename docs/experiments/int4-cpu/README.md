# SigLIP2 CPU delivery — 22 October 2026

**CPU-optimized APK, with physical phone performance still unverified.** This build preserves the new SigLIP2 center scanner and improves desktop CPU execution while maintaining the recorded accuracy target. It does not establish that Galaxy M51 or other phones meet the 1–2-second goal. Full production inference on ARM hardware remains the unresolved acceptance check.

## Downloads

- [GarbageClassifier-new-model.apk](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/checkpoint/apks/new-model-cpu/GarbageClassifier-new-model.apk)
- [Matching complete source ZIP](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/checkpoint/source/new-model-cpu/GarbageClassifier-new-model-source.zip)
- [APK SHA-256](../../../apks/new-model-cpu/SHA256SUMS.txt) · [source SHA-256](../../../source/new-model-cpu/SHA256SUMS.txt)

Only `checkpoint` changes. Earlier APKs/source archives, `main`, `development` and `backup` remain preserved. The archive includes actual model weights, native runtime AAR, Gradle wrapper, licenses, code, tests and evidence. Its `SOURCE_MANIFEST.json` verifies each included file and identifies the matching APK checksum. Local SDK paths, build caches, private signing keys and historical delivery archives are excluded.

## What was fixed

The previous model requested MatMulNBits `accuracy_level=1` in all 72 quantized matrix operations. ORT 1.22.0's source shows that the x86 packed path requires AVX2. The existing software emulator lacks AVX/AVX2 and KVM, so it takes `ComputeBUnpacked`, allocating/dequantizing the weight matrix before SGEMM on each invocation. That implementation explicitly contains a TODO to move dequantization into prepacking. This establishes the fallback mechanism; it does not quantify how much of the prior 23.6 minutes was dequantization versus emulated matrix arithmetic. No further full-model emulator inference was run.

ARM64 has separate NEON dispatch. Compute level 4 permits blockwise int8 activation arithmetic with the existing int4 weights when ORT detects a supported integer dot-product kernel. `GetComputeType` falls back to float computation when unavailable. This selection is inside the native runtime; the app has no chipset whitelist, GPU requirement or fragile parsing of phone CPU names. ARMv7 and x86_64 retain their supported CPU fallbacks. Performance on older CPUs remains unknown and may be inadequate.

The selected model changes only 60 compute-mode attributes, in encoder blocks 1–10. Blocks 0 and 11 retain level 1. Every stored initializer/weight byte, label, input/output shape and preprocessing rule is unchanged. This is neither retraining nor the rejected full-model int8 conversion. The first all-fast trial yielded 151 correct classifications and was rejected; preserving the first and last blocks yielded 153. No confidence threshold was lowered.

The app explicitly enables ORT graph optimizations and sequential scheduling, with two CPU threads and spinning disabled. Graph optimizations were already active in the prior runtime defaults; making them explicit is not claimed as an additional speed gain. A desktop profile identified MatMulNBits as the dominant operator. NNAPI/XNNPACK were not enabled: these dominant custom operators are CPU-registered, and simply enabling another provider does not establish acceleration. No runtime/model downloads are required by the installed app.

The center gate, asynchronous single worker, two independent confirmations, four-second hold, appearance checks, six-second evidence-age limit, removal/reset and existing interface are unchanged. The original small detector only gates object presence; it cannot supply a fallback material recommendation. Center mode draws no fabricated localization boxes.

## Measured results

Same cached center crops, same presence proposals, confidence 0.80 and margin 0.15; no temporal locking is simulated. All 464 images were evaluated once per new compute variant. Previously completed original results were reused.

| RealWaste, 240 supported images | Previous SigLIP2 | Selected mixed compute |
| --- | ---: | ---: |
| Correct accepted material | 152 | 153 |
| Wrong accepted material | 13 | 11 |
| Rejected | 75 | 76 |
| Wrong wet/dry recommendation | 2 | 2 |
| Accepted among 120 unsupported/ambiguous images | 22 | 22 |

The 104 TACO scenes retain four correct of eight supported centered scenes, four supported rejections and 18 accepted among 96 ambiguous/unsupported scenes. Background/clutter rejection remains limited. Glass/Plastic class breakdowns are in `mixed-summary.json`. Four of 464 top labels changed, so this arithmetic optimization is not bit-exact output parity; maximum top-probability change was 0.05143. Dataset training overlap is unknown. These still-image results are not a real-camera accuracy measurement.

| Performance check | Actual measurement |
| --- | --- |
| Selected model, 464 desktop center inferences | Median 197.3 ms; p95 243.9 ms; two threads, ONNX CPU EP. |
| Paired desktop gray-input check | Original median 301.2 ms; mixed median 254.0 ms, about 15.7% lower. Six alternating-order measured calls per model after one warm-up each; shared-host variability is visible in the raw times. |
| Android x86_64 small kernel fixture | Two 4×32 by 32×16 matrix operations completed in 38 ms. This is only an operator test, not a full-model latency estimate. |
| Android production model session | Bundled model checksum/tensor checks and session creation passed in 5,127 ms under software emulation. No full-model inference was requested. |
| Physical ARM64/ARMv7/x86_64 phones | Not tested. No phone inference latency, peak RAM, thermal or battery measurements. |

The earlier 23.6-minute full-model result belongs to the old compute configuration in the unaccelerated emulator. It is not a phone benchmark. The current APK is a technically justified CPU optimization and a testable delivery, not a verified universal performance fix.

## Verification

- Gradle `testNewModelUnitTest`, `assembleNewModel`, `assembleNewModelAndroidTest` and `lintNewModel` passed offline: **85 JVM tests, zero failures/errors/skips; lint zero errors and two existing warnings**.
- One Android instrumentation invocation, bounded at 45 seconds, passed in 7.619 seconds. It ran both compute levels on a small slice of real model weights, checked outputs against independently dequantized float matrix multiplication, and loaded the actual production model from the installed APK. Maximum absolute kernel error was 0.0000000298 for both levels on this fallback CPU. It does not validate the ARM integer kernel or full-model Android output.
- Host kernel reference errors were 0.0000000447 for float compute and 0.0002572 for integer compute. The production full-gray-image test remains available for physical devices; it was not rerun on the slow emulator. Its explicitly relaxed arithmetic tolerances accommodate optimized versus fallback compute, not bitwise equality.
- The existing Pillow resize golden tests and confirmation/stale-evidence/removal tests passed. Production input remains RGB, antialiased bilinear 224×224, normalized to [-1,1], CHW, upright center crop. No camera or tracking code changed this turn.
- APK ZIP integrity, v2 signature, original signing certificate, zip alignment, model checksum/size, runtime native-library bytes, required notices and all three packaged ABIs passed inspection. API 24 minimum / target 35 and 64-bit ELF alignment were checked. Installation and native execution were tested only on API 29 x86_64; other API/ABI combinations are packaged, not physically certified.
- CAMERA and the app's internal receiver permission are the only permissions; there is no INTERNET permission. Models and native libraries are bundled. Offline packaging and kernel execution are verified; full live offline scanning on a physical phone remains untested.
- The test-fixture generator initially assumed two-dimensional scale tensors. It was corrected to reshape the model's flat scale storage, then both host and Android reference checks passed. No unresolved build/test errors remain.

New production model SHA-256: `63802ff39c00c52663846e4bf7c0dfe511e4e4242398a24240bce733cdbdf366` (56,733,021 bytes).

Runtime AAR SHA-256: `04a4617a9c797cf49225595e45b5546081cb34c86ac817581141577d3b7dbfe2`.

## Install and build on Windows

Download the APK, retain the previous stable APK for rollback, and verify its SHA-256 with PowerShell:

```powershell
Get-FileHash .\GarbageClassifier-new-model.apk -Algorithm SHA256
Get-FileHash .\GarbageClassifier-new-model-source.zip -Algorithm SHA256
```

Open the APK on the phone and permit installation from the browser/file manager. It shares the previous app ID and signing key, so it updates the installed application. Grant Camera, enable airplane mode, and hold one well-lit object centrally against a plain background. It needs two independent confident results before confirmation. Replace/remove the object and check that old results do not return. If inference takes longer than the six-second freshness window, results should stay uncertain rather than acquire a stale lock. Preserve and reinstall the previous APK if the experiment is unusable; saved APK files are not modified.

For building, install Android Studio, JDK 17, SDK Platform 35 and Build Tools 35.0.0. Extract the matching source ZIP, open `GarbageClassifier`, set Gradle JDK 17 and allow Android Studio to configure the SDK location. Initial Gradle dependency resolution requires internet; the app itself does not. No model or native-runtime download is needed.

```powershell
.\gradlew.bat :app:testNewModelUnitTest :app:assembleNewModel :app:lintNewModel -PnewModel
adb install -r .\app\build\outputs\apk\newModel\app-newModel.apk
```

USB installation requires Developer options/USB debugging. A locally built debug key differs from the supplied APK's key; uninstall the supplied app first if Android reports a signature mismatch. This clears settings and permissions.

`MaterialClassifier.kt` prepares pixels and executes the verified model; `CenterModelRunner.kt` owns the single inference worker; `CenterScanner.kt` associates results with the current centered object; `RecognitionTracker.kt` enforces confirmation and holding. `CameraAnalyzer.kt` handles CameraX frames; `MainActivity.kt` manages permission, preview and bin controls. See the preserved architecture documentation for the unchanged UI components.

## Licenses and reproducibility

The app and original presence detector retain AGPL-3.0 obligations, including corresponding source when distributing the APK. The SigLIP2 model is Apache-2.0 (`prithivMLmods/Augmented-Waste-Classifier-SigLIP2`, revision `d203b52311f70ac3245d5fb8345d88344bec138e`); ONNX Runtime is MIT with third-party notices. The Licenses screen and source notices include the compute-mode modification. No new model was downloaded and no training occurred.

`configure_int4_compute.py` reproduces the selected file from the preserved original export; `evaluate_int4_execution.py` verifies unchanged weights and evaluates the cached data; `create_int4_kernel_fixture.py` generates the Android operator fixture. Data images stay outside the repository. Source URLs/checksums and the dispatch explanation are in `kernel-diagnosis.json`; raw evaluation, paired timing, JUnit, lint and Android results are beside this report.
