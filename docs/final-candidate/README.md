# Final candidate — 23 October 2026

[Download APK](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/checkpoint/apks/final-candidate/GarbageClassifier-final-candidate.apk) · [Matching source ZIP](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/checkpoint/source/final-candidate/GarbageClassifier-final-candidate-source.zip) · [APK SHA-256](../../apks/final-candidate/SHA256SUMS.txt) · [Source SHA-256](../../source/final-candidate/SHA256SUMS.txt)

## Changes

The center scanner now exposes the existing lightweight detector's smoothed box only when there is one visible, confirmed track with the same class, overlapping geometry and matching appearance as the current confident SigLIP2 evidence. Existing generation/age checks still reject stale inference. Uncertain or contradictory evidence hides the rectangle. Removal hides it immediately while retaining the existing result-card hold. Changed identity, resets and ambiguous multiple objects cannot inherit a labeled rectangle.

`CenterScanner.kt` reuses `RecognitionTracker` smoothing and `CenterGate.canLocalize`; `MainActivity` reuses its existing CameraX coordinate transform and `DetectionOverlay`. Normal rectangles retain blue/purple; wrong-bin rectangles retain orange. Material labels and cards are unchanged. Inspection describes only the centered identified object, not all contents of a bin. The detector can still mislocalize an object; no claim of perfect real-world boxes is made.

Licenses now opens a six-item list of existing component names. Selecting an item opens its full selectable, scrollable license/notice text. Close or Back returns to the list; closing the list resumes the camera. The selected component survives activity recreation. Text is loaded only when its item opens. The former combined notice was split losslessly: concatenating the four resulting files reproduces every original byte. AGPL, Apache, LiteRT, SigLIP2, ONNX MIT/third-party notices, copyrights, model revisions and source obligations remain accessible offline. `index.json` maps components to existing full texts without abbreviating them.

The only documentation cleanup is a clear final-candidate README. No historical branches, APKs, archives or experiment evidence were deleted. No models, inference arithmetic, thresholds, runtime libraries or camera geometry were changed. Only `checkpoint` receives this delivery; `main`, `development` and `backup` remain unchanged.

## Essential verification

| Check | Result |
| --- | --- |
| JVM tests | **91 passed**, zero failures/errors/skips. Six added regressions cover independent confirmation, smoothed detector coordinates, per-object bin status, uncertainty/contradictions, identity/position changes, removal and reset. Existing rotation/geometry and stale-evidence tests also pass. |
| Gradle | `testNewModelUnitTest`, `assembleNewModel`, `lintNewModel`, with `-PnewModel --offline`: passed. |
| Lint | Zero errors; two unchanged warnings: NestedWeights and UseKtx. Android build analytics also emitted a nonfatal read-only settings warning. |
| APK | CRC/integrity, signature v2, original signing certificate, compact ZIP alignment, all 15 native libraries, ARM64/ARMv7/x86_64, API 24 minimum / target 35, 64-bit ELF alignment passed. |
| Model/runtime | Actual CPU-optimized SigLIP2 weights and ONNX libraries match expected bytes. Original detector remains bundled for presence/localization only. No classifier fallback was added. |
| Offline/licenses | No INTERNET or microphone permission. All six catalog entries resolve to every bundled license text, with exact source/APK byte matches. Original combined notice reconstruction is byte-identical. |
| Source archive | Packaging verifies each archived file by SHA-256 and records the APK checksum in `SOURCE_MANIFEST.json`. See adjacent checksum files. |

Model SHA-256 remains `63802ff39c00c52663846e4bf7c0dfe511e4e4242398a24240bce733cdbdf366`. Previous model benchmarks are reused, not rerun. The project owner reports the preceding CPU-optimized APK works on a physical Galaxy M51 with somewhat better recognition; that is owner feedback, not a Codex phone test. This final candidate's rectangle placement, smoothness, wrong-bin highlighting and license navigation/rotation still need a brief physical-phone check. No new emulator loop, full-model inference benchmark or physical-device test was run for this polish task. Detector/localization accuracy and varied phone performance remain limitations.

## Windows build and installation

Download the APK and keep the preceding working APK for rollback. Open it on the phone, allow installation from the browser/file manager when Android requests it, and grant Camera. The same application ID/signing key allows updating the previous supplied APK. Test one well-lit centered object in airplane mode, then move/remove/replace it and try Wet/Dry inspection. Wait for independent confirmation before expecting a labeled rectangle. Check Licenses, open one component and Close back to the list, then Close to resume scanning.

Verify downloaded files in PowerShell against the linked SHA256SUMS files:

```powershell
Get-FileHash .\GarbageClassifier-final-candidate.apk -Algorithm SHA256
Get-FileHash .\GarbageClassifier-final-candidate-source.zip -Algorithm SHA256
```

Extract the matching source ZIP and open `GarbageClassifier` in Android Studio. Use JDK 17, SDK Platform 35 and Build Tools 35.0.0. Let Android Studio configure the SDK location. First-time Gradle dependencies need internet; model/runtime files are already included and installed inference is offline.

```powershell
.\gradlew.bat :app:testNewModelUnitTest :app:assembleNewModel :app:lintNewModel -PnewModel
adb install -r .\app\build\outputs\apk\newModel\app-newModel.apk
```

The optional ADB installation needs USB debugging. A locally generated debug key differs from the supplied APK key; uninstall the supplied app first if Android reports a signature mismatch, which clears app settings/permissions. Private signing keys and SDK/build caches are excluded from the source archive.

For students: `MainActivity` manages permission, result cards, bin controls and the component license dialogs. `CameraAnalyzer` receives/closes camera frames. `CenterScanner` links asynchronous SigLIP2 results to the current detected object. `CenterGate` rejects ambiguous object/box associations. `RecognitionTracker` smooths boxes and preserves confirmation/hold/reset rules. `DetectionOverlay` draws mapped rectangles with existing status colors. `MaterialClassifier` and `CenterModelRunner` retain the verified model and single background inference worker.

Distribute this APK with its matching complete source and notices. The application/original detector remain AGPL-3.0, SigLIP2 Apache-2.0 and ONNX Runtime MIT with its third-party obligations. All full license texts remain in the source and the app's Licenses menu.
