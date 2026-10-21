# Garbage Classifier

An offline Android camera scanner for a Class 9 AI project, due **25 October 2026**. It recognizes supported materials, recommends Wet/Dry bins and checks recognizable bin contents. No account, upload, microphone or Internet permission.

**The improved model remains unfinished.** DETR + SigLIP2 improved some material scores but produced too many misplaced boxes and more wrong wet/dry recommendations. The stable APK retains the original model and adds the tested fix that prevents an expired classification from returning after camera interruptions. Four-second holds, smooth boxes, bin inspection and Licenses remain. Recognition can still be wrong; Codex has not physically tested a Galaxy M51.

## Downloads and review

- [GarbageClassifier-stable.apk](apks/stable/GarbageClassifier-stable.apk) · [complete matching source](source/stable/GarbageClassifier-stable-source.zip)
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

## Repository guide

| Branch | Purpose |
| --- | --- |
| `main` | Owner-approved final version; untouched by this task. |
| `development` | Verified source, APK and reports. |
| `backup` | Preserved previous work; untouched. |
| `checkpoint` | Progress protection during this task. |

Older branches and APKs remain preserved. `app/` contains Android source and its real bundled detector; `apks/` contains named builds; `source/` contains matching archives; `docs/` contains reports/instructions; `tools/` contains verification scripts and preserved candidate assets. `delivery/` and `preservation/` retain historical work. Only verified duplicate partial benchmark files were removed; complete reports and Git history remain.

The application and bundled original model are **AGPL-3.0**. Distribute corresponding source and notices with the APK. First-time builds need development dependency downloads; the installed scanner works entirely offline.
