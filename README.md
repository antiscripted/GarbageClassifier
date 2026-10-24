# Garbage Classifier — live recognition test

This `checkpoint` build separates visible object detection from material confirmation and fixes cumulative-motion rejection. **It has not been verified on the M51; `main` stays unchanged until owner approval.**

- [Download GarbageClassifier-live-test.apk](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/checkpoint/apks/live-test/GarbageClassifier-live-test.apk)
- [Matching complete source ZIP](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/checkpoint/source/live-test/GarbageClassifier-live-test-source.zip)
- [Exact changes, verification, limitations and diagnostics](docs/live-test/README.md)

A valid centered detector box now appears immediately in the existing neutral/dashed style. Material labels and normal/wrong-bin colors require verified classification. Continuous object tracking tolerates gradual motion and changing backgrounds while retaining original appearance, removal and timeout safeguards. Model weights, confidence thresholds, UI wording and licenses are unchanged.

114 JVM tests, build, lint and APK/model/license checks passed. These results do not establish real-phone recognition reliability. No M51 rejection logs were supplied; the fixes address code-reproduced errors. All previous deliveries remain preserved, including the [preceding recognition test](apks/recognition-test/GarbageClassifier-recognition-test.apk).

## Preserved main submission

**School submission — 25 October 2026.** A native Android application that classifies one centered waste object automatically, fully offline. Kotlin, CameraX, LiteRT and CPU-optimized SigLIP2/ONNX Runtime. No accounts, uploads, microphone or Internet permission.

### Main downloads (unchanged)

- **[GarbageClassifier.apk](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/main/apks/GarbageClassifier.apk)**
- **[Complete matching source ZIP](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/main/source/GarbageClassifier-source.zip)**
- SHA-256: [APK](apks/SHA256SUMS.txt) · [source](source/SHA256SUMS.txt)
- [Submission verification](docs/SUBMISSION.md) · [Preserved fallback APKs and source](backup/README.md)

The APK is unchanged from reviewed final candidate `20ea77c2a74e8e0aa23a82d3df9be470d459cbe8`. It includes stable rectangles for the confirmed tracked object, Wet/Dry Bin Inspection, four-second result holds, stale-result protection and a compact Licenses component list with complete offline texts. Inspection covers the centered object, not every item in a bin.

## Install on Android

1. Download the APK above. Keep a [verified stable fallback](apks/stable/GarbageClassifier-stable.apk) available.
2. Open it and allow installation from the browser/file manager if Android requests it. Supplied APKs share the previous signing key and update the same installed application.
3. Grant Camera. Hold one well-lit object centrally against a simple background. Wait for confirmation before expecting a labeled rectangle. In Bin Inspection, select Wet or Dry; the identified object uses the existing wrong-bin color when appropriate.
4. In Licenses, select a component to read its complete text. Close returns to the list; closing the list resumes scanning.

API 24+ is targeted, with ARM64, ARMv7 and x86_64 libraries bundled. The project owner reports that the preceding CPU-optimized APK works on a physical Samsung Galaxy M51. The final candidate's rectangle placement and license navigation have not received new physical-phone verification. Recognition and localization can still be incorrect; uncertain results must not be treated as confirmed sorting advice.

## Build on Windows

Install Android Studio, **JDK 17, Android SDK Platform 35 and Build Tools 35.0.0**. Extract the source ZIP, open its `GarbageClassifier` folder, select JDK 17 for Gradle, and let Android Studio configure the SDK location. The real models and native runtime are included. Initial Gradle dependency resolution requires internet; installed inference does not.

Run PowerShell in the project folder:

```powershell
.\gradlew.bat :app:testNewModelUnitTest :app:assembleNewModel :app:lintNewModel -PnewModel
```

APK output: `app\build\outputs\apk\newModel\app-newModel.apk`. Optional installation with USB debugging enabled:

```powershell
adb install -r .\app\build\outputs\apk\newModel\app-newModel.apk
```

A locally generated debug key differs from the supplied APK key. If Android reports a signature mismatch, uninstall the supplied app before installing the locally built one; this clears app settings and permissions. Private signing keys and local SDK/build caches are not distributed.

Verify downloaded files against the linked SHA256SUMS files:

```powershell
Get-FileHash .\GarbageClassifier.apk -Algorithm SHA256
Get-FileHash .\GarbageClassifier-source.zip -Algorithm SHA256
```

## Review guide

Reused verification: **91 JVM tests passed**, APK build passed, lint had **zero errors and two existing warnings**. Model, signing, offline packaging, full license accessibility and source-manifest checks are documented in [submission verification](docs/SUBMISSION.md). No new model benchmark or emulator loop was run for publication.

`MainActivity.kt` manages permission, results, bin selection and license dialogs. `CameraAnalyzer.kt` handles camera frames. `CenterScanner.kt` connects asynchronous SigLIP2 classification to the current detected object; `CenterGate.kt` checks box association. `RecognitionTracker.kt` smooths boxes and maintains confirmation/hold/reset rules. `DetectionOverlay.kt` draws rectangles with existing colors. `MaterialClassifier.kt` runs the bundled model and `CenterModelRunner.kt` owns its background worker.

| Location | Contents |
| --- | --- |
| `app/` | Android project source, resources, detector and licenses |
| `tools/models/`, `tools/android-runtime/` | Actual SigLIP2 weights and native runtime |
| `apks/GarbageClassifier.apk` | Current submission APK |
| `source/GarbageClassifier-source.zip` | Matching complete source |
| `backup/` | Previous new-model APKs/source/checksums and reviewed candidate |
| `apks/stable/`, `source/stable/`, `delivery/` | Stable and original old-model fallbacks |
| `docs/`, `tools/` | Instructions, tests, research evidence and verification scripts |

`main` is the approved submission. `checkpoint`, `development`, `backup` and historical Git branches remain preserved. The repository's `backup/` directory is separate from the `backup` branch. Historical reports retain their original context; the links at the top of this README identify the current delivery.

## Licenses

The application and original detector are **AGPL-3.0**; distribute complete corresponding source and notices with the APK. SigLIP2 is Apache-2.0; ONNX Runtime is MIT with third-party notices. All full license texts, copyright notices, model attributions and source obligations remain in the source and the offline Licenses menu. [License details](docs/LICENSES.md) · [Indian segregation rules](docs/SEGREGATION.md).
