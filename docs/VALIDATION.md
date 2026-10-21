# Stable delivery validation — 21 October 2026

`GarbageClassifier-stable.apk` includes the stale-result fix and retains the original offline detector. Existing valid results were reused; no model experiments or optional probes were repeated during final delivery.

## Passed checks

| Check | Result |
| --- | --- |
| Gradle debug and Android test APK build | Passed |
| JVM tests | 75 passed: Geometry 12, Decoder 17, WasteRules 24, RecognitionTracker 22 |
| Python comparison-metric tests | 7 passed |
| Android scanner tests, API 29 x86_64 emulator | 9 passed, 0 failures/errors; 1 optional candidate test skipped |
| Lint | 0 errors, 2 retained warnings |
| Camera permission | Initial denial, permanent denial, Open settings, actual Android approval dialog and return to scanner passed |
| Orientation and bin selection | Portrait → landscape → portrait; Wet/Dry changes, inspection state preservation and returning to Scanner passed |
| Offline state | Emulator Wi-Fi and mobile data disabled; APK has no INTERNET or microphone permission |
| Model | Original 2,836,973-byte TFLite asset verified inside APK |
| APK | Signature v2, original signing certificate, ZIP 16 KB alignment and all 9 native ELF library alignments verified |
| Licenses | Bundled notices verified; dialog open/recreation/close and camera resume passed |
| Preservation | Original APK/source and previous experimental APK/source still match recorded SHA-256 values |

Evidence: [validation/stable](validation/stable/). The [APK inspection](validation/stable/apk-inspection.json), [Android XML](validation/stable/android-tests.xml), permission/orientation reports and preserved-artifact checksums identify the exact checks. Earlier results remain in their historical folders.

The XML has ten test cases: nine passed and one conditional candidate probe skipped. Gradle's progress display ends at “11/10”; this is not an eleventh scanner test. The runner reports ten cases and the XML confirms the skip. The connected Gradle task succeeded in 3 minutes 41 seconds. The instrumentation runner recorded 172.682 seconds; these are test-run durations, not inference latency.

## Android coverage

The nine scanner tests execute real bundled-model inference on plastic, metal and paper fixtures; reject a black image and an uncertain glass fixture; check sensor rotations 90°/180°/270° and box IoU above 0.90; verify CameraX center-crop and 90° preview transforms; check live rear preview, bin state across recreation and background/resume; verify Licenses pauses/resumes preview; and exercise the new expiry regression.

The expiry test sets an expired result on the actual activity, invokes its expiry runnable, and verifies that tracking generation increments and the UI says “Object not clear”. This closes the previous 900–1,500 ms gap in which a cleared classification could be restored from old tracker state. [CODE_REVIEW.md](CODE_REVIEW.md) explains the change.

The 22 tracker unit tests cover repeated confirmation, four-second holds, removal, replacement, ambiguity, contradictions, smoothing, independent objects/bins, resets and frame stalls. Stable output is not proof of correct material recognition.

Permission and orientation reviews used actual Android dialogs and fresh accessibility hierarchy bounds/selected states. One null-root transition recovered through the existing bounded retry. No screenshot-based or physical-camera alignment claim is made; the existing software emulator has a framebuffer limitation.

## Artifact identity and commands

APK SHA-256: `3dc4a038bad73efb15409aa17af6030d813080460dee2dd86b301f0dc1e2ca2d`.

Bundled model SHA-256: `9c258ac8a0344cd07deb28ddcedb096e5bd30c092b807ff7e442f40279d8f54d`.

The APK supports API 24+, targets API 35, and contains ARM64, ARMv7 and x86_64 libraries. It contains neither the candidate ONNX weights nor ONNX Runtime. The original APK remains unchanged at SHA-256 `a36e198fcdf690f13ff335d352f181a88a10cdf920f1a70ec06c3fc67759e73b`.

The existing JDK 17, Gradle 8.11.1, AGP 8.9.2, Kotlin 2.0.21 and SDK/Build Tools 35 environment was reused:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug --offline
./gradlew :app:connectedDebugAndroidTest --offline
python -m unittest discover -s tools -p test_accuracy_metrics.py -v
python tools/inspect_apk.py --apk app/build/outputs/apk/debug/app-debug.apk --report docs/validation/stable/apk-inspection.json
```

Lint was also rerun after the final test-only probe configuration change. Packaging copies the inspected APK without rebuilding and generates a source manifest tied to its SHA-256. Every archived file is checksum-verified; archives and old APKs are excluded from the source ZIP to avoid recursive duplication. Corresponding checksums accompany the new files.

## Why the new model was not integrated

[The completed comparison](experiments/2026-10-21/README.md) records all results. DETR + SigLIP2 int4 improves known-material RealWaste correctness from 86/240 to 184/240 at 0.60 confidence, but wrong wet/dry recommendations increase from 5 to 13. On 104 cluttered TACO scenes it gives 66 correct material-and-box matches but 175 unmatched boxes and 405 missed targets. Higher confidence still leaves substantial localization errors.

The smaller original-detector + SigLIP2 alternative improves material/bin metrics, but misses 463/494 TACO targets. Its Android latency and output parity remain unverified: the earlier four-call software-emulator probe was deliberately stopped after 531 host seconds without completing. The recorded “Process crashed” message came from that force-stop, not an independent observed crash. No probe was repeated for final delivery. The snapshot PSS was about 162 MiB, not a peak or combined-model RAM requirement. DETR was not executed on Android.

Consequently, no `GarbageClassifier-new-model.apk` is claimed. The shipped scanner remains fully offline with the original model. No training, production threshold changes, redesign or silent model substitution occurred.

## Errors and remaining limits

- During the earlier candidate work, a metadata-path lookup and a missing ID-list path failed before inference; paths were corrected and an explicit metadata option added. The first probe compilation incorrectly called an instance version method statically; it was corrected and the build passed. Logs are retained.
- Android analytics could not write the managed home directory. Native debug symbols in the test-only runtime could not be stripped. Neither blocked the final scanner build.
- Lint retains `NestedWeights` in the landscape layout and a `UseKtx` style suggestion. No correctness errors remain in lint.
- Original-model glass/plastic confusion, plastic overprediction and angle-sensitive food recognition remain. Similar-looking objects and lighting changes can defeat RGB-grid tracking. Hidden/contaminated contents and sanitary/special-care items are not reliably recognized.
- Codex performed **no physical Galaxy M51 testing**. Actual ARM latency, heat, battery use, sustained live recognition and physical-camera overlay placement remain untested. API 24, newer Android versions and a 16 KB-page-size device were not boot-tested here; packaging checks are not hardware tests.
- Windows instructions are provided but were not executed on Windows. The APK is debug-signed, not a Play Store release.

Use the [phone checklist](STABILITY.md) and [Windows instructions](WINDOWS.md) for teacher/student acceptance before the 25 October deadline. `main`, `backup` and the previous APKs remain preserved.
