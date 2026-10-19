# Latest preservation checkpoint — 19 October 2026

This checkpoint saves the files that were already present when preservation resumed. No application source was rewritten, no tools were installed, no model was downloaded, and no Android build or test was run during this preservation pass.

## Saved contents

The branch contains all 59 current versionable project files: Kotlin application code, portrait and landscape resources, Android manifest, Gradle configuration and wrapper, the actual bundled TFLite model, unit and instrumentation tests with fixtures, model provenance and verification results, and license texts. `PROJECT_FILES.json` lists every file with its size and SHA-256. The original preservation commit and its artifacts remain intact in the branch history and the parent preservation directory.

Six files had newer unsaved changes: MainActivity.kt, CameraController.kt, the two activity layouts, strings.xml, and the new licenses/NOTICE.txt asset. These implement the approved Licenses/Close screen and camera observer cleanup. This checkpoint saves them as found; it does not claim they have completed end-to-end testing.

This directory contains a copy of the latest existing app-debug.apk and its existing build and lint reports. The instrumentation APK is unchanged from `../../artifacts/app-debug-androidTest.apk`, so it is retained there without duplication. `ARTIFACTS.json` records the copied artifacts' hashes.

## Existing evidence, not newly executed verification

- The existing build-resumed.log records successful debug APK, instrumentation APK and lint tasks in 44 seconds.
- The latest existing lint XML contains two performance warnings: DisableBaselineAlignment and NestedWeights. It contains no errors.
- Earlier saved unit-test XML reports contain 53 tests with zero failures, errors or skipped tests: Geometry 12, WasteRules 24, YoloDecoder 17.
- Earlier emulator instrumentation evidence records six passing tests covering real-image inference, uncertain/empty inputs, sensor rotations and CameraX coordinate transforms. That run predates the latest UI changes.
- The actual 320 by 320 model is bundled and its checksum and tensor contract were verified during earlier development. The existing 60-image probe found confident material confusions; this is not a validated general-purpose classifier.
- No physical Samsung Galaxy M51 testing has been performed.

## Remaining work

- Retest the current APK's permission handling, live preview, portrait/landscape behavior, background/resume, bin inspection and license screen.
- Investigate the model's observed material confusions and complete the alternative-model evaluation before claiming reliable recognition.
- Complete Windows build/install instructions, the student/teacher architecture explanation, explicit Indian segregation-rule documentation, and distribution/license guidance, including checking transitive license notices.
- Test offline performance, box alignment and real waste on the M51 with the students and teacher.
- Package the reviewed release and finish reusable cloud environment setup/start instructions.

Ignored machine-specific local.properties, build caches/intermediates, IDE files and signing keys remain in the workspace and are intentionally excluded from Git. The APK, model, source and useful existing verification evidence are preserved. A second local archive was verified byte for byte before this checkpoint: `/workspace/preservation-backups/GarbageClassifier-latest-2026-10-19.tar.gz`, SHA-256 `17dd469eb425c30da1acff7904eacda5a9d08da566050b3a0527e8b9a023da2a`.
