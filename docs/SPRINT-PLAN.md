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
- [ ] A3 auto-reconnect showed pairing after reinstall (expected: backup is off — confirm with force-stop test)
- [ ] B4 code 36 arrow direction unconfirmed (hardware check, not code)
- [ ] I6 database upgrade — columns not verified
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
