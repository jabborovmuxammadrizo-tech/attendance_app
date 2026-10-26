# Attendance

Offline attendance app for teachers (Android, Kotlin + Jetpack Compose + Room).
Take a roll call per group, save it, share the report to Telegram, browse/edit history,
see real statistics, and back everything up to a JSON file.

## Run it

1. Install [Android Studio](https://developer.android.com/studio) (current stable).
2. **File → Open** and choose this folder. Let Gradle sync (it downloads Gradle 9.3.1 itself).
3. Pick an emulator or a phone with USB debugging and press **Run**.

No keys, keystores or `.env` file are needed. Debug builds are signed with the standard
Android debug key automatically.

## Release build (optional)

```
KEYSTORE_PATH=/path/to/upload-key.jks STORE_PASSWORD=... KEY_PASSWORD=... [KEY_ALIAS=upload] \
  ./gradlew assembleRelease
```
Without those variables the release build is simply left unsigned.

## Tests

`./gradlew testDebugUnitTest` runs the JVM tests (group-name matching, Telegram message parsing,
report round-trip, date ordering, CSV escaping).

See `CHANGES.md` for what was fixed and improved.
