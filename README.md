# Garbage Classifier

Native Kotlin Android app for the Class 9 AI startup competition. Opens the rear camera, runs a bundled waste detector entirely on the phone, and shows a material, a wet/dry recommendation and a bounding box. Bin inspection flags recognizable objects that disagree with the selected bin. No account, server, microphone, uploads, paid API or Internet permission is used.

**Review build, 19 October 2026:** the application and automated checks are implemented. The pretrained model makes confident mistakes on some waste materials. This is a working educational prototype, not a validated general-purpose waste sorter. Physical Galaxy M51 testing and student/teacher acceptance are still required before the 20–21 October review and 25 October competition.

## Get started

- [Windows installation, Android Studio build and APK installation](docs/WINDOWS.md)
- [How the components work](docs/ARCHITECTURE.md)
- [Indian segregation rules and scope](docs/SEGREGATION.md)
- [Model verification, comparisons and limitations](docs/MODEL.md)
- [Tests, errors and device review checklist](docs/VALIDATION.md)
- [Attribution and distribution obligations](docs/LICENSES.md)
- [Reproducing the cloud build](docs/CLOUD.md)

Android 7.0/API 24 or newer, with a rear camera. The universal debug APK supports arm64-v8a, armeabi-v7a and x86_64. Primary intended phone: Samsung Galaxy M51, Snapdragon 730G. This phone has not been tested here.

Open the project folder in Android Studio, select JDK 17, install SDK Platform 35 and Build Tools 35.0.0, then run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. An initial build downloads development dependencies; the installed app works without network access. Gradle 8.11.1 and its checksum are pinned in the included wrapper. No separate Gradle installation is necessary.

## Using the app

Grant the camera permission. Hold one ordinary, well-lit object against a simple background. Confirmed detections show purple boxes, the material and a wet/dry recommendation. Ambiguous, unsupported or missing detections show “Object not clear”; uncertain boxes are gray and dashed. Scores and repeated frames reduce instability but cannot guarantee a correct prediction.

Select “Bin inspection”, then “Wet” or “Dry”. Confirmed mismatches use orange boxes and “Wrong bin”; matches show “Correct bin”. Uncertain objects keep their uncertain state. A visible match cannot establish that everything in a bin is correctly sorted: hidden objects, contamination and hazardous contents are outside the model's capabilities.

“Licenses” opens bundled attribution and legal texts without network access. Camera analysis pauses while that screen is open.

## Project contents

`app/src/main` contains the application and actual 2,836,973-byte model. `app/src/test` contains deterministic decoder, geometry and sorting tests. `app/src/androidTest` tests native inference, camera transforms and UI lifecycle. Test photos are excluded from the main APK. `tools/verify_model.py` independently checks the model with desktop TensorFlow. `docs/validation` contains measured results. `preservation` holds historical snapshots; their APKs and reports are intentionally not overwritten by later development.

The application and bundled detector are distributed under AGPL-3.0. Keep the complete corresponding source, model and notices with any APK distribution. See [LICENSE](LICENSE) and [distribution guidance](docs/LICENSES.md).
