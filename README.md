# VideoBrowser

VideoBrowser is a lightweight Android browser focused on speed, control of tabs, and clear access to online video content.

## Project status
- Current verified release: v1.0.5
- Target device: Samsung Galaxy S23 Ultra
- Deployment: sideloaded private build, not Google Play
- Status: build produced successfully, APK released, and app installed on the connected S23 device

## Highlights
- Fixed the switch button to rotate orientation exactly like the proven CXWebView implementation.
- Expanded the URL bar to consume available screen width and reorganized the controls into a Chrome-inspired dark layout.
- Kept the screen awake during fullscreen video playback and while the WebView is displaying media content.
- Added an internal ad-redirect recovery mechanism that returns the user to the original video page when a quick redirect to an ad or popup occurs.
- Updated the browser UI and release metadata for the current build.

## Technical baseline
- Kotlin + Jetpack Compose
- Android Gradle Plugin 8.7.3
- Gradle 8.9
- JDK 17
- compileSdk 36
- targetSdk 36
- minSdk 33

## Notes
This repository follows the technical specification prepared for VideoBrowser and implements a functional browser scaffold with a Compose-based UI, tab state, quick navigation, and WebView rendering. The architecture is aligned with the broader concept described in the documentation: a browser engine abstraction, privacy-first web access, and a future path toward stronger ad-blocking and direct-video extraction.

## Release
The current APK release is published to the GitHub Releases page for this repository.

## Build and install
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
export ANDROID_HOME=/home/ivar/Android/Sdk
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```
