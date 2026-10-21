# Experimental stability APK — 21 October 2026

[Download APK](GarbageClassifier-experimental-debug.apk) · [Complete corresponding source](../../source/GarbageClassifier-experimental-source.zip) · [Windows build/install instructions](../../docs/WINDOWS.md)

This debug build adds conservative tracking, a four-second confirmed-result hold, smoothed boxes and automatic removal resets. It retains the original real offline model, UI, bin-inspection and Licenses screen. Identity ambiguity, replacement and lifecycle resets can clear a hold early. It does not fix the detector's known material errors.

Verification: 75 JVM tests, 7 Python metric tests and 8 API 29 emulator instrumentation tests passed; permission, orientation and bin-control review passed. Lint has zero errors and two non-blocking warnings. Model checksum, permissions, signature, all three ABIs and 16 KB packaging alignment passed. [Full evidence and limitations](../../docs/VALIDATION.md).

The original APK remains in [../../delivery/](../../delivery/). Both APKs share the package name and cloud debug certificate; installing one updates the other. Keep both files for comparison. Codex did not perform physical Galaxy M51 testing. The APK is 24,012,513 bytes; verify its SHA-256 with [SHA256SUMS.txt](SHA256SUMS.txt).

The application and model are AGPL-3.0. Distribute the matching complete source and notices with this APK; see [license obligations](../../docs/LICENSES.md). No new model training or cloud inference was added.
