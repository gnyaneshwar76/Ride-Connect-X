# After the laptop reset — what to install, in order

Written 23 September 2026, before the reset. Follow this top to bottom; each step
only needs the ones above it.

**Your work is safe in two places:**

1. **GitHub** — `https://github.com/gnyaneshwar76/Ride-Connect-X`, branch
   `security-hardening-2026-09` (all the code and docs).
2. **`E:\RideConnetX\backup\`** — everything that is *not* on GitHub: Claude's memory,
   chat history, skills, settings, the security-audit reports, the Suzuki and Maps
   APKs, the cluster photos and ride recordings, the JADX decompile, and the original
   app images. See `backup\RESTORE.md` for where each folder goes back to.

> **If E: survives the reset, you lose nothing.** Only the C: drive items were at
> risk, and they are now copied into `E:\RideConnetX\backup\reference\`.

---

## 1. Install these, in this order

| # | Install | Why this project needs it | Notes |
|---|---|---|---|
| 1 | **Git for Windows** | Cloning, and Claude's Bash tool runs on its shell | Sign in to GitHub afterwards |
| 2 | **JDK 17** (Temurin, or the one bundled with Android Studio) | Gradle builds | The bundled one is fine |
| 3 | **Android Studio** (latest) + **Android SDK** | Builds, emulator, and `adb` | During setup tick **Android SDK Platform-Tools** |
| 4 | **Node.js LTS** | Firebase CLI and helper scripts | |
| 5 | `npm install -g firebase-tools` | Deploying the public site and Firestore rules | Then `firebase login` — the old login is gone |
| 6 | **Claude Code** | How we work | Then open `E:\RideConnetX` |
| 7 | **Suzuki Ride Connect** (official app, Play Store) | Reference for packet captures | Keep it **disabled** while testing ours; it takes the BLE link exclusively |
| 8 | **Lockito** (Play Store) | Fake GPS rides from the desk, no riding | Set it as the mock-location app in Developer options **by hand** — OnePlus blocks `appops` from adb |

**On the phone (OnePlus Nord CE 3 5G):** turn on Developer options → USB debugging,
and after installing our app grant it notification access.

### Paths to remember

```
adb:     %LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe      (not on PATH)
launch:  adb shell am start -n com.gnyaneshwar.rideconnectx/com.eshwar.rideconnectx.MainActivity
```

The applicationId (`com.gnyaneshwar.rideconnectx`) differs from the Kotlin namespace
(`com.eshwar.rideconnectx`) **on purpose**. Firebase is registered against the
applicationId.

---

## 2. Get the code back

```bash
git clone https://github.com/gnyaneshwar76/Ride-Connect-X.git E:\RideConnetX
cd E:\RideConnetX
git checkout main
```

If `E:\RideConnetX` survived the reset, skip this — it is already there and already
pushed.

## 3. Put back the two files that are deliberately NOT on GitHub

1. **`frontend\app\google-services.json`** — from the backup if you have it, otherwise download
   it again: Firebase Console → Project settings → Your apps → `rideconnectx-89528dd3`.
   Without it the build fails.
2. **`Test-Tracker.md`** — the proof list. Local only, never GitHub.

Then restore Claude's memory and history exactly as `backup\RESTORE.md` describes. The
folder name `E--RideConnetX` must match character for character, or Claude will not
connect its memory to this project.

## 4. Check it all works

```bash
cd E:\RideConnetX\frontend
.\gradlew.bat testDebugUnitTest assembleDebug
```

Expect **all tests green** (79/79 as of 22 Sep) and an APK at
`frontend\app\build\outputs\apk\debug\app-debug.apk`. If Gradle fails oddly right after a fresh
install, run `.\gradlew.bat clean` first — a half-finished build can package an APK
that installs and then crashes.

## 5. Start the next chat with

```
Read docs/status/HANDOFF.md, then docs/status/PROJECT-STATUS.md, then continue.
```

---

## What we were in the middle of

- **Turn arrows — first priority, and the only thing blocking sign-off.** Needs the
  scooter, engine running, about 10 minutes: hold codes 1 and 4, check the arrow
  clears when navigation ends, confirm the heartbeat fires once per beat, and switch
  the scooter off and on mid-session to prove writes resume without an app restart.
- **Security:** two audit runs done, all run-1 leads fixed. Five leads still open, led
  by *no Room table is scoped to the signed-in account*. Reports are in
  `backup\security-audit-runs\RideConnetX\`.
- **On hold, and yours to do:** the release keystore (create it, put the passwords in
  `keystore.properties` — git-ignored), and Firebase App Check.
- **Not done on purpose:** R8 / `isMinifyEnabled` — the right fix for debug logs
  surviving into release, but obfuscation breaks at runtime rather than at compile
  time, so it needs an on-device smoke test first.
- **After the turns:** weather on the cluster, find-my-parked-scooter, the alert lamps.
  Mileage is dropped for good — the scooter already shows it on Trip A/B.

## Things that have cost real time before — don't repeat them

- The emulator has **no Bluetooth radio**. Anything BLE must be tested on the phone.
- Only one app can hold the scooter. Force-stopping the official app is not enough, it
  respawns — disable it. **Never uninstall it; you need it for real journeys.**
- Shut the emulator down before a clean build. Both at once leaves ~0.3 GB free and the
  build gets OOM-killed. `.\gradlew.bat --stop` does not stop the Kotlin daemon.
- `wm size 2560x1600` + `wm density 276` checks the tablet layout without a tablet AVD.
  Reset both afterwards.
