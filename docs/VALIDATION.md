# Validation and remaining limits

Experimental stability build, **21 October 2026**. Review target: 20–21 October; competition: 25 October. The app remains a prototype with known recognition errors. Software checks do not establish field accuracy or phone performance.

## Results for the new APK

| Check | Result |
| --- | --- |
| Gradle debug APK and Android test APK | Built successfully |
| JVM tests | 75 passed, 0 failed, 0 errors, 0 skipped |
| Python comparison-metric tests | 7 passed |
| Android instrumentation on API 29 x86_64 emulator | 8 passed, 0 failed, 0 skipped |
| Lint | 0 errors, 2 warnings |
| Model asset | Exact original SHA-256 and 2,836,973-byte size verified inside APK |
| Packaging | Signature v2, same certificate as original, ZIP 16 KB alignment and all 9 native ELF libraries checked |
| Permissions | CAMERA only runtime permission; no INTERNET or RECORD_AUDIO |
| Offline legal screen | Bundled texts checked; open/recreation/close and camera resume test passed |
| Camera permission review | Initial denial, permanent denial, Settings, approval and offline-state checks passed |
| Interactive orientation review | Portrait → landscape → portrait, Wet/Dry selection, preserved inspection state and return to Scanner passed |

Evidence is in [validation/experimental-2026-10-21](validation/experimental-2026-10-21/). The [inspection JSON](validation/experimental-2026-10-21/apk-inspection.json) identifies the exact APK. The original APK remains unchanged with SHA-256 `a36e198fcdf690f13ff335d352f181a88a10cdf920f1a70ec06c3fc67759e73b`; its source archive is also unchanged. [Original build validation](https://github.com/antiscripted/GarbageClassifier/blob/5021e619e63750cf603975dc0f9057061e22c54a/docs/VALIDATION.md) remains available at its historical commit.

## Commands and test coverage

The existing cloud environment was reused: Temurin JDK 17.0.20.1+1, Gradle 8.11.1, AGP 8.9.2, Kotlin 2.0.21, SDK/Build Tools 35. No repeated setup or model download was needed. Commands:

```bash
source /workspace/tooling/android-env.sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug --console plain
./gradlew :app:connectedDebugAndroidTest --console plain
/workspace/tooling/compare-venv/bin/python tools/test_accuracy_metrics.py
python tools/inspect_apk.py --apk app/build/outputs/apk/debug/app-debug.apk --report docs/validation/experimental-2026-10-21/apk-inspection.json
```

The first Gradle command completed in 45 seconds. The connected command completed in 4 minutes 10 seconds, including installation and test infrastructure. The eight-test instrumentation runner itself recorded 168.569 seconds. These elapsed times are not inference benchmarks.

The 75 JVM tests comprise Geometry 12, Decoder 17, WasteRules 24 and RecognitionTracker 22. They cover box transforms, clipping, normalized YOLO outputs, invalid tensors, class-aware NMS, verified class order, confidence/margin rules, unsupported classes and wet/dry decisions. Tracker tests cover acquisition, a four-second hold, 1.25-second absence, confidence dips, occlusion, contradictions, replacement, smoothing, separate objects/bins, crossing ambiguity, appearance drift, resets and stalled frames. The unchanged geometry/decoder fixtures were rerun with the new tracking suite.

The eight Android tests actually execute:

1. Real bundled LiteRT inference for three regression images (plastic, metal and paper).
2. No accepted classification from a black image.
3. An uncertain real glass example does not recommend a bin.
4. Sensor-buffer rotations 90°, 180° and 270° preserve detection and mapped box IoU above 0.90.
5. CameraX center-crop coordinate mapping.
6. CameraX 90° preview coordinate mapping.
7. Rear preview reaches STREAMING; bin-inspection/Dry state survives activity recreation and background/foreground.
8. Licenses pauses preview, survives recreation and Close resumes STREAMING.

The interactive review additionally denied camera permission, exercised permanent denial and Open settings, granted through the actual Android dialog, and confirmed Wi-Fi/mobile data were disabled. UI hierarchy bounds and selected states verified portrait, landscape and return to portrait, with Wet/Dry and Scanner/bin-inspection transitions. See `permission-review.json` and `orientation-review.json` with their logs. These assertions do not claim fresh screenshot rendering.

The emulator uses a synthetic rear camera, software CPU emulation without KVM, and API 29 x86_64. It is not a Samsung phone. No new screenshot-based visual alignment claim is made: the existing emulator has a recorded framebuffer/capture limitation. Mathematical transforms, real image inference and actual Android view states are separate checks from live physical-camera overlay placement.

## Model decision and accuracy

The original 320×320 model remains bundled for fully offline inference, with unchanged preprocessing and thresholds. [The completed comparison](EXPERIMENT.md) found better static-image material accuracy from float SigLIP2, but no justified Android CPU deployment. Dynamic-int8 substantially regressed glass/plastic recognition; int4 saved storage without a comparable host CPU improvement and was not accuracy- or Android-tested. No candidate runtime or weights were added; no training occurred.

The stability APK reduces brief result/box changes, but does not change the detector's underlying material accuracy. On 240 known-material RealWaste images the current model gave 86 correct, 80 wrong and 74 rejected results at the exploratory 0.60 cutoff. These static probes do not include application temporal confirmation and are not calibrated field accuracy. A repeated confident error can become a stable wrong classification.

## Errors and warnings

- Kotlin's compiler daemon reported one startup termination. Gradle recovered automatically; compilation, tests and packaging succeeded in that same invocation. No manual build rerun was needed.
- Android analytics initialization cannot write `/home/agent/.android/analytics.settings` in this managed environment. This did not block builds or tests.
- Lint reports the retained landscape `NestedWeights` layout warning and a `UseKtx` style suggestion for `Bitmap.getPixel`. Neither is a correctness error; no cosmetic rebuild was made solely to suppress them.
- Earlier benchmark TACO EXIF orientation handling affected 11 images. Those rows were corrected and rerun before this delivery; both first-pass and corrected evidence remain explicitly labelled. One selected TACO photo returned HTTP 404 and is excluded with a recorded error.
- The UI automation tool briefly returned a null root during window transitions. Its bounded existing retry recovered for both permission and orientation checks; the transient output is retained in the log.
- The existing emulator's screenshot limitation remains; no restart or screenshot recovery loop was attempted for this delivery.

No application bug was found by the completed JVM or Android instrumentation tests that required redesign or model changes.

## Remaining acceptance work

The owner previously tested the original APK on a physical Galaxy M51 and reported that the camera/app work, but glass/plastic confusion, plastic overprediction, uncertainty and angle-sensitive food results remain. Codex has performed **no physical Galaxy M51 testing**. The experimental APK still requires physical validation of tracking, live box alignment, recognition, CPU responsiveness, heat and battery use. No Android 7/API 24, Android 15/16, ARM hardware or 16 KB-page-size device was boot-tested here; the packaging checks do not replace those tests.

The [stability phone checklist](STABILITY.md) specifies same-object comparisons, removal and replacement, multiple objects, inspection, rotation, foreground/background and airplane mode. Record errors as well as successes in `DEVICE_REVIEW.csv`. Sanitary/special-care classes remain unsupported; local segregation rules need teacher/local-authority review. Windows build instructions were documented but not executed on Windows. The APK is debug-signed, with no Play Store release or upload.
