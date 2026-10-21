# Garbage Classifier

A native Kotlin Android project for two Class 9 students and their teacher. The rear camera runs a bundled waste detector entirely offline. The app shows a material and wet/dry recommendation; bin inspection highlights recognizable items in the wrong selected bin. No account, photo upload, microphone, paid API or Internet permission.

**21 October 2026 experimental build:** adds conservative object tracking, a four-second result hold, smoothed boxes and automatic removal resets. The original model and interface are retained. This improves display stability, not the model's underlying accuracy. The owner reports that the original APK works on the Galaxy M51 but confuses some materials; Codex has not physically tested that phone. Competition deadline: **25 October**.

## Download

- [Experimental debug APK](apks/experimental/GarbageClassifier-experimental-debug.apk) — new stability build.
- [Complete matching source ZIP](source/GarbageClassifier-experimental-source.zip).
- [Original working APK](delivery/GarbageClassifier-debug.apk) and [original source ZIP](delivery/GarbageClassifier-source.zip) — unchanged fallback.
- Checksums: [experimental APK](apks/experimental/SHA256SUMS.txt), [experimental source](source/SHA256SUMS.txt), [original delivery](delivery/SHA256SUMS.txt).

On GitHub, open a file and choose **Download raw file**. The two APKs share a package name and signing key; installing one updates the installed app. Keep both files to switch back. Android 7/API 24 or newer and a rear camera are required. The universal APK supports ARM64, ARMv7 and x86_64.

## Build and review

Open the extracted project or clone `development` in Android Studio. Use JDK 17, SDK Platform 35 and Build Tools 35.0.0. Gradle is included:

```powershell
git clone --branch development https://github.com/antiscripted/GarbageClassifier.git
Set-Location GarbageClassifier
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. The first build needs Internet access for development dependencies; the installed application needs no download or connection.

- [Exact Windows setup, build and installation instructions](docs/WINDOWS.md)
- [Main Kotlin files explained for student review](docs/ARCHITECTURE.md)
- [Tracking behavior and phone review checklist](docs/STABILITY.md)
- [Tests, errors and limitations](docs/VALIDATION.md)
- [Completed model comparison and deployment decision](docs/EXPERIMENT.md)
- [Model contract](docs/MODEL.md), [Indian segregation rules](docs/SEGREGATION.md), [licenses](docs/LICENSES.md)

## Use

Grant camera access and hold one well-lit object against a simple background. Purple boxes indicate confirmed objects. Uncertain objects show “Object not clear” and gray dashed boxes. A confirmed card is held briefly, but a missing object's box disappears immediately. Replacement, ambiguity and lifecycle resets can end a hold early; see the stability guide.

Select “Bin inspection”, then “Wet” or “Dry”. Orange boxes and “Wrong bin” flag confirmed mismatches; “Correct bin” applies only to recognizable matches. Hidden contents, contamination and unsupported materials remain outside the detector's capabilities. “Licenses” opens attribution offline and pauses the camera.

## Repository organization

| Branch | Purpose |
| --- | --- |
| `main` | Existing default branch; left unchanged pending owner approval. Switch to `development` for this delivery. |
| `development` | Latest verified experimental source, evidence and APK. |
| `backup` | Saved pre-finishing checkpoint `3eb166f9bb09b9207b4f33bf1a1feefa704cf691`. |
| Previous `finish/`, `experiment/`, `preserve/` branches | Retained historical work; none deleted or rewritten. |

| Folder | Contents |
| --- | --- |
| `app/` | Android source, bundled real model, JVM and Android tests. Kept in standard Android Studio layout. |
| `apks/experimental/` | New experimental APK and checksum. |
| `source/` | Complete experimental source archive and checksum. |
| `delivery/` | Frozen original working APK and source archive. |
| `docs/` | Build instructions, explanations, rules, licenses and limitations. |
| `docs/experiments/2026-10-20/` | Model comparison results, provenance and export evidence. |
| `docs/validation/experimental-2026-10-21/` | New build and test evidence. Earlier validation folders remain historical. |
| `tools/` | Optional developer verification, benchmarking and packaging scripts. |
| `preservation/` | Historical snapshots, retained without overwriting. |

No old branches or useful artifacts were deleted. Source archives intentionally exclude other APKs/archives and local build tools to avoid recursive duplication. Candidate research weights and evaluation datasets are not bundled.

The app and bundled model are distributed under **AGPL-3.0**. Share complete corresponding source and required notices with any APK. This remains an educational prototype: stable output can still be wrong, and physical-phone recognition, responsiveness and student/teacher acceptance require review.
