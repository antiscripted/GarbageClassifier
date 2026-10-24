# Recognition test — 24 October 2026

This is a separate phone-test build on `checkpoint`. **Do not promote it until the owner confirms the fix on the Galaxy M51.** The published `main` APK and all earlier APK/source deliveries remain unchanged.

- [GarbageClassifier-recognition-test.apk](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/checkpoint/apks/recognition-test/GarbageClassifier-recognition-test.apk)
- [Matching source ZIP](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/checkpoint/source/recognition-test/GarbageClassifier-recognition-test-source.zip)
- SHA-256: [APK](../../apks/recognition-test/SHA256SUMS.txt) · [source](../../source/recognition-test/SHA256SUMS.txt)

## Actual blockers and fixes

1. **Narrow objects were excluded before inference.** `CenterGate.select` previously discarded proposals below 15% of the center square and required 25% coverage. A centered bottle occupying 10% of its width and 80% of its height covers only 8%, so it could never reach classification. Selection now considers 4%-coverage proposals and accepts a centered object's substantial long dimension (at least 45%) with a short dimension of at least 8%, or the original 25% area criterion. Tiny details, off-center objects and multiple significant objects still fail. SigLIP2's 0.80 confidence and 0.15 class-margin thresholds are unchanged.
2. **YOLO was a mandatory prerequisite to running SigLIP2.** Stable center frames now reach the asynchronous classifier even without a YOLO match. Detector presence and localization are separate from inference scheduling. Crucially, classifier-only background predictions cannot produce bin advice: a result still needs matching real-object evidence from capture (or the short verified detection grace interval) and a current detector match. If YOLO never supplies a usable object box, the app remains uncertain; this build does not pretend classification alone proves object presence.
3. **One lost detection invalidated pending work and the first confirmation.** A 750 ms grace period now preserves pending work and first independent evidence only while the scene and sampled previous object region still match. No rectangle is drawn while detection is missing. Longer loss, changed appearance, a changed object or a camera gap invalidates the generation. Reacquisition after invalidation starts a new track rather than inheriting a held label. Small narrow-box jitter uses the normal overlap criterion during this short grace interval.
4. **Capture-time expiry could make slow inference impossible to confirm.** Previously a result older than six seconds was rejected and cached evidence expired relative to its original capture. Results may now finish within 15 seconds only if scene/object continuity was maintained throughout; delivery must be processed within 750 ms. Cached evidence is timed from completion, with a 16-second evidence interval allowing a second independent slow result. Expiring old cached evidence no longer cancels a newer pending job. Camera gaps over 1.5 seconds, lifecycle resets and mismatched generations still reject old results. Two independent confident outputs remain necessary; repeated camera frames never acquire a lock.

`CenterEvidence.kt` contains the testable continuity/ticket policy. `CenterScanner.kt` schedules inference, validates completed results and keeps recommendations tied to a real object. Center appearance sampling now includes positions near the center line, which the previous 20/40/60/80% grid could miss for narrow bottles. Existing smoothing, four-second result-card holds, approved wording, inspection colors, licenses and offline model/runtime remain. These are heuristic identity checks, not guaranteed object identity.

## Verification

- **106 JVM tests passed**, zero failures/errors/skips. Fifteen new tests cover narrow upright/horizontal bottles, tiny/off-center/multiple objects, inference without YOLO, brief losses, inference submitted during a brief loss, prolonged loss, removal against an unchanged background, eight-second inference, over-age/delayed results, camera gaps/resets, gradual appearance drift, first-confirmation retention, narrow-box jitter and independent slow confirmations.
- Gradle `testNewModelUnitTest`, `assembleNewModel`, `lintNewModel`, using `-PnewModel --offline`, passed. Lint: zero errors, two existing warnings. No emulator loop or physical-device test was performed.
- APK signing/certificate, ZIP integrity/alignment, actual SigLIP2 and original detector checksums, exact runtime native-library bytes, API 24+/ARM64/ARMv7/x86_64 packaging, complete offline licenses and absence of Internet/microphone permissions were checked. Model SHA-256 remains `63802ff39c00c52663846e4bf7c0dfe511e4e4242398a24240bce733cdbdf366`.
- No training, model download or new model inference benchmark occurred. One inexpensive replay used the already recorded model outputs and detector proposals to check the changed geometric gate. This omits temporal behavior and is not phone validation.

| Cached still-image check | Previous gate | New gate |
| --- | ---: | ---: |
| Correct accepted material, 240 supported RealWaste images | 153 | 162 |
| Wrong accepted material | 11 | 12 |
| Rejected supported images | 76 | 66 |
| Wrong wet/dry recommendation | 2 | 2 |
| Accepted among 120 unsupported/ambiguous images | 22 | 23 |
| TACO: correct among eight supported centered scenes | 4 | 4 |
| TACO: accepted among 96 unsupported/ambiguous scenes | 18 | 15 |

The wider shape eligibility is a tradeoff, not proof that false positives are eliminated. Model errors, missed detector proposals, transparent objects, poor light, clutter, similar-looking replacements and inference above the hard limit can still cause incorrect or uncertain results. The previous still-image accuracy numbers do not establish live-phone reliability.

## Phone test and focused diagnostics

Install this APK as an update to the current app; the package/signing certificate is unchanged. Keep [the published main APK](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/main/apks/GarbageClassifier.apk) for rollback. No UI or permission change is required. Try a centered bottle at normal distance, briefly move it, replace it with another object, remove it, and check an empty background. Also check Wet/Dry inspection. Rectangles should appear only after independent confirmation and disappear during detection loss. The existing result card may briefly retain its last confirmed result after removal.

With Android platform-tools installed and USB debugging enabled on Windows, capture focused diagnostics:

```powershell
adb logcat -v time GarbageClassifierScanner:I GarbageClassifier:E '*:S' > recognition-log.txt
```

Stop with Ctrl+C after reproducing the problem. Logs contain counters and model/result metadata, no camera images, saved photos, audio or uploads:

- `frames`, `proposals`, `object_frames`, `submitted`, `accepted`, `busy`, `confirmed`, `localized` summarize progress every five seconds. `accepted` means identity/timing validation passed; confidence checks and independent confirmation still decide display.
- `result=no_object_evidence` means inference ran, but the safety rule withheld bin advice because no verified matching object was available.
- `reset reason=scene_changed`, `object_changed`, `object_removed`, `detection_lost` or `frame_gap` identifies invalidation.
- `inference_ms` is the classifier's measured native call time; `latency_ms` includes loading/preprocessing and scheduling from capture to completion. `inference_too_old`, `delivery_too_old`, `identity_changed`, `scene_mismatch` and `object_mismatch` identify rejected results.

Real-phone improvements, latency, recognition reliability and box stability require the owner's test. No promotion to `main` has been made or authorized for this fix.

## Source and build

The source ZIP includes actual models, native runtime, Gradle wrapper, licenses, tests and evidence; each file is verified by the packaging manifest. Historical APKs/source archives, local SDKs, caches and private signing keys are excluded. Use Android Studio with JDK 17, SDK 35 and Build Tools 35.0.0:

```powershell
.\gradlew.bat :app:testNewModelUnitTest :app:assembleNewModel :app:lintNewModel -PnewModel
```

Output: `app\build\outputs\apk\newModel\app-newModel.apk`. A locally generated signing key differs from the supplied APK; uninstall first if installing a local build produces a signature mismatch. AGPL-3.0 corresponding-source requirements and all existing model/runtime license notices remain intact; distribute the matching source with this APK.
