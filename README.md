# Garbage Classifier

An offline Android camera scanner for a Class 9 AI project, due **25 October 2026**. It recognizes supported materials, recommends Wet/Dry bins and checks recognizable bin contents. No account, upload, microphone or Internet permission.

**New SigLIP2 CPU-optimized APK available. Physical phone speed remains unverified.** The new build uses capability-selected int4 CPU kernels while retaining float compute in the first and last transformer blocks. It improves the recorded supported-image result from 152 to **153/240 correct**, retaining **two wrong wet/dry recommendations**. All weights remain unchanged. Desktop inference measured a 197 ms median across 464 center crops; this is not a Galaxy M51 measurement or proof of the 1–2-second target on phones.

- [Download GarbageClassifier-new-model.apk](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/checkpoint/apks/new-model-cpu/GarbageClassifier-new-model.apk)
- [Matching complete source ZIP](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/checkpoint/source/new-model-cpu/GarbageClassifier-new-model-source.zip)
- [APK SHA-256](apks/new-model-cpu/SHA256SUMS.txt) · [source SHA-256](source/new-model-cpu/SHA256SUMS.txt)
- [Performance, tests, limitations and Windows/phone instructions](docs/experiments/int4-cpu/README.md)
- [Previous unverified SigLIP2 APK](apks/new-model/GarbageClassifier-new-model-unverified.apk) remains preserved.

The build, all 85 JVM tests, lint and APK integrity checks passed. A bounded Android test verified small real-weight matrix outputs and full production model loading. Full-model ARM inference and live phone behavior remain untested. No costly full-model software-emulator run was repeated. Only `checkpoint` contains this delivery; protected branches and previous APKs are unchanged.

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
