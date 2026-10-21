# Candidate Android runtime probe

The preserved ONNX Runtime Android 1.22.0 AAR comes from Maven Central:
https://repo.maven.apache.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/1.22.0/onnxruntime-android-1.22.0.aar

SHA-256: `04a4617a9c797cf49225595e45b5546081cb34c86ac817581141577d3b7dbfe2`.
The upstream MIT license and third-party notices are retained here. It is a test-only dependency; it is not included in the scanner APK. Keeping the already-downloaded artifact avoids repeated downloads during evaluation.

`CandidateRuntimeTest` runs only when explicitly given `candidateModel` and `candidateSize` instrumentation arguments. Place the candidate in the debug application's private files directory using `adb push` followed by `adb shell run-as org.garbageclassifier cp`. It loads the verified model on the Android CPU with two threads, performs one warm-up and three measured calls, records finite outputs and process memory, then closes tensors and sessions. It also measures the original detector in the same process for context. Run no camera activity at the same time.

The report is written to `<candidateModel>.android.json` in that directory. Check its SHA-256 and output values against the export report. Software-emulator timing is not Galaxy M51 timing. PSS is a process snapshot, not a maximum RAM requirement or a measurement of both candidates loaded together.
