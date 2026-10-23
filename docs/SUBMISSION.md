# Approved submission

Current downloads: [GarbageClassifier.apk](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/main/apks/GarbageClassifier.apk) and [GarbageClassifier-source.zip](https://github.com/antiscripted/GarbageClassifier/raw/refs/heads/main/source/GarbageClassifier-source.zip).

The owner approved promotion of candidate `20ea77c2a74e8e0aa23a82d3df9be470d459cbe8` to `main`. Publication changes only repository organization, delivery names, source-archive exclusions and documentation. Application/build code, model weights, runtime, rectangles, single-object Bin Inspection and component Licenses are unchanged. Git history is preserved without force-pushing; `checkpoint`, `development`, `backup` and historical branches remain intact.

## Verification

- Reused the candidate's **91 passing JVM tests**, successful APK build and lint result of **zero errors/two existing warnings**. No costly test, benchmark or emulator inference was repeated.
- Rechecked APK ZIP integrity, v2 signing and the original certificate; expected model/runtime bytes; full license catalog/text accessibility; offline permissions; packaged ARM64, ARMv7 and x86_64 libraries; API 24 minimum/target 35; ZIP and 64-bit ELF alignment. See [APK inspection](submission-apk-inspection.json).
- Published APK SHA-256: `24b32943ac1774c83660167bf78fe809b85e2fb9292485a9640aa3957f64acee`, identical to the reviewed candidate.
- SigLIP2 SHA-256 remains `63802ff39c00c52663846e4bf7c0dfe511e4e4242398a24240bce733cdbdf366`. The original detector is retained for presence/localization; no classifier/model change occurred.
- The complete current source ZIP contains the Android project, wrapper, actual models/native runtime, license texts, tests, instructions and evidence. Packaging verifies every archived file against its SHA-256. `SOURCE_MANIFEST.json` binds the archive contents to the APK checksum. [Current APK checksum](../apks/SHA256SUMS.txt) · [Current source checksum](../source/SHA256SUMS.txt).
- The two previous new-model deliveries, their source archives/checksums and reviewed candidate are preserved under [backup/](../backup/README.md). An older experimental source archive/checksum was also moved there to leave one obvious current source ZIP. Git blob comparisons confirm all 14 moved files are byte-identical; [preservation record](submission-preservation.json).
- The verified stable APK/source and original old-model APK remain available unchanged. The `backup/` directory does not modify the `backup` Git branch.

No new physical-phone testing occurred. The owner's Galaxy M51 report concerns the preceding CPU-optimized APK. Final rectangles, license navigation and other phone behavior retain the candidate's documented verification limitations; publication does not establish new device coverage or performance. See [candidate evidence](final-candidate/README.md) and [model evidence](experiments/int4-cpu/README.md).

Windows installation/build commands and project navigation are in the [main README](../README.md). The APK remains a debug-signed school submission, signed with the same certificate as the previous supplied APKs. AGPL-3.0 corresponding-source obligations and all Apache/MIT/third-party license texts, copyrights and attributions remain preserved offline. Distribute the matching source and notices with the APK.
