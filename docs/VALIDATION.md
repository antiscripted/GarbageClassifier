# Validation and remaining limits

Review date: **20 October 2026 (India)**. Student/teacher review target: **20–21 October**; competition deadline: **25 October**. The APK is an educational review build. Software checks do not establish reliable waste recognition.

## Build and packaging checks

The actual Linux cloud build used Temurin JDK 17.0.20.1+1, Gradle 8.11.1, Android Gradle Plugin 8.9.2, Kotlin 2.0.21, SDK Platform 35 and Build Tools 35.0.0. Command:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug --console plain
```

The debug APK and test APK built successfully. The Gradle command was run again on 20 October, with the JVM test task explicitly rerun (`:app:testDebugUnitTest --rerun`); 53 tests passed. The APK SHA-256 remains `a36e198fcdf690f13ff335d352f181a88a10cdf920f1a70ec06c3fc67759e73b`, identical to the APK used by the passing eight-test Android run on 19 October. No application source or model change was needed in the finishing pass. Lint has zero errors and one `NestedWeights` performance warning for the two-axis landscape layout. The earlier baseline-alignment warning was fixed; the remaining small layout is retained without suppression or another layout dependency. Landscape controls scroll on short displays.

Static APK inspection verified the real model's exact bytes/SHA-256, no test photographs in the main APK, no INTERNET or RECORD_AUDIO permission, and only CAMERA as a runtime permission. AndroidX also contributes an app-scoped signature permission for private receiver handling. `apksigner verify --verbose` passes with APK Signature Scheme v2. `zipalign -c -P 16 4` passes, and ELF inspection finds 16 KB load-segment alignment in all nine packaged native libraries. These checks support modern packaging compatibility; no Android 15/16 or 16 KB-page-size device was boot-tested here.

The bundled ABIs are arm64-v8a, armeabi-v7a and x86_64. Minimum SDK is 24; target/compile SDK is 35. API 24, API 35 and physical ARM devices remain untested. No Play Store release signing or upload was performed.

## Automated tests

The JVM suite has **53 tests**: GeometryTest 12, WasteRulesTest 24 and YoloDecoderTest 17. It covers letterboxing, edge/crop clipping, rotations, invalid/nonfinite model outputs, exact tensor length, normalized box decoding, class order, class-aware NMS, score/margin cutoffs, unsupported classes, temporal confirmation and wet/dry inspection decisions. It includes a real model-output fixture. The final unit command reruns the test task rather than relying only on an up-to-date build result.

The Android suite has eight tests:

1. `bundledModelDetectsThreeRealMaterialExamples`: real LiteRT inference for plastic, metal and paper, confidence checks and source-coordinate box bounds.
2. `emptyImageDoesNotProduceClassification`: a black image produces no accepted detection.
3. `uncertainRealExampleDoesNotRecommendABin`: the selected glass regression image stays uncertain.
4. `allSensorRotationsPreserveObjectAndBoxLocation`: 90°, 180° and 270° sensor-buffer variants preserve the material and box IoU above 0.90 after mapping.
5. `cameraXMappingAccountsForCenterCrop`: known full-buffer rectangle maps to the expected cropped preview.
6. `cameraXMappingAccountsForNinetyDegreePreviewRotation`: a known box maps correctly through CameraX's rotated preview transform.
7. `cameraStreamsAndBinSelectionSurvivesRecreation`: actual rear-camera preview reaches STREAMING; Dry/bin-inspection selection survives recreation; preview resumes after background/foreground.
8. `licensesSurviveRecreationAndCloseResumesPreview`: the preview becomes IDLE while Licenses is open, the dialog survives recreation, and Close resumes STREAMING.

Existing Android result from 19 October for this exact APK: **8 passed, 0 failed**, 168.097 seconds; the complete final runner output is in `docs/validation/final/instrumentation-final-pass.log`. The freshly rerun JVM suite reports **53 passed, 0 failed, 0 errors, 0 skipped**. The emulator is API 29 x86_64 with a synthetic rear camera, SwiftShader and software CPU emulation, without KVM. Its Wi-Fi and mobile data are disabled. It is not a Samsung phone and its runtime is not a performance benchmark.

## Finishing checks on 20 October

The actual Android permission dialog was exercised from cleared application data: initial denial shows Camera permission required / Allow camera; permanent denial shows Open settings; that action opens Android application settings; a fresh ALLOW returns directly to the scanner. Wi-Fi and mobile data were both confirmed disabled. The results are in `docs/validation/final/permission-review.json` and the matching log.

Actual Android view-state assertions also passed for portrait preview/result bounds, Wet/Dry selection, a landscape transition preserving bin-inspection/Dry, and return to portrait preserving Wet. Switching back to Scanner hides bin-selection controls. These are recorded in `docs/validation/final/orientation-review.json`; they verify layout/state, not fresh screenshot rendering or physical box alignment. The emulator's screenshot limitation is described below. No production application bug was found that required a source change.

The earlier eight-test Android run was reused because the APK bytes are unchanged. The source APK, model and existing license screen were retained without retraining, replacement, new wording or feature redesign. Only review evidence, branch/build instructions and delivery files were completed.

## Model evaluation

The real model was independently inspected and executed on the host, then in Android's LiteRT runtime. `tools/verify_model.py` revalidates its checksum, tensor types/shapes, 65 int8 weight tensors, finite outputs, real fixtures and blank-image behavior. `tools/probe_materials.py` exactly reproduced the first 60 recorded per-image probe results. Larger exports and a separate YOLOv8m checkpoint were evaluated; details and raw evidence are in MODEL.md and `docs/validation`.

The requested model remains demonstrably unreliable on some materials: on the follow-up 200 known-material images, 125 were accepted but only 82 matched the dataset material, including five incorrect wet recommendations for dry examples. There are no wet-food ground-truth examples in those probes. These are material checks, not box-based mAP or calibrated confidence estimates. Temporal confirmation cannot fix repeated confident mistakes. No alternative tested established a dependable replacement. General accuracy and wet-food recognition on local classroom examples remain acceptance blockers, even though the application runs offline.

## Errors encountered and resolution

- The initial Java runtime lacked a compiler. A checksum-verified full Temurin JDK 17 was installed outside the project.
- The downloaded JDK initially lacked the cloud proxy's trusted CA. Gradle uses the platform Java trust store; TLS verification was not disabled.
- Model-CDN access was denied. The author-hosted byte-identical mirror was verified against the pinned Hugging Face LFS SHA-256 before use.
- ADB initially needed a writable Android user directory. It was prepared with the required filesystem permission; no credentials were copied and HOME was not repurposed.
- The first license UI test accidentally matched “close” inside the legal text. A more specific button selector then exposed the platform's automatic “CLOSE” capitalization. The dialog now explicitly renders the approved “Close”, and the test targets the actual button. The failed runs are retained, not described as passing runs.
- An interactive `uiautomator` dump briefly returned a null root during a window transition. A later dump and screenshot established the dialog's actual state. No application crash was recorded in that investigation.
- On 20 October, the long-running software emulator returned black screenshots with the display still ON and logged `openColorBuffer` framebuffer errors. Its UI hierarchy remained responsive. An emulator restart was attempted to restore capture. Its slow software boot also raised a System UI (not Garbage Classifier) ANR dialog, dismissed with Wait. Fresh screenshot capture remained unreliable, so it is not claimed as completed visual verification; this was an environment issue, not a reason to modify application code. The failed capture is explicitly retained as `screenshots/emulator-framebuffer-failure.png`. Current rotation checks use Android view bounds and selected states; the retained valid portrait screenshot is dated 19 October.
- The ad hoc orientation helper initially encountered the System UI dialog and then an already-landscape state during resume. Its state checks were corrected to continue from the actual orientation; no application change was made for these test-harness issues.
- Android metrics initialization emits a read-only `analytics.settings` warning in this managed environment; builds and tests still run.
- Official Indian government references and GitHub's PR API were blocked by the network proxy. Their domains were added to the configuration draft; current local-rule verification and automatic PR creation are not claimed.

## Physical M51 and teacher review checklist

None of the following physical-device checks has been performed here. Record both correct and wrong outcomes; do not rehearse only successful samples.

- Install the final APK on the M51. Test a clean install with permission denial, later approval, permanent denial/Open settings and relaunch. Confirm the rear preview opens immediately after permission.
- Enable airplane mode and explicitly disable Wi-Fi. Reboot/reopen the app and confirm the scanner works without downloads or network permission.
- Try at least ten safe, ordinary examples each of food waste, glass, metal, plastic and paper/cardboard, with two lighting conditions and different backgrounds. Include unsupported and ambiguous objects. Record material, recommendation, uncertainty and errors in DEVICE_REVIEW.csv.
- Confirm purple boxes follow the same object while moving near each screen edge in portrait and landscape. Check tall/wide objects, partial occlusion, multiple objects, device rotation and returned foreground state. Do not count a correct category as proof of correct box placement.
- Compare the same ordinary objects in Wet and Dry bin-inspection modes. Confirm wrong/correct/uncertain states, and explain that unseen contents are not inspected.
- Run for ten minutes. Observe preview smoothness, time to first stable result, battery/temperature and memory behavior. Record actual phone measurements instead of quoting host timing or a promised FPS.
- Open/close Licenses, rotate with it open, then return to the camera. Check large text, small screens, TalkBack and one-handed use.
- Check local segregation instructions with the teacher/local authority, especially sanitary and special-care streams; do not test with hazardous materials.
- Review MODEL.md, LICENSES.md, the source and the full failed/passed evidence before presenting. Keep the app described as a prototype if accuracy remains inadequate.

The delivery contains complete source and a debug APK, but physical-device acceptance, current local-rule confirmation and recognition reliability remain limitations. Windows commands were not executed on a Windows machine here.
