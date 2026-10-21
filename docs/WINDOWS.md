# Windows build and installation

These commands are for Windows 10/11 PowerShell. The Android build has been run on Linux in the cloud; these Windows steps have been reviewed but not executed on a Windows computer here.

## Install the supplied APK

1. Copy the supplied `GarbageClassifier-experimental-debug.apk` to the Galaxy M51 using a USB cable, or download it from the project's `apks/experimental/` folder (the original remains in `delivery/`).
2. Open it in Samsung My Files. If Android requests it, enable “Allow from this source” for My Files, then install. The supplied APK is a development/debug build, not a Play Store release.
3. Launch “Garbage Classifier” and allow camera access. No other runtime permission is needed. Android's camera permission dialog may mention recording video; the app only analyzes frames and does not save video.
4. Turn on airplane mode and turn Wi-Fi off. Reopen the app and try the scanner. The model is already inside the APK; there is no first-launch model download.
5. Review the device checklist in VALIDATION.md. Do not use hazardous, sharp or sanitary waste as classroom test objects.

The original and experimental APKs share the package name and cloud debug signing key. Installing with `adb install -r` updates the installed version; it does not overwrite either saved APK file. To return to the original, install `delivery/GarbageClassifier-debug.apk` with the same command. No uninstall is normally needed between the supplied APKs.

If Android reports that an existing installation conflicts, it may be signed by a different debug key. Uninstall the old `org.garbageclassifier` app and reinstall. This removes its permission history and UI settings; the app does not store photos or accounts. A version built on another computer usually has a different debug signing key.

## Build from source in Android Studio

1. Install Android Studio Meerkat 2024.3.1 or a newer version compatible with Android Gradle Plugin 8.9.2, from https://developer.android.com/studio . Keep the project's pinned plugin and wrapper versions when opening it; an automatic upgrade is not required.
2. Install a full JDK 17, for example Eclipse Temurin 17 from https://adoptium.net/temurin/releases/?version=17 . A Java runtime without `javac` is insufficient.
3. Extract the complete supplied source ZIP to a short path such as `C:\Projects\GarbageClassifier`. Open that directory, which contains `settings.gradle.kts`, in Android Studio. Alternatively clone the active development branch:

   ```powershell
   git clone --branch development https://github.com/antiscripted/GarbageClassifier.git C:\Projects\GarbageClassifier
   ```

4. In Android Studio, open Settings → Build, Execution, Deployment → Build Tools → Gradle. Set Gradle JDK to the installed JDK 17 directory. Allow Gradle to use the included wrapper.
5. Open Tools → SDK Manager. Install **Android SDK Platform 35**, **Android SDK Build-Tools 35.0.0**, **Android SDK Platform-Tools**, and **Android SDK Command-line Tools**. Accept their license terms. Android Studio writes the machine-specific `local.properties`; it must not be committed or copied from the cloud.
6. Click Sync Project with Gradle Files. The first sync needs Internet access to Google Maven, Maven Central and the Gradle distribution servers. This is build-time access only.
7. Select the `app` configuration, connect the phone and click Run, or build using PowerShell below.

## Exact PowerShell build commands

Replace the example JDK directory with the actual directory containing `bin\javac.exe`. Do not include `\bin` in JAVA_HOME.

```powershell
Set-Location C:\Projects\GarbageClassifier
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.1+1'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
& "$env:JAVA_HOME\bin\java.exe" -version
& "$env:JAVA_HOME\bin\javac.exe" -version
.\gradlew.bat --version
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
if ($LASTEXITCODE -ne 0) { throw 'Android verification failed' }
```

Outputs:

- App APK: `app\build\outputs\apk\debug\app-debug.apk`
- Test APK: `app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk`
- Unit results: `app\build\reports\tests\testDebugUnitTest\index.html`
- Lint results: `app\build\reports\lint-results-debug.html`

To verify the bundled model:

```powershell
(Get-FileHash .\app\src\main\assets\trash_detector.tflite -Algorithm SHA256).Hash.ToLower()
```

Expected: `9c258ac8a0344cd07deb28ddcedb096e5bd30c092b807ff7e442f40279d8f54d`.

## USB deployment and Android tests

On the M51, open Settings → About phone → Software information; tap Build number seven times. Enable USB debugging in Developer options. Connect using a data-capable cable, unlock the phone and accept its RSA debugging prompt. Install Samsung's Android USB driver if Windows does not detect the phone.

```powershell
$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"
& $adb devices
& $adb install -r .\app\build\outputs\apk\debug\app-debug.apk
& $adb shell am start -n org.garbageclassifier/.MainActivity
.\gradlew.bat :app:connectedDebugAndroidTest
if ($LASTEXITCODE -ne 0) { throw 'Android device tests failed' }
```

Connect one target device for this command. An emulator with a rear camera also works; a camera-less emulator cannot pass the live-preview tests. Connected tests grant the app's camera permission and exercise recreation and background/resume. These are development tests, not bundled classroom features.

## Troubleshooting

- “SDK location not found”: finish SDK Manager installation and open/sync the project in Android Studio, or set ANDROID_HOME as above.
- Java compilation unavailable: point JAVA_HOME and Gradle JDK at a full JDK 17. Check `javac -version`.
- Dependency download failure: check the network/proxy and retry the failed operation. Keep TLS and artifact checksums enabled; do not download unverified replacements.
- Device “unauthorized”: unlock it and accept the USB debugging prompt. “No devices”: check the cable, driver and USB mode.
- Permission denied: use “Allow camera”; after permanent denial use “Open settings”, grant Camera, and return.
- “Camera unavailable”: close another camera app, then “Try again”. A rear camera is required.
- “Object not clear”: use more light, a closer object and a simple background. This can also be the correct outcome for unsupported material. Do not force a category by lowering thresholds for a presentation.
- Installation blocked by school policy: use a teacher-approved test phone. No paid API key or account is required.
