# Sprint plan — testing, evidence, fixes (26–29 Sep 2026)

The one page both the local session and the cloud session work from. Update it
as things finish; the newest state wins.

## Deadlines

| When | What |
|---|---|
| **Sat 27 Sep, evening** | Owner finishes all phone and scooter testing; evidence collected |
| **Mon 29 Sep, morning** | Every fix in the queue below done, tested, pushed, reported |

## How the work is split

1. **Testing (owner, local session).** The owner runs every test by hand on the
   phone and scooter. The local session only records results in the private
   tracker (`Test-Tracker.md`, never on GitHub) and publishes status to the
   `evidence` branch. **No fixing while testing is under way.**
2. **Hand-off.** When the owner says *"fix it, I'm going"*, the local session
   copies every new failure into **Fix queue** below, pushes this file, and
   starts a cloud session.
3. **Fixing (cloud session).** Works on branch `fixes-2026-09-24`, top of the
   queue down. For each item: fix at the root cause, build, run unit tests,
   commit with the test ID in the message, tick it here. Push after every item
   so nothing is lost if the session stops.
4. **Report.** The cloud session writes **Cloud report** at the bottom and
   pushes. The local session reads it, installs the build on the phone, and
   the owner re-tests.

### Parallel fixing (from 26 Sep)

Fixes don't wait for testing to end. **One** cloud session does all of them,
on **one** temporary branch, while the owner keeps testing:

- Branch: `temp/sprint-fixes` (from `fixes-2026-09-24`). All fix commits go
  here, one commit per test ID. Deleted after it is folded back in.
- New failures are sent to the same cloud session as follow-up messages.
- Before building, create a **placeholder** `app/google-services.json`
  (package `com.gnyaneshwar.rideconnectx`, dummy ids) so Gradle compiles.
  Never commit it. Real sign-in is checked on the phone, not in the cloud.
- Per item: root-cause fix → `./gradlew testDebugUnitTest assembleDebug` →
  commit with the test ID → push → tick it in this file → report cause,
  change, files, test result, and what the owner must check on the phone.
- The local session pulls `temp/sprint-fixes`, installs on the phone, and the
  owner re-tests.

## Budget (snapshot 26 Sep, 15:50)

| Pool | Used | Resets / expires |
|---|---|---|
| Pro 5-hour limit | 7% | rolling, 5 h |
| Pro weekly (all models) | 8% | Mon 29 Sep, 04:30 |
| Cloud session credits | $0 of $100 | expire 5 Nov 2026 |

Local sessions draw on the Pro limits; cloud sessions draw on the $100 credits.
If the Pro limit runs out mid-work, continue in a cloud session.

## Build facts the cloud session needs

- Layout: Android project at the repository root (`app/`, `./gradlew`).
- `app/google-services.json` is **not** in git. Without it the build fails at
  `processDebugGoogleServices`; unit tests of pure logic can still be run with
  `./gradlew :app:testDebugUnitTest` only after it exists. If it is missing in
  the cloud, fix and commit code, and mark items **needs device build** here.
- Tests: `./gradlew testDebugUnitTest` — 84/84 pass at `d832438`.
- Never push to `main`, never merge, never touch the `evidence` branch.

## Fix queue

Status: `[ ]` open · `[x]` fixed, awaiting re-test · `[v]` re-tested OK on the phone

### Already fixed on `fixes-2026-09-24` — awaiting owner re-test
- [x] G4 guest data lost on Google sign-in; account chooser showed 5 accounts
- [x] G6 returning account re-showed setup; merge screen only for guest → Google
- [x] G2 email accounts had no verification
- [x] C8 no warning when notification access is off (Notifications page only)
- [x] H5 / R1 location share slow, repeat taps, "No thanks" left it stuck
- [x] D9 approximate location was shared instead of refused

