# RACCOON AD STUDIO — Local MVP

An early native Android MVP for creating a simple multi-image advertising video locally. Arabic-first UI, RTL layout, local project persistence, multi-image import, scene reordering/removal, per-scene duration, and MP4 export through AndroidX Media3 Transformer.

## Honest scope

This is an MVP, not the complete product brief. It does **not** include generative video, a real timeline with text/audio overlays, crossfades, client CRM, brand kits, undo/redo, or production-grade preview. The exported video currently sequences the selected image scenes for their configured durations; it does not claim to create AI footage. Media3's supported behavior can vary by Android device and codec.

## Build locally

1. Install Android Studio with Android SDK 36 and JDK 17.
2. Open this folder in Android Studio.
3. Allow Gradle sync to finish.
4. Run the `app` configuration on a device/emulator, or run `gradle assembleDebug` from a terminal where Gradle is installed.
5. APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Build APK with GitHub Actions

1. Create a new empty repository on GitHub.
2. Upload/push the contents of this folder to the repository root (the `.github/workflows` folder must be included).
3. Open the repository's **Actions** tab and enable Actions if prompted.
4. Select **Build Android Debug APK** and click **Run workflow**, or push a commit to `main`/`master`.
5. Wait for the workflow to finish. A green check means the build job succeeded.
6. Open the completed workflow run and download the `raccoon-ad-studio-debug-apk` artifact.
7. Extract the downloaded ZIP to get `app-debug.apk`, then transfer it to your Android phone and install it. Android may ask you to allow installs from that source.

GitHub-hosted Actions minutes and artifact retention are subject to your account/repository plan and current GitHub limits. No API key is needed for this MVP.

## Current verification status

The authoring environment used to generate this source did not have Android SDK or Gradle installed, so an APK could not be built or runtime-tested here. The GitHub Actions workflow is provided to perform the first real build. Fix any build errors surfaced by that run before treating the APK as verified.
