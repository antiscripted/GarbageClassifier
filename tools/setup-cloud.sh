#!/bin/bash
set -eu
repo_root=${GARBAGE_CLASSIFIER_DIR:-/workspace/GarbageClassifier}
tooling_root=/workspace/tooling
mkdir -p "$tooling_root/downloads" "$tooling_root/android-home" "$tooling_root/gradle-home"
fetch_verified() {
    artifact_url=$1
    artifact_path=$2
    artifact_hash=$3
    if [ ! -f "$artifact_path" ]; then
        curl --fail --location --retry 2 "$artifact_url" --output "$artifact_path.part"
        printf '%s  %s\n' "$artifact_hash" "$artifact_path.part" | sha256sum -c -
        mv "$artifact_path.part" "$artifact_path"
    fi
    printf '%s  %s\n' "$artifact_hash" "$artifact_path" | sha256sum -c -
}
if [ ! -x "$tooling_root/jdk17/bin/javac" ]; then
    jdk_archive="$tooling_root/downloads/OpenJDK17U-jdk_x64_linux_hotspot_17.0.20.1_1.tar.gz"
    fetch_verified 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.20.1%2B1/OpenJDK17U-jdk_x64_linux_hotspot_17.0.20.1_1.tar.gz' "$jdk_archive" '3808d1d15e3ec6bd5b84057fb5d84c33d8a1536a258146bcea2e603fc726e08e'
    tar -xzf "$jdk_archive" -C "$tooling_root"
    ln -sfn "$tooling_root/jdk-17.0.20.1+1" "$tooling_root/jdk17"
fi
export JAVA_HOME="$tooling_root/jdk17"
export ANDROID_HOME="$tooling_root/android-sdk"
export ANDROID_USER_HOME="$tooling_root/android-home"
export GRADLE_USER_HOME="$tooling_root/gradle-home"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
if [ ! -x "$ANDROID_HOME/cmdline-tools/12.0/bin/sdkmanager" ]; then
    sdk_archive="$tooling_root/downloads/commandlinetools-linux-11076708_latest.zip"
    fetch_verified 'https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip' "$sdk_archive" '2d2d50857e4eb553af5a6dc3ad507a17adf43d115264b1afc116f95c92e5e258'
    sdk_unpack=$(mktemp -d "$tooling_root/sdk-unpack.XXXXXX")
    unzip -q "$sdk_archive" -d "$sdk_unpack"
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    mv "$sdk_unpack/cmdline-tools" "$ANDROID_HOME/cmdline-tools/12.0"
    rmdir "$sdk_unpack"
fi
python3 - <<'PY'
import os
from pathlib import Path
from urllib.parse import urlparse
props=Path(os.environ['GRADLE_USER_HOME'])/'gradle.properties'
settings={}
proxy=urlparse(os.environ.get('HTTPS_PROXY',''))
if proxy.hostname:
    for protocol in ['http','https']:
        settings[f'systemProp.{protocol}.proxyHost']=proxy.hostname
        settings[f'systemProp.{protocol}.proxyPort']=str(proxy.port or 80)
trust=Path('/etc/ssl/certs/java/cacerts')
if trust.is_file():
    settings['systemProp.javax.net.ssl.trustStore']=str(trust)
lines=props.read_text().splitlines() if props.exists() else []
lines=[line for line in lines if line.split('=',1)[0] not in settings]
lines.extend(f'{key}={value}' for key,value in settings.items())
props.write_text('\n'.join(lines)+'\n')
PY
sdk_manager="$ANDROID_HOME/cmdline-tools/12.0/bin/sdkmanager"
if [ ! -f "$ANDROID_HOME/platforms/android-35/android.jar" ] || [ ! -x "$ANDROID_HOME/build-tools/35.0.0/aapt" ] || [ ! -x "$ANDROID_HOME/platform-tools/adb" ]; then
    yes | "$sdk_manager" --sdk_root="$ANDROID_HOME" --licenses
    "$sdk_manager" --sdk_root="$ANDROID_HOME" 'platforms;android-35' 'build-tools;35.0.0' 'platform-tools'
fi
cat > "$tooling_root/android-env.sh" <<'ENV'
export JAVA_HOME=/workspace/tooling/jdk17
export ANDROID_HOME=/workspace/tooling/android-sdk
export ANDROID_USER_HOME=/workspace/tooling/android-home
export GRADLE_USER_HOME=/workspace/tooling/gradle-home
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
ENV
cd "$repo_root"
"$JAVA_HOME/bin/javac" -version
./gradlew --version
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug --console plain
