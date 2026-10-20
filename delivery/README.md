# Garbage Classifier review delivery — 20 October 2026

- `GarbageClassifier-debug.apk`: installable Android debug application, minimum Android 7.0/API 24, with the actual waste model bundled for offline inference.
- `GarbageClassifier-source.zip`: matching complete Android Studio source, Gradle wrapper, model, tests, license notices, Windows instructions and validation evidence. Every archived file is listed and hashed in `SOURCE_MANIFEST.json`.
- `SHA256SUMS.txt`: SHA-256 values for the APK and source ZIP.

Copy the APK to the phone, open it in Samsung My Files, allow installation from that source if requested, then launch Garbage Classifier and grant camera access. No account or Internet connection is required. Test with airplane mode enabled and Wi-Fi off. No physical Galaxy M51 testing is claimed.

To build, extract the source ZIP, open its `GarbageClassifier` folder in Android Studio, select a full JDK 17, and install Android SDK Platform 35 and Build Tools 35.0.0. In PowerShell from that project folder:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

The built APK is `app\build\outputs\apk\debug\app-debug.apk`. The first build needs Internet access for development dependencies; installed inference stays offline. `docs/WINDOWS.md` gives exact installation, SDK, Java, USB and troubleshooting steps. Windows execution has not been performed in this cloud environment.

`docs/ARCHITECTURE.md` explains the main Kotlin files for the students and teacher. `docs/VALIDATION.md` separates completed checks from untested physical-device work. `docs/SEGREGATION.md` explains the five-material wet/dry policy and its limits. `docs/LICENSES.md` records AGPL-3.0 model/application obligations and third-party notices.

The build and 53 JVM tests pass; eight earlier Android tests pass for the identical APK. One documented NestedWeights lint performance warning remains. The pretrained model still makes confident material mistakes, including some incorrect wet recommendations. Keep this described as an educational review build until accuracy and real-device behavior are accepted by the teacher. No model retraining or replacement was performed in the 20 October finishing task.

The source ZIP excludes historical preservation snapshots, generated build caches, machine-specific local.properties, Git metadata and signing keys. Those are not needed to build the application. Earlier snapshots remain on the repository's separate preservation branches. Distribute the complete source ZIP and notices together with the APK under the included AGPL-3.0 terms.
