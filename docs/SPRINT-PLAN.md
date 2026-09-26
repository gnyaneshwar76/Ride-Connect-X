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
- [x] N1 permissions are all requested at once — must be one by one (guest and sign-in setup) — **rework 26 Sep 19:30:** dialogs must chain automatically, no button taps
- [x] N2 guest name typed on the guest screen does not prefill Create Profile's Name (nickname must stay empty)
- [x] N3 Back during Create Profile exits, and reopening skips setup into a half-made account — must resume setup, never skip it — **changed 26 Sep 19:30:** unfinished setup must reset (sign out, clear) on relaunch and on Back → Exit
- [x] N4 a previous person's local name (guest "rocky bhai") prefilled another Google account's setup — local profile must be cleared/scoped per account
  - [x] N4 rework: guest → Google with no profile, Back and forward on Create Profile brings the guest name back
- [x] N5 guest signing into an account that already has a profile: no prompt — must ask "add your guest data to this account?" or let them pick another account
- [x] N6 an account whose profile exists in Firestore was asked to create a profile again after sign-out/sign-in — restore must always run and mark setup done
- [x] AUDIT after the above: walk every sign-in / sign-out / guest / setup path end to end and fix basic-logic gaps (stale state, back-navigation, half-finished setup)
- [x] N7 no warning when the app's own Notifications permission is off — warning card on the Notifications page with tap-to-allow, re-checked on resume
- [x] N9 service reminder never fired for an overdue service ("300 days over") — one in-app entry (C7) and one phone notification (E8), not repeated
- [x] N10 service-centre field accepted an odometer number — centre must contain letters; odometer numbers only
- [x] N11 R2 regression: "Find nearby on Maps" not visible on Add Service Record with no past centres — must always show (no live Places search)
- [x] N8 (low) location share takes 3–4 s — start the fix when the SOS sheet opens
- [x] N12 verify: Google sign-in swiped away mid-spinner, reopened straight to Dashboard — was sign-in complete?
- [ ] Full-app basic-logic audit (27 Sep overnight) — items below
  - [x] AUD-1 Profile name/city edit and photo removal saved on the phone only; the next sign-in brought the old ones back
  - [x] AUD-2 Settings → Connection alerts did nothing (saved, never read)
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

Branch `temp/sprint-fixes`. Unit tests: **100/100** pass after N1–N6 + AUDIT
(was 85/85; `./gradlew testDebugUnitTest assembleDebug`, placeholder
google-services.json). Real sign-in, Firestore and permission dialogs are
checked on the phone only.

### Second cloud session, 26 Sep — verification only, no code change
- The scheduled run started a second session on this branch at the same time.
  It made its own N1 fix, found N1–N6 + AUDIT already pushed here, and dropped
  its commit rather than overwrite them. Nothing of it was pushed.
- Independently rebuilt `6bb12bd` from clean with the placeholder
  google-services.json: `./gradlew testDebugUnitTest assembleDebug` passes,
  **100/100** tests.
- Reviewed the N2–N6 + AUDIT sign-in and setup logic (`ProfileHandover`,
  `AuthState.resolve`, `SetupGate`, `AuthRepositoryImpl.persist`,
  `addGuestDataToAccount` / `declineGuestMerge`). It matches the fix this
  session had planned on its own. No defects found.
- **Phone checks:** the same as the N1–N6 and AUDIT sections above.

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

### N1 permissions all at once — fixed (`a69dad9`)
- **Cause:** Permissions fired every dialog back to back (the controller
  auto-advanced on each result), then fired "turn on Bluetooth" and the
  location prompt together.
- **Change:** new `SetupSteps` walks one step at a time: Notifications →
  Bluetooth → turn on Bluetooth → Location → turn on location. The first
  dialog comes up on arrival; each next one waits for a tap on the button, which
  names it ("Allow Bluetooth", "Turn on location"…). A refusal moves on;
  nothing is asked twice in one visit; the last step shows Continue / Continue Anyway.
- **Files:** `core/util/SetupSteps.kt` (new), `core/util/PermissionsController.kt`,
  `presentation/screens/PermissionsScreen.kt`, `res/values/strings.xml`,
  test `core/util/SetupStepsTest.kt`.
