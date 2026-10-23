SAMBA

A customizable Dynamic Island-style experience for Android.

SAMBA is an Android application focused on bringing a clean, configurable floating island interface to Android devices. Built with Kotlin and Jetpack Compose, the project prioritizes visual consistency, practical customization, and a lightweight user experience.

Overview

SAMBA explores an alternative way to present a compact island interface around the top area of an Android screen. Its appearance is built around a dark UI, subtle gradients, and cyan–purple accents.

The project is under active development. Features and device compatibility depend on the current implementation.

Development

Component| Technology
Language| Kotlin
UI| Jetpack Compose
Build system| Gradle
Platform| Android

Build

Open the project in Android Studio, allow Gradle to sync, and run:

./gradlew assembleDebug

On Windows:

gradlew.bat assembleDebug

The debug APK is normally generated at:

app/build/outputs/apk/debug/app-debug.apk

The project must have a compatible JDK, Android SDK, and Gradle environment configured.

Design Goals

- A consistent interface with clear visual hierarchy.
- Customization that remains usable across screen sizes.
- Purposeful animations and restrained visual effects.
- Minimal, maintainable changes to the existing codebase.
- Accurate documentation of implemented features and known limitations.

Project Status

SAMBA is a work in progress. Not all planned functionality is necessarily available in the current build.

Contributions

Bug reports, technical feedback, and focused improvements are welcome. Changes should preserve existing functionality and remain consistent with the project's architecture.

License

No license has been added yet. Unless a license is provided, the project should not be assumed to be available for redistribution or modification under an open-source license.
