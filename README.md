# Garbage Classifier

Fully offline native Android waste scanner for the **25 October 2026** school submission. Kotlin, CameraX, LiteRT and ONNX Runtime; no accounts, uploads, microphone or Internet permission.

## Final candidate

- [Download GarbageClassifier-final-candidate.apk](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/checkpoint/apks/final-candidate/GarbageClassifier-final-candidate.apk)
- [Download matching complete source ZIP](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/checkpoint/source/final-candidate/GarbageClassifier-final-candidate-source.zip)
- SHA-256: [APK](apks/final-candidate/SHA256SUMS.txt) · [source](source/final-candidate/SHA256SUMS.txt)
- [Final changes, test results, limitations and Windows instructions](docs/final-candidate/README.md)

This candidate restores smoothed detector rectangles only for the confidently identified, same tracked centered object. Wrong-bin items use the existing inspection color. Inspection remains single-object; it does not certify an entire bin. Licenses opens a compact component list, with all complete notices and license texts accessible offline.

The CPU-optimized SigLIP2 model, confidence thresholds, four-second holds and stale-result safeguards are unchanged. The project owner reported that the preceding CPU-optimized APK works on a physical Samsung Galaxy M51 with somewhat better recognition. **The final candidate's restored rectangles and license navigation have not yet been physically verified.**

Essential checks passed: **91 JVM tests**, APK build, lint with zero errors/two existing warnings, signing/model/runtime integrity, offline permissions, complete license accessibility and lossless notice preservation. No model benchmarks or expensive emulator inference were repeated. API 24+, ARM64, ARMv7 and x86_64 remain packaged; physical compatibility on all devices is not claimed.

## Build and install

Open the source folder in Android Studio with **JDK 17, SDK Platform 35 and Build Tools 35.0.0**. Use the included Gradle wrapper:

```powershell
.\gradlew.bat :app:testNewModelUnitTest :app:assembleNewModel :app:lintNewModel -PnewModel
```

The output is `app\build\outputs\apk\newModel\app-newModel.apk`. First-time development dependency resolution needs internet; installed inference does not. Open the downloaded APK on the phone and allow installation from that browser/file manager, then grant Camera. The supplied APK shares the previous signing key and installs as an update. Keep the previous APK for rollback.

## Repository guide

`checkpoint` holds this final candidate for review. `main`, `development` and `backup` remain unchanged; promotion requires owner approval. `app/` contains Kotlin and Android resources, `tools/models/` the actual SigLIP2 weights, `tools/android-runtime/` the bundled runtime, `apks/` named APKs, `source/` matching archives and `docs/` instructions/evidence.

All historical branches, APKs, archives and experiment evidence are preserved. [Previous CPU-optimized APK](apks/new-model-cpu/GarbageClassifier-new-model.apk) · [stable release](apks/stable/GarbageClassifier-stable.apk) · [original APK](delivery/GarbageClassifier-debug.apk).

[Architecture](docs/ARCHITECTURE.md) · [Indian segregation rules](docs/SEGREGATION.md) · [Model performance evidence](docs/experiments/int4-cpu/README.md) · [Licenses](docs/LICENSES.md).

The app and original detector retain AGPL-3.0 source-distribution obligations. SigLIP2 is Apache-2.0; ONNX Runtime is MIT with third-party notices. Distribute the matching source and notices with the APK.