- **Tests:** `SetupStepsTest` (3) pass.
- **Phone check:** clear app data, go through setup. Only the Notifications
  dialog appears by itself. Answer it: no other dialog appears, and the button
  reads "Allow Bluetooth". Tap it for the Nearby-devices dialog, then "Turn on
  Bluetooth" (only if the radio is off), "Allow location", "Turn on location"
  (only if off), then "Continue". Refuse one and the button moves to the next step.

### N2 guest name not prefilled — fixed (`4a33065`)
- **Cause:** the guest screen saved and navigated in the same tap. The
  navigation cleared its ViewModel and cancelled the save before the rider
  name was written. The keyboard's Done saved without navigating.
- **Change:** navigation happens only after the guest is saved, from the button
  and from Done. The nickname is still never derived from the name.
- **Files:** `presentation/viewmodel/AuthViewModel.kt`,
  `presentation/screens/GuestProfileScreen.kt`, `presentation/viewmodel/VehicleViewModel.kt` (comment).
- **Tests:** lifecycle ordering, no pure logic to test; 100/100 still pass.
- **Phone check:** Continue as guest, type "Test Rider", tap Continue (and on a
  second run press keyboard Done instead). Create Profile's Name shows
  "Test Rider"; Nickname is empty.

### N3 Back during Create Profile skipped setup — fixed (`5374278`)
- **Cause:** Splash sent every signed-in user to the Dashboard, whatever the
  setup state.
- **Change:** `SetupGate.launchRoute`: signed in with the profile unfinished →
  Permissions (it forwards to Create Profile when nothing is missing); finished
  → Dashboard. Back on Create Profile now asks "Finish setting up later?"
  (Cancel / Exit) instead of leaving silently.
- **Files:** `presentation/navigation/SetupGate.kt` (new), `NavGraph.kt`,
  `presentation/screens/CreateProfileScreen.kt`, test `SetupGateTest.kt`.
- **Tests:** `SetupGateTest` pass.
- **Phone check:** start setup (guest or Google), on Create Profile press Back.
  The sheet appears; Cancel stays. Press Back again → Exit. Reopen the app: it
  goes back into setup (Permissions or straight to Create Profile), never the
  Dashboard. Also swipe the app away from Recents mid-setup and reopen: same.

### N4 previous person's name leaked into another account — fixed (`c197381`)
- **Cause:** the local profile had no owner. Signing into an account with no
  cloud profile merge-wrote whatever was local into it and prefilled its setup.
  Guest rows were also claimed by any account.
- **Change:** the local profile records its owner (uid or guest) and a "carry"
  mark set only by "Save to an account" / "Move to another account".
  `ProfileHandover` decides on each sign-in:
  - the account's own profile → kept;
  - a carried profile going to an account with none of its own → carried, and
    guest rows are claimed only in this case;
  - anything else → cleared with its photo, then the account's profile is restored.

  Continue-as-guest clears a profile that belongs to an account.
- **Files:** `domain/model/ProfileHandover.kt` (new), `data/local/UserPreferencesStore.kt`,
  `data/repository/AuthRepositoryImpl.kt`, test `domain/model/ProfileHandoverTest.kt`.
- **Tests:** `ProfileHandoverTest` pass.
- **Phone check:** as guest "rocky bhai", finish setup, Profile → Sign out.
  Sign in with a Google account that has **no** profile: Create Profile shows
  that account's Google name and picture, not "rocky bhai", with an empty
  nickname. Then check the other way round: as a guest, Profile → "Save to an
  account" → sign into an empty account. The guest's name and vehicle are
  carried over, and so are its contacts and rides.

### N5 guest into an account with a profile: no prompt — fixed (`6a3e641`)
- **Cause:** a guest signing into an account with a profile had the account's
  profile restored over theirs and their rows claimed, silently. The old "Guest
  data added" screen was keyed on the session still being a guest at sign-in,
  which it never is after "Save to an account", so it never showed.
- **Change:** that case now asks "Add your guest data to this account?" with
  **Add to this account** and **Choose another account**. Nothing moves before
  the answer.
  - Add: the account's profile wins and the guest's rides, service records and
    contacts join it.
  - Choose another: signs out of that account and goes back to Sign In, keeping
    the guest data for the next account.

  The question is saved, so leaving the app brings it back.
