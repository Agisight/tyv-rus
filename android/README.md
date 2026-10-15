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

## Android publication

The Android application ID is `ru.tyv.sostuk`. Publish it as a new Google Play app with Play App Signing enabled. It is a separate installation from the legacy `ru.tuvlin.tyv_rus_android` app; users must install it separately.

The shared Kotlin source namespace and Compose resource package remain `ru.tuvlin.tyv_rus_android`; they do not determine the Android application ID.

## Android release signing

Copy `signing.properties.example` to `signing.properties` in the project root and fill in your local upload key settings. Keep the keystore and passwords outside Git. The same `UPLOAD_*` settings can also be supplied through Gradle properties or environment variables.

Debug builds use the default Android debug key. Without upload key settings, release builds are unsigned; configure your upload key before uploading a release to Google Play.

## Android dictionary

The bundled SQLite file contains the original `dictionary(word_phrase, html_text)` data. Android streams it from the generated Compose resource asset into its own `noBackupFilesDir` and prepares a normalized, indexed search column once. All 16,627 articles keep their original SQLite row IDs, including homonyms. The original empty heading of article 1 is recovered as `а` from its text and the legacy word list.

Increment `DICTIONARY_REVISION` in `Dictionary.android.kt` whenever the bundled data or preparation schema changes. Each revision uses a new cache filename, so an old installed/backup database cannot override a newer dictionary. The source database is never modified at runtime. All database work runs on `Dispatchers.IO`.

Run the Android database regression tests on an emulator with `./gradlew :composeApp:connectedDebugAndroidTest`.

## Dictionary and questions (Android 2.1)

The dictionary list shows meanings and source examples. Tuvan headings use the normalized prefix index; Russian meanings use an FTS4 Unicode index prepared locally from the original articles. Russian results show the matching meaning, accept word prefixes and treat `ё`/`е` alike. Results are capped at 120; headings retain all 16,627 articles and separate homonyms.

«Спросить» parses common Russian requests for a translation, meaning or examples and returns up to four dictionary articles. It uses rules and dictionary data, not a language model or generated translations. Examples come from the source markup. Conversation history stays in memory, survives article navigation, and can be cleared.

The optional Google Translate button opens an external browser with the requested text and direction. The application has no network permission, analytics or translation API dependency. Google Translate requires an internet connection and is governed by Google's privacy policy. In-app privacy information is available from the information button.

Validated target: Android. The existing iOS scaffolding still requires its own dictionary reader and platform validation.

Version 2.1.1 expands Russian request wording in both translation directions, supports follow-up requests such as «А примеры?» using the last matched heading, and offers explicitly labelled spelling suggestions for missing Tuvan words. Suggestions do not silently replace the requested word.

Version 2.1.2 repairs misspelled request words outside quoted terms, with one-edit suggestions for short dictionary words and up to two edits for longer ones. Russian vocabulary is derived on demand from the prepared dictionary, Tuvan vocabulary from headings. Both chat and regular search label suggestions; chat uses a unique correction with a notice and keeps ambiguous alternatives separate. All spelling work runs locally off the UI thread, and fuzzy work is cancellable.

Version 2.1.3 stores examples as structured Tuvan/Russian pairs derived from the original purple HTML spans. Prepared database revision 3 refreshes older plain-text example caches; the bundled source database remains unchanged. Both conversation examples and list previews label the two languages separately.