### Open — to fix after testing
- [ ] A3 auto-reconnect showed pairing after reinstall (expected: backup is off — confirm with force-stop test) — code reviewed, no change; owner force-stop check below
- [ ] B4 code 36 arrow direction unconfirmed (hardware check, not code)
- [ ] I6 database upgrade — columns not verified
- [ ] N1 permissions are all requested at once — must be one by one (guest and sign-in setup)
- [ ] N2 guest name typed on the guest screen does not prefill Create Profile's Name (nickname must stay empty)
- [ ] N3 Back during Create Profile exits, and reopening skips setup into a half-made account — must resume setup, never skip it
- [ ] N4 a previous person's local name (guest "rocky bhai") prefilled another Google account's setup — local profile must be cleared/scoped per account
- [ ] N5 guest signing into an account that already has a profile: no prompt — must ask "add your guest data to this account?" or let them pick another account
- [ ] N6 an account whose profile exists in Firestore was asked to create a profile again after sign-out/sign-in — restore must always run and mark setup done
- [ ] AUDIT after the above: walk every sign-in / sign-out / guest / setup path end to end and fix basic-logic gaps (stale state, back-navigation, half-finished setup)
- [ ] _new failures from 26–27 Sep testing go here_

### Waiting on an owner decision
- [ ] R2 live search of Suzuki service centres (Google Places) — needs billing enabled by the owner
- [ ] Welcome + "new sign-in" security emails — needs Cloud Functions (Blaze) + an email service
- [ ] Publish the `profile.createdAt` Firestore rule — changes the live database, ask first
- [ ] Separate-branch repository layout (`frontend`, `backend`, `docs`, `tools`, `temporary`) — owner to confirm

### Security hardening still open
- [ ] Firebase Console: turn on **email enumeration protection** (Authentication → Settings)
- [ ] Release signing key → then App Check enforcement (I2, I4)
- [x] Password rules on sign-up (length/strength shown before submit) — **cloud, now**

## Cloud report

_Written by the cloud session when the queue is done._

Branch `temp/sprint-fixes`. Unit tests: **85/85** pass
(`./gradlew testDebugUnitTest assembleDebug`, placeholder google-services.json).

### Password rules on sign-up — fixed (`60eb69f`)
- **Cause:** sign-up only required Firebase's 6-character minimum, and the rule
  was never shown before submit.
- **Change:** new `PasswordRules` (8+ characters, a letter and a number). Shown
  under the password field on Create Account only, muted until met, green once
  met; Create Account stays disabled until then. Sign-in keeps the old 6-char
  gate so existing accounts still work. Firebase's weak-password message now
  states the same rule.
- **Files:** `domain/model/UserSession.kt`, `presentation/viewmodel/AuthViewModel.kt`,
  `presentation/screens/SignInScreen.kt`, `res/values/strings.xml`,
  `data/remote/FirebaseAuthDataSource.kt`, test `domain/model/PasswordRulesTest.kt`.
- **Tests:** new `PasswordRulesTest` passes; 85/85.
- **Phone check:** Sign In → Email → "New here? Create account". Type `abc12` —
  rule line grey, button disabled. Type `abcdefgh` — still grey/disabled. Type
  `abcdefg1` — rule turns green, button enabled, account is created. Then sign
  out and sign in to an older account with a short password — must still work,
  and no rule line is shown on the sign-in form.

### A3 auto-reconnect after reinstall — no code change
- **Cause:** expected behaviour. `android:allowBackup="false"` plus
  `data_extraction_rules.xml` exclude every DataStore file, so an uninstall
  wipes `last_device_address`. With no saved address,
  `BleRepositoryImpl.reconnectLastDevice()` and the reconnect watcher have
  nothing to reconnect to, so the rider must pair again.
- **Force-stop path reviewed:** `MainActivity.onCreate` (cold start) calls
  `reconnectLastDevice()`, which reads the saved address and connects; the
  saved address passes `ScooterCandidateFilter`; the service connects by
  address via `getRemoteDevice` with no scan needed; the watcher retries every
  3–30 s. Nothing clears the saved device except Forget vehicle and Delete
  local data. Startup never routes to the pairing screen by itself (Splash →
  Dashboard). No defect found.
- **Phone check:** with the scooter paired and on, Settings → Apps →
  RideConnectX → Force stop, then reopen. Expected: Dashboard, and "Connected"
  within ~10 s without opening Pair Vehicle. If it does not connect, capture
  `adb logcat -s RCX-BLE` and send it — that would be a real bug.
