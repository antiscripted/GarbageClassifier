# Live recognition test — 24 October 2026

- [GarbageClassifier-live-test.apk](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/checkpoint/apks/live-test/GarbageClassifier-live-test.apk)
- [Matching complete source ZIP](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/checkpoint/source/live-test/GarbageClassifier-live-test-source.zip)
- SHA-256: [APK](../../apks/live-test/SHA256SUMS.txt) · [source](../../source/live-test/SHA256SUMS.txt)

This separate test build follows `e0d1caf5`. `main` and every previous delivery remain unchanged. It is not promoted or claimed to be phone-verified.

## Changed behavior

Whenever the detector selects one valid centered object, its existing smoothed coordinates are now rendered immediately. Before material confirmation, the existing gray dashed rectangle and “Object not clear” label are used. Confirmed material uses the existing blue/purple style; confidently verified wrong-bin objects use orange. If detection is absent, no rectangle is drawn. There are no synthetic boxes or guessed labels.

Overlay verification is separated from the held result card. A held classification cannot color or label a box when current material evidence is uncertain or contradictory. The existing card-hold behavior remains. `Recognition.forOverlay()` applies this distinction; `MainActivity` reuses its existing coordinate transform, overlay labels and status colors. No interface redesign or new wording was introduced.

## Concrete logic errors fixed

The previous policy compared every detected box and completed result to the object's **original coordinates**. A slowly moving bottle could therefore fail the overlap test even though every successive detector frame matched. The full center-image appearance check could also reset that same tracked object merely because its background changed. A third comparison of cached recognition against its old box could discard confirmation again after motion.

The policy now checks geometry against the **last observed object position**, while keeping appearance anchored to the original object to prevent gradual identity drift. Completed results rely on that uninterrupted identity generation and compare object appearance rather than requiring the box to remain at its capture position. Cached classifications are already invalidated by this identity policy and are no longer independently discarded for cumulative translation. Viewport changes, abrupt position jumps, appearance replacement, removal, prolonged detection loss, lifecycle reset and existing time limits still invalidate results. Background-only scenes retain the scene-stability check.

No confidence, class-margin, geometric selection or timing thresholds were changed this turn. The two-independent-result requirement, four-second hold, 750 ms brief-loss handling, 15-second inference limit and completion-time freshness remain. Bin advice still requires actual matching object evidence; a classifier prediction with no verified object remains uncertain.

## Diagnosis limits and logs

The supplied phone feedback was the screen text “Object not clear”; no M51 Logcat rejection records were available in the workspace or supplied in chat. That text cannot distinguish missed detections, resets, slow inference, low confidence or insufficient confirmation. The changes above fix code-reproduced motion errors, not a measured attribution of the phone's failure rate. Real-world recognition may still fail because of model accuracy, lighting, missed detections, confidence or inference time.

The existing `GarbageClassifierScanner` diagnostics now also report:

- `classification=confidence_rejected`, `margin_rejected`, `unsupported_material` or `eligible`;
- `confirmation_pending` and `box_label_verified`, alongside detector/inference counters;
- `inference_finished` with latency and native inference time, even when the result later becomes obsolete;
- `result=discarded_on_reset`, so a completed result is no longer silently erased before its rejection can be diagnosed.

Optional focused capture from Windows with USB debugging and Android platform-tools:

```powershell
adb logcat -v time GarbageClassifierScanner:I GarbageClassifier:E '*:S' > live-test-log.txt
```

Stop with Ctrl+C after reproducing a failure. Diagnostics contain metadata, not camera images, photos, audio or network uploads.

## Essential verification and remaining phone checks

**114 JVM tests passed**, zero failures/errors/skips. Eight added tests cover neutral real-coordinate boxes without bin advice, verified inspection status, held-card/uncertain-box separation, absent-object hiding, gradual motion with changing background, appearance replacement, abrupt position jumps and changed viewports. Existing narrow-object, slow-result, identity and stale-result regressions pass.

Gradle `testNewModelUnitTest`, `assembleNewModel` and `lintNewModel` passed offline; lint has zero errors and two existing warnings. APK signing/original certificate, ZIP integrity/alignment, actual model and native runtime bytes, API 24+/ARM64/ARMv7/x86_64 packaging, offline permissions and complete license accessibility were checked. The source archive is verified file-by-file by SHA-256 and bound to the APK checksum in `SOURCE_MANIFEST.json`.

The CPU-optimized SigLIP2 model is unchanged: `63802ff39c00c52663846e4bf7c0dfe511e4e4242398a24240bce733cdbdf366`. No training, downloads, model benchmarks, cached accuracy replay or emulator inference loop occurred. Tests establish the specified logic behavior, not demonstration readiness or reliable M51 classification.

Install this APK as an update using the existing signing key. On the phone, a valid detector match should now show a neutral box before classification. Check slow motion, material confirmation, wrong-bin color, removal and replacement. Actual detection rate, recognition reliability, timing, coordinate placement and new UI behavior remain unverified on the M51. Keep the [published main APK](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/main/apks/GarbageClassifier.apk) for rollback. No promotion will occur without owner approval.

The source includes the unchanged full licenses, actual weights/runtime and Gradle wrapper. Build with Android Studio/JDK 17, SDK 35 and Build Tools 35.0.0:

```powershell
.\gradlew.bat :app:testNewModelUnitTest :app:assembleNewModel :app:lintNewModel -PnewModel
```

Output: `app\build\outputs\apk\newModel\app-newModel.apk`. Preserve AGPL corresponding-source and all existing Apache/MIT/third-party obligations when distributing the APK. A locally generated debug signing key differs from the supplied APK, so local installation may require uninstalling the supplied build first.
