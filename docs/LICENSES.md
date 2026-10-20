# Attribution and distribution

## Application and model

The application is distributed under GNU Affero General Public License version 3. The complete text is in root `LICENSE` and in the APK at `assets/licenses/MODEL-AND-APP-AGPL-3.0.txt`. The approved Licenses screen displays it offline.

Detector: **YOLO-TrashDet-Pruned**, alexNova / Alejandro Casanova, based on Ultralytics YOLOv8. Source: https://huggingface.co/alexNova/YOLO-TrashDet-Pruned at revision `fbb540cd6b415c08387f4c305d153a3c358ea78b`. Its card and export metadata declare AGPL-3.0. The bundled file is `TFLite Format/YOLOv8n-TrashDet-10-imgsz320-int8.tflite`; its weights are unmodified.

The byte-identical download mirror is the author's [Android-Trash-Detection-with-YOLO repository](https://github.com/Alejandro-Casanova/Android-Trash-Detection-with-YOLO/tree/14a626f9654603237c86a8064805f50c4a659921). The author's card links the model/pruning work. Retain upstream attribution and links when redistributing; inspect upstream training/export sources if modifying or re-exporting the model. The app's camera pipeline, decoding and UI were implemented in this project; the model was pretrained and was not trained by the students or this project.

When distributing a built APK:

1. Supply the complete corresponding source for that APK, including Kotlin/XML, build files, Gradle wrapper, actual model, notices and build/install instructions. The supplied source ZIP is intended to accompany the APK. Keep both available together in the same download location.
2. Keep the AGPL license, copyright/attribution and warranty notices intact. Mark later changes and their dates in version history and documentation.
3. Distribute covered modifications under the applicable AGPL terms. Recipients retain the license's rights to inspect, modify and redistribute. Do not describe the model as proprietary or exclusively owned by this project.
4. If adapting the program for remote network interaction, review AGPL section 13's corresponding-source obligations. This app performs inference locally and offers no network service.
5. Keep third-party license texts and notices with the software. A debug signing key is not included in Git or the source archive; users can build and install their own independently signed APK.

## Dependencies and notices

| Component | License / notice location |
| --- | --- |
| AndroidX CameraX, Activity, Core, Lifecycle and supporting AndroidX libraries | Apache-2.0; Android Open Source Project notices in `assets/licenses/NOTICE.txt` and Apache text in `ANDROIDX-LITERT-APACHE-2.0.txt`. |
| Google AI Edge LiteRT 1.4.1 | Apache-2.0 plus accompanying notices; `assets/licenses/LITERT-NOTICES.txt` is copied unmodified from the actual 1.4.1 Maven AAR's `LICENSE`, including its bundled third-party notice. |
| Kotlin standard library | Apache-2.0, JetBrains and Kotlin contributors; Apache text included. |
| Gradle wrapper | Apache-2.0; distribution is obtained by the checksum-pinned wrapper. Apache text in `docs/licenses/APACHE-2.0.txt`. |
| JUnit and AndroidX test tools | Build/test dependencies only, not in the application APK. JUnit 4 uses EPL-1.0 (copied from the 4.13.2 JAR to `docs/licenses/JUNIT-EPL-1.0.txt`); AndroidX test uses Apache-2.0. Their packages retain their supplied licenses. JUnit source is available at https://github.com/junit-team/junit4/tree/r4.13.2 . |
| TrashNet test photographs | MIT, Gary Thung 2017. Full notice in `docs/licenses/TRASHNET-MIT.txt`. Files are in the instrumentation test APK/source only, not the main APK. |

The complete initial model card and metadata are retained in `docs/model`. Test-image provenance and SHA-256 values are in `docs/validation/fixture-sources.json`. Model alternatives were evaluated using local development tools; their weights and Python dependencies are not bundled in the application or source ZIP.

This file explains how this project is packaged; the full license texts control the legal terms. The school should retain the source distribution when sharing the APK publicly.