- **Files:** `presentation/screens/ProfileFoundScreen.kt`,
  `presentation/viewmodel/ProfileFoundViewModel.kt` (both rewritten),
  `domain/repository/AuthRepository.kt`, `AuthRepositoryImpl.kt`,
  `UserPreferencesStore.kt`, `SetupGate.kt`, `NavGraph.kt`, `AuthViewModel.kt`.
- **Tests:** `ProfileHandoverTest` (ASK case) and `SetupGateTest` pass.
- **Phone check:** as a guest, add an emergency contact. Profile → "Save to an
  account" → sign into a Google account that **already has** a profile. The
  question appears with that account's name and email.
  (a) "Choose another account" → Sign In; pick an empty account → the guest
  profile and contact carry over.
  (b) Repeat and tap "Add to this account" → Dashboard shows the account's own
  name and vehicle, and Safety lists the guest's contact.
  (c) Kill the app while the question is showing and reopen: it asks again.

### N6 existing profile asked to be created again — fixed (`118f484`)
- **Cause:** Sign In navigated as soon as Firebase reported the user, while the
  sign-in was still reading the account and restoring its profile. With
  permissions already granted, Permissions read "not done" at once → Create
  Profile. Also, a failed account read (offline) left the rider signed in with
  nothing restored.
- **Change:** Sign In routes only after the sign-in call has finished (restore
  included). A failed account read undoes the sign-in and shows "Couldn't reach
  your account. Check your internet and try again."
- **Files:** `presentation/navigation/NavGraph.kt`, `data/repository/AuthRepositoryImpl.kt`.
- **Tests:** navigation timing, not pure; 100/100 pass.
- **Phone check:** with an account that has a finished profile: Profile →
  Sign out → sign in with the same account. Goes to the Dashboard with the name,
  vehicle and photo back, and no Create Profile. Repeat with mobile data and
  Wi-Fi off after choosing the account: the error shows on Sign In and you stay
  there (not Create Profile).

### AUDIT sign-in / sign-out / guest / setup — fixed (`0b9a24c`)
- **Traced:** Splash; Intro; Sign In (Google, email sign-in and sign-up with
  verification); guest; Permissions; Create Profile; Profile Found; Profile
  sign-out / move / delete account; `OwnerScope`.
- **Fixed:**
  1. A Firebase user counted as signed in before the sign-in had finished on the
     phone, so a sign-in killed halfway resumed into setup with nothing restored
     and rows saved as the guest's. The session is now saved **last**, and
     `AuthState.resolve` accepts a Firebase user only when its saved session
     matches it.
  2. "Save to / Move to another account" forced setup to done, so an unfinished
     profile skipped setup on the next account.
  3. A guest-merge question left by an unfinished sign-in could have been shown
     to a different account.
  4. Permissions' Continue read a placeholder instead of the stored setup state.
  5. The "Couldn't reach your account" message no longer starts with "Signed in, but".
- **Files:** `domain/model/AuthState.kt`, `AuthRepositoryImpl.kt`, `NavGraph.kt`,
  `ProfileViewModel.kt`, test `domain/model/AuthStateResolveTest.kt`.
- **Tests:** `AuthStateResolveTest` (3) pass; 100/100.
- **Noted, not changed (owner to decide):**
  - Back on the Permissions step during setup still leaves without asking;
    reopening resumes, so nothing is lost.
  - A guest's plain "Sign out" keeps their rides and contacts on the phone for
    the next guest, as the sheet says.
  - The "Save to an account" sheet says rides, service records and contacts
    "stay on this phone either way". They now move into the account the guest
    signs into (or on "Add"), so the wording needs updating.
- **One-off effect after installing:** an older install whose saved session has
  no account id shows as signed out once. Signing in again restores everything.
- **Phone check:**
  1. Signed out, start Google sign-in, pick the account, and swipe the app away
     from Recents while the spinner is still showing. Reopen: you get Intro /
     Sign In, not setup or the Dashboard. Signing in again works normally.
  2. With a finished account, Profile → "Move to another account" → sign into
     another account with no profile. You land on the Dashboard with the carried
     profile. Repeat after leaving a guest's setup half done: you land in setup,
     not on the Dashboard.


