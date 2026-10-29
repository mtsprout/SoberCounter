# Releasing

Release signing reads these keys from the git-ignored `local.properties`:

```
RELEASE_STORE_FILE=...
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```

Build the Play bundle:

```
./gradlew bundleRelease
```

Upload `app/build/outputs/bundle/release/app-release.aab`. Before each upload bump `versionCode` (and `versionName`) in `app/build.gradle.kts`; Play rejects a versionCode it has already seen.

Store listing text and graphics live in `store/`.

Run the unit tests with `./gradlew testDebugUnitTest`.
