# Cloud build environment

The existing checkout is `/workspace/GarbageClassifier`. Use it directly; do not create a worktree unless the user explicitly asks. Preservation branches and their archived outputs remain available. The completed work is on `finish/review-build-2026-10-20`.

## Build setup

```bash
cd /workspace/GarbageClassifier
bash tools/setup-cloud.sh
source /workspace/tooling/android-env.sh
```

The script prepares full Temurin JDK 17.0.20.1+1, Android command-line tools 12.0, Platform 35, Build Tools 35.0.0 and platform-tools under `/workspace/tooling`, and runs the wrapper's build/unit/lint checks. It pins and checks download SHA-256 values. The wrapper independently pins Gradle 8.11.1 and its distribution SHA-256. Existing valid installations are reused. SDK packages require acceptance of the Android SDK license terms; the setup script supplies that acceptance for the authorized build environment.

The script was replayed successfully in the current prepared instance. A clean new-machine restoration has not been tested. The original downloads and installations were performed earlier in this workspace. The command-line tools ZIP was checked against Google's repository manifest and its SHA-256 is pinned in the script.

Gradle's proxy host/port is derived from the existing HTTPS_PROXY environment variable. No token or proxy password is copied into the repository or saved instructions. The platform's Java CA trust store is used when available, preserving TLS validation. A full JDK is required; the initially supplied Java runtime did not contain a compiler.

`local.properties`, signing keys, SDKs, JDKs, caches and Python environments are intentionally excluded from source control. The APK uses a development signing key; keep that local key if you need compatible in-place updates. Never commit it.

## Optional software emulator

An API 29 x86_64 emulator named `GarbageClassifier29` and its system image have been installed in the current instance. There is no `/dev/kvm`, so it uses software emulation and its timing is not representative of a phone. ADB's user directory must be writable; the initial restricted environment needed `/home/agent/.android` prepared before ADB could create its files. Do not change HOME or copy any authentication files to work around that.

If the emulator is not already running, use the retained AVD:

```bash
source /workspace/tooling/android-env.sh
export ANDROID_EMULATOR_HOME=/workspace/tooling/android-home
export ANDROID_AVD_HOME=/workspace/tooling/android-home/avd
export XDG_RUNTIME_DIR=/workspace/tooling/runtime
mkdir -p "$XDG_RUNTIME_DIR"
nohup "$ANDROID_HOME/emulator/emulator" -avd GarbageClassifier29 \
  -no-window -no-audio -no-boot-anim -no-snapshot -no-metrics \
  -gpu swiftshader_indirect -accel off -cores 2 -memory 2048 \
  -camera-back emulated -camera-front none \
  > /workspace/tooling/emulator.log 2>&1 &
adb devices
adb shell getprop sys.boot_completed
```

Wait for `sys.boot_completed` to return `1`, polling in short intervals and inspecting the emulator log on failure. A process ID alone is not readiness. Do not start another emulator with the same AVD while one is running. Live processes do not survive environment publication/restoration.

The normal developer test command is:

```bash
cd /workspace/GarbageClassifier
./gradlew :app:connectedDebugAndroidTest --console plain
```

For direct instrumentation, install both APKs and run:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell svc wifi disable
adb shell svc data disable
adb shell am instrument -w -r org.garbageclassifier.test/androidx.test.runner.AndroidJUnitRunner
```

ADB can return exit zero even when an instrumentation assertion fails. Check the instrumentation summary and failure statuses; do not infer a passing run from ADB's exit status alone. The UI tests need a working rear camera on their target. The software emulator supplies a synthetic moving scene, not actual waste or a physical phone feed.

## Model tooling

The existing `/workspace/tooling/model-venv` contains TensorFlow CPU 2.17.1, NumPy 1.26.4 and Pillow 11.1.0. It is only for independent model verification:

```bash
/workspace/tooling/model-venv/bin/python tools/verify_model.py --report /tmp/model-report.json
```

The optional comparison environment and downloaded alternative checkpoints live outside the checkout. They are not required to build or run the app. The bundled model means no model download is needed during a normal Gradle build.

## Saved configuration versus current runtime

The setup script and startup guidance are saved through the onboarding configuration draft. Saved instructions do not run automatically in the current shell, and saving does not publish the environment. Review/save the draft in environment settings, then publish to retain the prepared environment for future tasks. The draft also adds official Indian reference sites and the GitHub API domain while preserving the existing Hugging Face rules. Those additions do not establish working access until applied and tested; the official-site and PR API requests were blocked during development.