### Third cloud session — re-check (26 Sep)
- Pulled `temp/sprint-fixes` at `d3849c4`, rebuilt with a placeholder
  `google-services.json`: `./gradlew testDebugUnitTest assembleDebug` passes,
  **100/100** unit tests. No code changes; N1–N6 and AUDIT stand as reported
  above. Ready for the owner's phone re-test.

### N7–N11 (26 Sep, 18:50 report) — third cloud session
Built with a placeholder `google-services.json`: `./gradlew testDebugUnitTest
assembleDebug` passes, **108/108** unit tests (100 before; +5 N9, +3 N10).

**N7 app notifications off, no warning** — `cc09046`
- **Cause:** only the notification *access* listener had a warning; nothing
  checked the app's own POST_NOTIFICATIONS permission.
- **Change:** `PostNotificationsWarning`, the same amber card, on the
  Notifications page only, re-checked on resume. Tap asks for the permission;
  once Android won't show the dialog any more, it opens the app's notification
  settings.
- **Files:** `components/NotificationAccessWarning.kt`, `screens/NotificationsScreen.kt`.
- **Phone check:** Android 13+. Settings → Apps → RideConnectX → Notifications
  off. Open Notifications: "Notifications are off" card. Tap → dialog, or the
  app's notification settings if denied before. Turn on, come back: card gone.

**N9 service reminder never fired** — `3ad92db`
- **Cause:** reminders were never built. `NotificationRepository.notify` had no
  callers and nothing was scheduled, so "Overdue — 300 days over" produced no
  in-app entry and no phone notification.
- **Change:** `ServiceReminder` checks each account's service status whenever it
  changes while the app runs (record added/edited, past dates included;
  odometer; sign-in/switch), plus a daily WorkManager job while it is closed.
  `ServiceReminderRule` (pure) keys a reminder by level (due soon / overdue) and
  the service it counts from: one in-app entry + one phone notification per
  level per service cycle, never per check. A new or edited record starts a new
  cycle. Status is paired with its owner, so a reminder never lands on the wrong
  account. Without the permission only the in-app entry is made.
- **Files:** `domain/model/ServiceReminderRule.kt`, `data/repository/ServiceReminder.kt`,
  `ServiceRepository.kt`, `NotificationRepository.kt`, `ServicePreferencesStore.kt`,
  `core/di/ServiceEntryPoints.kt`, `RideConnectXApp.kt`, `app/build.gradle.kts` +
  `libs.versions.toml` (work-runtime-ktx 2.10.0), test `ServiceReminderRuleTest`.
- **Phone check:** on the overdue account, open the app. Within seconds: one
  phone notification "Service overdue — … 300 days over" and one entry in
  Notifications. Close and reopen: no second one. Add a record dated today:
  nothing. Edit it to a date 4+ months ago: one new reminder. Service reminders
  off in Settings: nothing.

