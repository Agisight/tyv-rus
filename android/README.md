This is a Kotlin Multiplatform project targeting Android, iOS.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.


Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

## Android release signing

Copy `signing.properties.example` to `signing.properties` in the project root and fill in your local upload key settings. Keep the keystore and passwords outside Git. The same `UPLOAD_*` settings can also be supplied through Gradle properties or environment variables.

Debug builds use the default Android debug key. Without upload key settings, release builds are unsigned; configure your upload key before uploading a release to Google Play.

## Android dictionary

The bundled SQLite file contains the original `dictionary(word_phrase, html_text)` data. Android streams it from the generated Compose resource asset into its own `noBackupFilesDir` and prepares a normalized, indexed search column once. All 16,627 articles keep their original SQLite row IDs, including homonyms. The original empty heading of article 1 is recovered as `а` from its text and the legacy word list.

Increment `DICTIONARY_REVISION` in `Dictionary.android.kt` whenever the bundled data or preparation schema changes. Each revision uses a new cache filename, so an old installed/backup database cannot override a newer dictionary. The source database is never modified at runtime. All database work runs on `Dispatchers.IO`.

Run the Android database regression tests on an emulator with `./gradlew :composeApp:connectedDebugAndroidTest`.
