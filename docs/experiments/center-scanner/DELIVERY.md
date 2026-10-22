# SigLIP2 experimental delivery — 22 October 2026

**Status: unverified experimental APK for physical Android testing.** This is a genuine new-model build, not an old-model fallback. It is not qualified as a reliable real-time release. No Galaxy M51 or other physical phone was tested.

## Downloads

- [GarbageClassifier-new-model-unverified.apk](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/checkpoint/apks/new-model/GarbageClassifier-new-model-unverified.apk)
- [GarbageClassifier-new-model-source.zip](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/checkpoint/source/new-model/GarbageClassifier-new-model-source.zip)
- [APK checksum](../../../apks/new-model/SHA256SUMS.txt) and [source checksum](../../../source/new-model/SHA256SUMS.txt)

All changes and artifacts are on `checkpoint`. `main`, `development`, `backup`, the original APK and earlier experimental/stable APKs remain unchanged. The source archive includes the Gradle wrapper, Kotlin code, both model files, vendored Android ONNX runtime, licenses, tests and experiment results. It excludes historical APKs/archives, build caches, local SDK paths and private signing keys. `SOURCE_MANIFEST.json` binds every archived file to its SHA-256 and the delivered APK checksum.

## What changed

SigLIP2 supplies the material and wet/dry recommendation. The original lightweight detector only checks that one substantial object appears near the center; its class prediction is never used as fallback. The center crop is 80% of the visible camera crop's short side, rotated upright and resized to 224 × 224 with antialiased bilinear interpolation. RGB channels are normalized to [-1, 1] and packed as CHW. The model's verified ten-label order is preserved. Battery, Clothes, Shoes and Trash cannot produce wet/dry recommendations. Biological maps to Wet; Cardboard, Glass, Metal, Paper and Plastic map to Dry.

Classification runs on one background worker with two ONNX CPU threads and spinning disabled. Only one native call may be active. The camera continues sampling presence and appearance; completed classifications must match the current object/generation and be no more than six seconds old. Two independent confident outputs are needed to acquire the existing four-second lock. Reused camera frames cannot confirm a result. Removal, changed appearance, expired evidence and lifecycle resets invalidate old results. Center classification draws no precise object boxes. Bin inspection checks the centered object; it does not inspect every object in a bin simultaneously.

## Tests and exact blocker

| Check | Result |
| --- | --- |
| `testNewModelUnitTest` | 85 tests, 0 failures/errors/skips. Includes eight center-gate/independent-evidence/lock/removal tests and two Pillow resize-reference tests. |
| `assembleNewModel` | Passed; real new-model APK built. |
| `lintNewModel` | 0 errors, 2 existing warnings: NestedWeights and UseKtx. |
| `assembleNewModelAndroidTest -PnewModel` | Passed; production inference test included. |
| APK structure | ZIP CRC, signature v2, original signing certificate, zip alignment, 15 native libraries across ARM64/ARMv7/x86_64, 64-bit ELF alignment, bundled notices and model/runtime byte checks passed. |
| Offline packaging | CAMERA and internal receiver permission only; no INTERNET permission. Both models and runtime are bundled. No inference download or API is configured. Live offline camera classification remains unverified. |
| Earlier Android runtime probe | ONNX 1.22.0 loaded the same model and returned [1,10] logits. Maximum absolute error versus host zero-tensor reference: 0.00000644 (tolerance 0.0001). One call took 1,413,039 ms; load took 4,882 ms. PSS after inference was 141,086 KiB versus 55,290 KiB baseline; native allocated heap was 150,823,856 bytes. These are emulator figures, not phone RAM requirements. |
| One final bounded production attempt | Actual new APK and test APK installed successfully. Production `MaterialClassifier` verified and memory-mapped its bundled weights, checked tensor shapes and created its ONNX session in 6,105 ms. Its first classification did not return within a 60-second host deadline (exit 124). The app was force-stopped. |

The blocker is inference latency inside the production classification call in this API 29 x86_64 software emulator, after successful asset loading/session creation. The environment has no `/dev/kvm` and advertises no AVX/AVX2. Slow software emulation and fallback CPU kernels are plausible contributors; no native profile establishes the exact cause. Neither the correct standalone runtime output nor successful installation verifies the production pixel-to-result path, live camera behavior or usable ARM latency. No additional emulator inference loops were run. No claim is made that this model is fast enough for the M51.

The two added resize tests verify a Pillow golden downsample within one RGB byte and constant-color preservation. CHW normalization and rotation were reviewed in code; the production gray-image output test timed out, so its numerical assertion was not reached. Core asynchronous evidence safeguards passed JVM tests; actual worker/lifecycle interaction with completed Android SigLIP2 results, new-model camera permissions, orientation, sustained memory/thermal behavior and live object changes remain untested. The earlier nine scanner Android tests and permission/orientation checks belong to the preserved old-model release and are not counted as new-model passes.

## Accuracy evidence reused, not rerun

Cached evaluation used 360 RealWaste images and 104 TACO scenes. `center-summary.json` is reproducible with `tools/summarize_center.py`; no inference is needed to summarize it.

