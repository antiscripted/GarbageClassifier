# Garbage Classifier

An offline Android camera scanner for a Class 9 AI project, due **25 October 2026**. It recognizes supported materials, recommends Wet/Dry bins and checks recognizable bin contents. No account, upload, microphone or Internet permission.

**New SigLIP2 center-scanner: experimental and unverified on phones.** A separate build now bundles the real 56.7 MB SigLIP2 model and ONNX Runtime. It improves cached single-object material results, but production Android inference did not finish within a 60-second emulator limit. The older standalone runtime probe returned correct outputs in 23.6 minutes under software emulation. This is not a verified real-time release or a Galaxy M51 speed measurement. No physical phone testing was performed.

- [Download GarbageClassifier-new-model-unverified.apk](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/checkpoint/apks/new-model/GarbageClassifier-new-model-unverified.apk)
- [Matching new-model source ZIP](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/checkpoint/source/new-model/GarbageClassifier-new-model-source.zip)
- [APK SHA-256](apks/new-model/SHA256SUMS.txt) · [source SHA-256](source/new-model/SHA256SUMS.txt)
- [New-model results, limitations and Windows/phone instructions](docs/experiments/center-scanner/DELIVERY.md)

Only `checkpoint` contains this experiment. The stable `development` APK and all older APKs remain preserved.

## Preserved stable downloads and review

- [GarbageClassifier-stable.apk](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/development/apks/stable/GarbageClassifier-stable.apk) · [complete matching source](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/development/source/stable/GarbageClassifier-stable-source.zip)
- Preserved: [original working APK](delivery/GarbageClassifier-debug.apk) · [previous experimental APK](apks/experimental/GarbageClassifier-experimental-debug.apk)
- [Windows installation and build instructions](docs/WINDOWS.md)
- [Main Kotlin files explained for students](docs/ARCHITECTURE.md) · [tracking and phone checklist](docs/STABILITY.md)
- [Tests and limitations](docs/VALIDATION.md) · [code review](docs/CODE_REVIEW.md) · [model comparison and blockers](docs/experiments/2026-10-21/README.md)
- [Indian sorting rules](docs/SEGREGATION.md) · [model contract](docs/MODEL.md) · [licenses](docs/LICENSES.md)

Download the raw APK file and open it on an Android 7/API 24 or newer phone with a rear camera. All supplied APKs share one package/signing key: installing one updates the installed app; saved APK files remain available for rollback. The universal APK supports ARM64, ARMv7 and x86_64. Grant Camera, then hold a well-lit object against a simple background. Uncertain items show “Object not clear”.

To build, open `development` in Android Studio with JDK 17, SDK 35 and the included Gradle wrapper:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

Stable release verification (not the new-model APK): **75 JVM tests, 9 Android scanner tests and 7 metric tests passed**; lint has zero errors. Camera permission, orientation and bin-selection checks passed on the API 29 software emulator. See the validation report for the one skipped optional probe and hardware limitations.

## Repository guide

| Branch | Purpose |
| --- | --- |
| `main` | Owner-approved final version; untouched by this task. |
| `development` | Verified source, APK and reports. |
| `backup` | Preserved previous work; untouched. |
| `checkpoint` | Progress protection during this task. |

Older branches and APKs remain preserved. `app/` contains Android source and its real bundled detector; `apks/` contains named builds; `source/` contains matching archives; `docs/` contains reports/instructions; `tools/` contains verification scripts and preserved candidate assets. `delivery/` and `preservation/` retain historical work. Only verified duplicate partial benchmark files were removed; complete reports and Git history remain.

The application and bundled original model are **AGPL-3.0**. Distribute corresponding source and notices with the APK. First-time builds need development dependency downloads; the installed scanner works entirely offline.
