# Sober Counter

A minimal Android app that counts your sober time. No sign-in, no accounts, no network. Everything stays on the device.

- Set your sobriety date and time.
- Neon bars for **Years, Months, Days, Hours**, each filling toward its next rollover.
- Define your own goals (e.g. 30 days, 60 days, 18 months, 1 year) and get a notification when each is reached.

## Build

Requires JDK 17+ and the Android SDK (set `sdk.dir` in `local.properties`).

```
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Built with Kotlin and Jetpack Compose. minSdk 26.