| RealWaste, same 240 supported images | Original pipeline | Center gate + SigLIP2 |
| --- | ---: | ---: |
| Correct material | 86 | 152 |
| Wrong material | 80 | 13 |
| Rejected | 74 | 75 |
| Wrong wet/dry recommendation | 5 | 2 |
| Accepted among 120 unsupported/ambiguous images | 39 | 22 |

The original acceptance threshold is 0.60; SigLIP2 is 0.80; both require a 0.15 class margin. Cardboard is grouped with Paper and Biological with Food waste for comparison. This is the complete still-image gate/classification policy, not live end-to-end accuracy; it omits temporal confirmation, camera blur and object transitions. Dataset training overlap is unknown. These results support phone testing, not a claim of universally better recognition.

The center-specific TACO policy yielded only eight supported centered scenes: four correct and four rejected. Eighteen of the other 96 unsupported/ambiguous scenes were accepted, including five of 49 scenes without a significant annotated center object. Clutter/background rejection therefore remains inadequate. Missing annotations do not establish that a scene contains no object. No precise-box accuracy is claimed for center scanning.

The DETR audit found that eight of 13 wrong-bin images had a correct whole-image top prediction: crop selection often isolated printed fruit on packaging, logos or tiny background details. No class-order or box-normalization swap was found. Using DETR's published aspect-preserving processor increased target coverage from 17/87 to 21/87 on the fixed 16-image diagnostic, insufficient to justify its extra CPU cost. The packaged build uses SigLIP2 center classification instead of DETR. It retains the existing int4 weights unchanged; the separate accuracy-level kernel experiment was not deployed. No training occurred.

## Install on Galaxy M51

1. Download the APK using the link above. Keep the original/stable APK available for rollback.
2. Open the APK and allow installation from that browser or file manager when Android requests it. This build has the same application ID and signing certificate as the previous supplied APK, so it updates the installed app rather than installing beside it. It does not change your saved old APK files.
3. Grant Camera. Turn on airplane mode. Hold one well-lit object centrally against a simple background, filling much of the preview. No precise boxes appear in this center variant.
4. Check several clearly different dry objects and organic items, then remove or replace each. A confirmation needs two independent results; if inference exceeds the six-second freshness budget, the app should remain uncertain rather than lock a stale label. Do not assume that remaining uncertain means the original model is running.
5. Check portrait/landscape rotation, background/resume, Wet/Dry selection and Bin inspection. Report time to first confirmed result, incorrect recommendations, failures to reset and camera freezes, with phone Android version. These are requested physical tests, not completed tests.
6. To roll back, reinstall the preserved stable APK. If Android rejects an update because of signing/version differences, uninstall this experimental app first; that clears its settings and permissions.

In Windows PowerShell, verify the downloaded files against their corresponding `SHA256SUMS.txt`:

```powershell
Get-FileHash .\GarbageClassifier-new-model-unverified.apk -Algorithm SHA256
Get-FileHash .\GarbageClassifier-new-model-source.zip -Algorithm SHA256
```

## Build on Windows

Install Android Studio, SDK Platform 35, Build Tools 35.0.0 and JDK 17. Extract the source ZIP and open its `GarbageClassifier` folder. Set the Gradle JDK to 17 and let Android Studio configure the SDK location. First-time Gradle dependencies require internet; APK inference does not. The actual model and ONNX AAR are already in the archive; do not download them again.

From PowerShell in that folder:

```powershell
.\gradlew.bat :app:testNewModelUnitTest :app:assembleNewModel :app:lintNewModel -PnewModel
```

Output: `app\build\outputs\apk\newModel\app-newModel.apk`. Optional USB installation with Developer options/USB debugging enabled:

```powershell
adb install -r .\app\build\outputs\apk\newModel\app-newModel.apk
```

A locally generated debug key differs from the delivered APK's key, so uninstall the delivered build before installing a locally signed build if Android reports a signature conflict. No private signing key is included.

## Main files for students and teacher

`MainActivity.kt` manages camera permission, preview, result card and bin controls. `CameraAnalyzer.kt` samples frames and closes CameraX images promptly. The build-specific `ScannerFactory.kt` selects the new center pipeline. `CenterScanner.kt` checks object presence, schedules classification and rejects outdated results. `CenterModelRunner.kt` owns the single background worker/session. `MaterialClassifier.kt` verifies model bytes, prepares pixels, runs ONNX and maps labels. `CenterGate.kt` rejects unsuitable center proposals; `AntialiasedResize.kt` implements the image resize. `RecognitionTracker.kt` maintains object identity and the four-second hold. `NewModelRuntimeTest.kt` exercises the actual production classifier; it is included for physical-device diagnosis, but its emulator output check did not finish.

## Licenses

The application and original detector retain AGPL-3.0 obligations, including corresponding source when distributing the APK. SigLIP2 weights are from `prithivMLmods/Augmented-Waste-Classifier-SigLIP2`, revision `d203b52311f70ac3245d5fb8345d88344bec138e`, under Apache-2.0; the weight-only ONNX conversion is documented in `tools/models/NOTICE.txt`. ONNX Runtime 1.22.0 is MIT with third-party notices. The app's existing Licenses screen includes the combined notices. Source license texts and attribution are bundled. Model SHA-256: `fec39a575bd1a2d2434a1f763989bba0c2ecdd3c2cbfd52a27d9f923f53abd10`.
