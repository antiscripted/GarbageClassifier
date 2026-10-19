# Preserved development snapshot

This branch preserves the existing Garbage Classifier project from the previous development session. Application source, tests, model, Gradle configuration and existing documentation were copied into Git without rewriting working files. No new Android development or model downloads were performed during preservation.

`PROJECT_FILES.json` records every original versionable project file with its size and SHA-256. The original repository README is still its original one-line title. `artifacts/` contains the existing debug APK and instrumentation APK. `evidence/` contains the existing build, unit-test, lint, host-model and emulator-instrumentation results. These are preserved records, not newly executed tests.

## Existing implementation

- Native Kotlin Android project, Gradle 8.11.1 wrapper, Android Gradle Plugin 8.9.2, Android SDK 35, minimum Android API 24.
- CameraX rear-camera preview, camera permission handling, worker-thread analysis and latest-frame backpressure.
- Actual bundled 320 by 320 YOLO waste detector, SHA-256 verified against the pinned Hugging Face manifest. The byte-identical artifact was obtained from the model author's GitHub mirror.
- LiteRT 1.4.1 CPU inference, YOLO decoding, letterbox removal, CameraX preview transforms, uncertain-result handling and temporal confirmation.
- Wet/dry segregation mapping and bin-inspection logic with distinct correct, wrong and uncertain object states.
- Portrait and landscape layouts, model provenance, AGPL license and third-party notices.

## Existing verification evidence

- The most recent saved Gradle build log records a successful debug APK build, JVM tests and lint.
- Saved JVM result XML files contain 53 tests, zero failures, zero errors and zero skipped tests.
- An earlier emulator instrumentation run records 6 passing tests: real image inference, uncertain and empty inputs, sensor rotations and CameraX transforms. Later UI/layout changes were made after that run; the final preserved APK has not had a complete end-to-end retest.
- Host inference and a fixed 60-image material probe were performed. The probe exposed confident material confusions. The model must not be described as reliably accurate across arbitrary waste.
- No physical Samsung Galaxy M51 testing has been performed.

## Unfinished work

- Complete live-camera permission, preview, rotation, background/resume and bin-inspection testing against the final APK. Permission denial was partially checked in the emulator; the full interaction sequence remains incomplete.
- Investigate and improve observed model material confusions. Larger exports were compared, and another pretrained detector was inspected/downloaded outside this project, but no replacement was integrated.
- Implement the Licenses/Close screen approved by the user. License texts are already bundled, but that screen does not yet exist.
- Write the requested Windows installation/build instructions, student/teacher component explanation, explicit Indian segregation-rule documentation, and complete model attribution/distribution guidance. The current README has not yet been expanded.
- Run physical M51/offline checks, evaluate inference speed and box alignment on that phone, and review classification examples with students and the teacher.
- Package the final reviewed delivery and save reusable cloud installation/start instructions when development resumes.

Machine-specific `local.properties`, build caches, generated intermediates, IDE files and signing keys are excluded from Git. The existing checkout and generated files remain on the workspace. A separately verified source-and-evidence archive was created outside the checkout before staging.