**N10 odometer number accepted as a service centre** — `c7a432d`
- **Cause:** the centre field took any text.
- **Change:** a centre must contain a letter (blank still saves as "Not
  recorded"); inline error under the field. The odometer field already keeps
  digits only.
- **Files:** `viewmodel/ServiceViewModel.kt`, `screens/ServiceScreen.kt`,
  `res/values/strings.xml`, test `ServiceCentreTest`.
- **Phone check:** Add Service Record, centre `2000` → Save shows the red error,
  nothing saved. `Sai Suzuki` saves. Try typing letters in Odometer: ignored.

**N11 "Find nearby on Maps" not visible** — `6a2467e`
- **Cause:** the option was never built on this branch; with no past centres
  the row under the field was empty.
- **Change:** a "Find nearby on Maps" chip always leads that row and opens a
  Suzuki service-centre search in Maps (browser if no Maps app). Past centres
  follow it once one exists. No Places API (R2 still waits on billing).
- **Files:** `screens/ServiceScreen.kt`, `res/values/strings.xml`.
- **Phone check:** fresh account, Add Service Record: chip visible, opens Maps
  near you. Save one record with a centre; the next Add shows that centre as a
  chip after the Maps one.

**N8 location share takes 3–4 s** — `f47e6dc`
- **Cause:** the fix was requested only at the tap.
- **Change:** opening the SOS sheet (sharing on, precise granted) starts the
  fix; the tap uses it if under a minute old, otherwise fetches again. Used
  once. Precise-only and one-share-at-a-time unchanged.
- **Files:** `viewmodel/SafetyViewModel.kt`, `screens/SafetyScreen.kt`.
- **Phone check:** open SOS, wait ~3 s, tap share: the message is ready almost
  at once. With "Approximate" only: still refused as before.

### Re-test rework (26 Sep, 19:30 report) — third cloud session
Built with a placeholder `google-services.json`: `./gradlew testDebugUnitTest
assembleDebug` passes, **110/110** unit tests. N2, N5, N6 passed on the phone.

**N1 rework: dialogs must chain by themselves** — `d5a4870`
- **Cause:** only the first step was launched automatically; each later step
  waited for a tap on its button.
- **Change:** the Permissions screen launches the next step as soon as the
  previous one is answered (allow or deny): Notifications → Nearby devices →
  Bluetooth on (only if off) → Location → Location on (only if off). A
  permission is answered when its result arrives; a radio prompt when the app
  is back in front. Never two at once. The button remains for anything refused.
- **Files:** `screens/PermissionsScreen.kt`, `core/util/SetupSteps.kt` (comment).
- **Phone check:** fresh install, Bluetooth and location off. After sign-in,
  without touching any button: Notifications dialog → answer → Nearby devices
  dialog → answer → "turn on Bluetooth" → answer → Location dialog → answer →
  location switch dialog. Deny one: the next still comes. Then Create Profile.

**N3 change: reset instead of resume** — `eb0249d`
- **Change:** on launch, signed in with setup not completed (no guest-merge
  question waiting) → Firebase signed out, session, unfinished local profile,
  photo and cached readings cleared, app opens on Intro. Back → "Leave setup?"
  → Exit on Create Profile does the same reset, then closes. The reset writes
  nothing to Firestore; Create Profile only writes to the cloud on Continue.
- **Not covered:** if Android kills the app in the background and later
  restores it from Recents (not a swipe-away), Android restores the screen, so
  setup continues where it was rather than resetting.
- **Files:** `navigation/SetupGate.kt`, `navigation/NavGraph.kt`,
  `screens/CreateProfileScreen.kt`, `viewmodel/AuthViewModel.kt`,
  `domain/repository/AuthRepository.kt`, `data/repository/AuthRepositoryImpl.kt`,
  test `SetupGateTest`.
- **Phone check:** (a) sign in with a Google account with no profile, reach
  Create Profile, type a name, swipe the app away, reopen: Intro, signed out;
  sign in again: Create Profile is empty apart from the Google name. (b) Same,
  but press Back → Exit: app closes; reopen: Intro. (c) As a guest: same two
  checks; the guest name is gone.

**N4 rework: guest name came back on Back/forward** — `906c29f`
- **Cause:** the form reads the stored profile once; the guest name was put
  back into it by sign-in. (1) Sign-in restored any profile fields in the
  account's cloud document, complete or not, so a name with no vehicle (the
  guest name merge-written by the original leak) came down on every sign-in.
  (2) "Save to an account" carried an unfinished profile and wrote it into the
  new account's Firestore document. Nothing in the form used rememberSaveable
  or SavedStateHandle for the name.
- **Change:** only a complete cloud profile (name + vehicle) is restored; an
  unfinished local profile is cleared, never carried. The stale cloud name is
  overwritten when the account finishes setup.
- **Files:** `domain/model/ProfileHandover.kt`, `data/repository/AuthRepositoryImpl.kt`,
  test `ProfileHandoverTest`.
- **Phone check:** the same guest → Google account as before. Create Profile
  shows the Google name. Back → Exit, reopen, sign in again (N3 resets): still
  the Google name, never "rocky bhai". Finish setup, sign out, sign in: your
  own name and vehicle come back.

**N12 verify: swiped away mid sign-in, reopened to Dashboard** — `eb0249d`
- **Finding:** Dashboard was correct. Since the AUDIT fix the account's
  session is saved as the very last step of sign-in, and the app counts an
  account as signed in only once it is saved. Sign-in keeps running in the
  background after a swipe, so it had finished and restored the complete
  profile. A sign-in killed before that last step opens on Intro, signed out.
- **Gap fixed:** in that killed case Firebase still held the account while the
  app showed signed out. Launch now signs Firebase out too.
- **Phone check:** repeat the swipe on an account with a profile: Dashboard
  with its name and vehicle, or Intro if it had not finished — never setup.

