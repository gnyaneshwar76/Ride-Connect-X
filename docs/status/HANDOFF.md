# HANDOFF — 17 September 2026 — SECURITY AUDIT DONE (no code changed)

Cloudflare's `security-audit-skill` was installed at `~/.claude/skills/security-audit/`
and run over the whole app. **Read-only: not one source file was touched** (git
status stayed at 145 modified, exactly as before the run).

**The turn arrows are still the next job.** This audit changes nothing about that
order — see the 11 Sep section below and `docs/status/PROJECT-STATUS.md`.

## Where the audit lives (outside the repo, deliberately)

```
C:\Users\eshwa\security-audit-skill\RideConnetX\run-1\
  REPORT.md            <- read this first
  NEEDS-VALIDATION.md  <- the six leads, with traces and how to check each
  FINDINGS-DETAIL.md   <- empty of findings, and explains why
  findings.json  coverage-ledger.json  architecture.md  run-metadata.json
```

## Result: six leads, zero *confirmed* vulnerabilities

That is a limit of the method, **not** a clean bill of health. The skill only allows
`confirmed` after a bounded check is *observed* inside an OS-enforced sandbox; this
Windows host has none, so nothing was executed. Every lead has a full source trace
and survived an independent verifier that tried to disprove it.

| # | Lead | Needs |
|---|---|---|
| 1 | `NavTestReceiver` exported with no permission — any app on the phone can write nav/alert frames to the cluster (debug builds only) | check whether the riding phone runs the debug build |
| 2 | Call-category notification from **any** app reaches the cluster as a caller name (the message branch checks the package; the call branch does not) | emulator + throwaway app |
| 3 | Auto-reconnect trusts the saved BLE address alone; no bonding, no `0xFEFB` service check before the rider name and message text are written | is the cluster bonded? |
| 4 | Telemetry byte-28 checksum never verified + odometer cache only ratchets up and is never cleared → **this is the 6,001,923 km card**, and it blocks new service records | a plain JVM test settles it |
| 5 | Sign-out is not an account boundary: the previous rider's profile/avatar/vehicle is uploaded into the next new account's Firestore doc | product decision + emulator |
| 6 | `ride-log.txt` (destinations, timestamps) in external storage, in release builds too, never deleted | Android 9 support wanted? |

**Desk-only fixes needing no scooter: 2, 4 and 6.** Do them after the turns.

## Fix before the first push (30 seconds)

`tools/ride-logs/` and `tools/maps-apk/` are untracked but **not** git-ignored. They
hold real device logcat captures, ride logs and third-party APKs, so one `git add .`
would publish them. Add both to `.gitignore`.

## Hardening worth doing before release

- R8 is off (`isMinifyEnabled = false`), so release builds keep every `Log.d`:
  BLE packets in hex, caller names, message senders, the Firebase uid.
- `allowBackup="true"` with empty backup rules pulls Room (emergency contacts,
  notifications), DataStore and the avatar into cloud backup.
- `firestore.rules` `profileImmutableFieldsIntact` does not protect `profile.createdAt`,
  despite its comment.
- `deleteAccount` leaves `favorites`/`rides` behind and no screen calls it.
- `persist()` treats a failed `exists()` (offline) as "new user" and overwrites the
  cloud profile with defaults.

## What the audit confirmed is RIGHT (don't undo these)

Maps relay gated on the OS-attested package name; Firestore rules owner-scoped with
default-deny and append-only rides; Google ID token verified server-side; `parseTelemetry`
cannot be crashed by a malformed frame; stale GATT clients ignored and closed (the
11 Sep fix); cloud avatar bounded to 256x256; no secrets in the repo; `ACTION_DIAL`
rather than `CALL_PHONE`, and `READ_SMS`/`READ_CALL_LOG` deliberately refused.

## Two honest gaps in the run

1. **Run status is `incomplete`.** The skill's own validators (`validate-findings.cjs`,
   `validate-coverage-ledger.cjs`) refuse every input on Windows — Node there has no
   `O_NOFOLLOW`/`O_NONBLOCK`. Patching a copy was refused by the permission system and
   was not worked around; WSL has no Node. Both JSON files were hand-checked against
   `report-schema.json` instead (6 records, 0 structural problems).
2. **Deployed Firebase state is not source-visible.** Open Firebase Console -> Firestore
   -> Rules for the project in `frontend/app/google-services.json`, confirm the published rules
   match `firestore.rules` (no leftover `if true` test-mode rule), then use the Rules
   Playground to confirm user A is denied `get`/`set` on `users/B`. Also check
   Authentication -> Sign-in method for providers you did not intend to enable.

Deferred to a later run: avatar image intake (decode/download bounds) and the
account-deletion lifecycle. Never covered: Room migrations, the SOS location-share path
end to end, `MainActivity` intent handling, merged-manifest components from libraries.

**Other three repos from that batch are not usable here:** SmoothUI and Magic MCP build
React/Tailwind web components (this app is Compose); Inspora is a Go social backend.

---

# HANDOFF — 11 September 2026, evening — TURN TEST DONE

Lockito + Maps + real cluster, 3.5 km loop around Dammaiguda (`tools/gpx/rcx-hyd-loop.gpx`).
Rider's two recordings: `Downloads/Mobile Devices/Record_2026-09-11-19-49-28_*.mp4`
and `..._19-53-18_*.mp4` (camera on the dashboard, Maps floating in the corner).

## Verdict — rider + video frames

| Maps | Code sent | Dashboard | Result |
|---|---|---|---|
| Straight / depart | 40 | straight up arrow | ✅ rider: good |
| U-turn | 39 | U-turn | ✅ rider: good |
| **Arrival** | **9** | **bullseye** | ✅ **first time ever seen** (19:51:08) |
| ↰ Turn left | 37 | curve bending LEFT | ❌ **direction right, SHAPE wrong** |
| ↱ Turn right | 35 | curve bending RIGHT | ❌ **direction right, SHAPE wrong** |

**NOT a left/right swap.** Frames at 19:55:18, 19:55:50, 19:56:58, 19:57:10 show every
turn bending the correct way. 37/35 draw a *curved road bend*, not the 90° junction
turn Maps shows. Do not swap anything.

**The fix, pending the rider's eye:** the 18 Aug sweep (`tools/cluster/sweep-1-52-worksheet.csv`)
photographed **code 1 = junction-shaped turn LEFT** and **code 4 = its mirror, turn RIGHT**.
Plan: LEFT 37→1, RIGHT 35→4. Confirm by holding 1 and 4 (`tools/cluster/sweep-hold.sh 1`) or
from the 18 Aug phone photos, then change `MapsArrowCatalog.kt` + `cluster-codes.csv`.

## Bugs found in this session

1. **Arrow stays on the dashboard after navigation ends.** `NavigationRelay.stop()`
   resets state and sends nothing. Needs a blanking packet on stop — code 46 drew
   nothing in the 11 Aug calibration, the candidate to test.
2. **Writes stop while reads continue after reconnects.** Heartbeat went out 7x per beat
   and every RX frame was logged 4x — old GATT sessions stacking up. Then
   `sendPacket — no writable link` while telemetry still arrived: the dashboard goes dark
   mid-ride while looking connected. App restart cleared it (heartbeat 1x). Root-cause fix needed.
3. **Stale straight arrow** at 19:55:02: Maps said Turn right 10 m, dashboard showed
   straight 0 m and "1.0 Km" left while Maps said 950 m — a frame from before a reroute
   stayed up. Logs were lost to the logcat buffer; reproduce with the file ride log.
4. Real ODO is **2,248 km** (live frame `000002248`). The 6,001,923 on the dashboard card
   was a bad cached value.

## Fixes built after the test — 11 Sep, night. Need hardware confirmation.

1. **Plain turns send junction glyphs:** `Maneuver.TURN_LEFT = 1`, `TURN_RIGHT = 4`
   (were 37/35, now `CURVED_ROAD_LEFT/RIGHT`). Hold 1 and 4 first next session.
2. **Arrow cleared when navigation ends:** `NavigationRelay.stop()` now sends code 46
   (the official app's blank) on the application scope.
3. **Link stacking:** `startHeartbeat()`/`sendProfile()` are `@Synchronized` (no orphan
   loops); `BleForegroundService` ignores and closes callbacks from any GATT client that
   is not the current one; `connect()` keeps a live link to the same address instead of
   tearing it down; `closeGatt()` clears `writeCharacteristic` and `linkedAddress`.
   Verify next session: heartbeat written **once** per beat, RX logged **once** per frame,
   and switch the scooter off/on mid-session — writes must resume without an app restart.

## Test-method notes
- Lockito GPX import: must be `<wpt>` points and opened via a MediaStore `content://` URI
  with `--grant-read-uri-permission` (`ImportActivity`). A plain `file://` path imports nothing.
- "Move in straight line" for dense OSRM tracks; Lockito also asks for location providers — OK.
- `dumpsys location` "last location" shows the stale REAL fix; check Lockito's notification
  `Coordinates :` instead.
- OnePlus mock-location app must be set by hand (Developer options).

---

# HANDOFF — 5 September 2026

> **⭐ PROMISE TO THE RIDER (11 Sep 2026) — do not lose this.**
> When the app is finished — signed release build, scooter tests passed, no open
> bugs, human testing done — **remind the rider and teach them how the whole app
> works**, then quiz them interview-style until they can explain it without help.
> Not before; finish the app first. Plan: BLE packet + checksum, Maps arrow
> decode, Screen→ViewModel→Repository→Room (fuel log), the `00000001` write-
> characteristic bug, odometer-vs-Trip-B tradeoff, then one feature built alone.

**Ship date 30 August has passed. Ask the rider for the new target.**

## State

Navigation works. Arrow decode proven on the cluster 21 August: 100% of frames
identified from the arrow bitmap, seven manoeuvres, zero fallbacks.

**Confirmed good by the rider, do not touch:**

- **Distance fields.** Under 1 km shows metres, 1 km and over shows kilometres.
  Rider verified this reads correctly on the dashboard. The `kmStyle` switch and
  `TEST_KMSTYLE` hook stay in place but the default (style 0) is right.
- ETA field, signal bars, battery + charging blink, telemetry.

**The only thing left to adjust: the turn arrows themselves.**

## Outstanding

| # | Item | Needs |
|---|---|---|
| 1 | **Turn arrows** — the rider's remaining complaint. Which codes draw what: hold 37, 35, then roundabout exits 20-26, `dist` = code | scooter, engine running, ~5 min |
| 2 | Arrival bullseye (code 9) — never fired | short 2 km Lockito route at 1x, run to the end |
| 3 | Alert lamps — cluster ignores them; missed-call counter reads 0, so it likely wants a **number**, not our `Y`/`N` flag | re-enable official Suzuki app, take a call, `logcat \| findstr "Data Packet"` |
| 4 | ~~Trip B mileage~~ **DROPPED 11 Sep by the rider** — the scooter already shows its own mileage on Trip A/B. Fuel log and dashboard Mileage card removed; 69/69 tests | — |
| 5 | **Signed release build — still 0%.** Keystore is the rider's job | rider |

Roundabout exit codes 20-26 are **predicted from the APK geometry**, only 23
confirmed. Ride log marks them `PREDICTED ONLY`. Item 1 confirms or corrects them.

⚠ Engine running during any scooter session — the 18 August sweep flattened the
battery. Phone hit 49 °C on 21 August under navigation + BLE + charging.

Build: **69/69 tests** (mileage tests removed with the feature). Release signing wired to a git-ignored `keystore.properties` — the rider only has to create the keystore.

**Order set by the rider, 11 Sep:** finish the turn arrows perfectly first. Weather on the cluster, find-my-parked-scooter and the alert lamps come after. The rider has another idea for the dashboard space the Mileage card left — ask after the turns.

---

# HANDOFF — 22 August 2026, 00:30

## ▶ TOMORROW: one 20-minute scooter session, everything is built and installed

Three fixes were written on 21 August after the rider rode the cluster test.
**Built, 98/98 tests passing, installed on the phone, none yet seen on hardware.**
The session was called off when the USB cable dropped at the scooter.

| # | Test | How | Time |
|---|---|---|---|
| 1 | **km distance format** — which of 3 encodings the cluster renders | `TEST_KMSTYLE --ei style 0/1/2`, then `TEST_CODE --ei code 37 --ei dist 2500` | 1 min |
| 2 | **Plain left (37) / right (35)** — is there a swap? | hold each code, `dist` = code | 2 min |
| 3 | **Roundabout exits 20-26** — predicted, never confirmed | hold each code | 3 min |
| 4 | **Arrival bullseye (code 9)** | short 2 km Lockito route at 1x, run to the end | 5 min |
| 5 | **Alert lamps** — capture the official app's packets | **re-enable official Suzuki app**, get a call, `logcat \| findstr "Data Packet"` | 10 min |

Order matters: 1-4 need **our** app holding the link, 5 needs the **official**
app holding it. So 5 goes last.

⚠ **Engine running throughout.** The 18 August sweep flattened the battery.
⚠ The phone hit **49 °C** on 21 August under navigation + BLE + charging. It
cooled to 36 °C. Don't charge during a long Lockito run unless it needs it.

### What was built on 21 August, awaiting confirmation

- **Roundabouts now send the exit direction**, using the cluster's own
  exit-specific icons at 20-26 rather than the bare ring. The rider rode the
  old behaviour and it was useless: 61 frames of that run were roundabouts
  whose exit Maps knew, drawn as a directionless circle. Codes 20-26 are
  **predicted from the APK geometry**, with 23 confirmed — the ride log marks
  them `PREDICTED ONLY`.
- **ETA field sends the arrival time**, not the phone's clock. The rider spotted
  the dashboard reading `0858Pm` while the clock beside it read 8:59. Maps
  states the journey duration in subText and the parser already extracted it.
- **Distance follows the rider's rule** — under 1000 m metres, 1000 m and above
  kilometres. Both fields. No more `9999m` on a 43 km destination. **The km
  layout has never been captured from the official app**, so three candidate
  encodings are switchable at runtime via `TEST_KMSTYLE`; metres are untouched
  and confirmed.

### NOT changed — deliberately

**Left/right were not swapped.** An early report suggested it, but the rider
corrected themselves: slight-left draws up-left and slight-right draws up-right,
both correct, and plain left/right were never actually compared against Maps.
No evidence of a swap, and the maneuver table is locked. Test 2 settles it.

---

# HANDOFF — 20 August 2026, 01:30

**Read this first, then `docs/protocol/Maps-Arrow-Decode-2026-08-20.md`, then
`docs/status/PROJECT-STATUS.md` and `docs/protocol/RideConnectX-Knowledge-Base.md`.**

Ship date: **30 August 2026.**

---

## ⭐ THE HEADLINE — the navigation unknown is SOLVED

**Google Maps does not put the turn direction in the notification's words.**
It writes the road you are turning *onto* and counts the distance down, so
`"Dammaiguda Rd  400 m"` is a turn with no left or right anywhere in the text.
The direction is carried **only by the arrow picture** in the notification's
**large icon** — a field this project had never read.

Measured on the 19 August evening ride: **1,643 of 1,873 frames (88%) went to
the cluster as STRAIGHT (code 40) when they were real turns.** That is exactly
what the rider reported seeing on the dashboard.

**Now fixed in principle and verified:** the arrow bitmap is fingerprinted and
matched against Google's own manoeuvre drawables, pulled from the Maps APK.
**67 arrows, every one mappable onto the rider's calibrated cluster codes.**

Full write-up, with the arrows drawn out and the complete 67-row table:
**`docs/protocol/Maps-Arrow-Decode-2026-08-20.md`**.

---

## ✅ WIRED IN — built 20 August 2026, 02:00. Untested on hardware.

All four steps are done and the suite is **95/95** (5 new tests pin the
behaviour). Built, but **not yet seen on the cluster** — the phone was
disconnected when this landed.

| Step | Where |
|---|---|
| The 67-name → cluster-code table | `data/nav/MapsArrowCatalog.kt` |
| Arrow → fingerprint → nearest name → code | `data/nav/ArrowMatcher.kt` |
| Arrival rule → code **9** | `MapsNotificationParser.parse` |
| STRAIGHT fallback when nothing matches | same |

**The arrow now outranks the text.** That is a deliberate reversal: the arrow is
what Maps actually draws, it exists on every frame, and it distinguishes slight
from normal from sharp and keep from fork — all of which the words usually omit.
The text remains the fallback, and a match is only accepted within
`ArrowMatcher.MAX_DISTANCE` (20 of 256 cells), so an arrow Maps introduces later
falls through to the text instead of being forced onto the nearest old shape.
If neither speaks: STRAIGHT, exactly as before, so this cannot be worse.

### ✅ TESTED ON THE CLUSTER — 21 August evening. It works.

Lockito + Maps + real cluster, stationary, engine running.
**Full write-up: `docs/testing/Cluster-Test-2026-08-21.md`.**

- **100% of frames `[from ICON]`**, zero fallbacks
- Seven manoeuvres sent, all from frames whose words carried **no direction**
- **Four codes drawn for the first time ever**: U-turn (39), slight left (19),
  slight right (41), roundabout (45)
- Photographed on the dashboard: right turn (35), roundabout (45),
  slight left (19)
- Rider saw Maps and the cluster show a U-turn **at the same instant**
- Heartbeat verified at the same time: signal bars, battery with the charging
  blink, clock, and telemetry coming back

### ⚠ ONE BUG FOUND — left and right appear SWAPPED

Rider comparing Maps' arrow against the cluster: **left draws right-ish, right
draws left-ish.** Every code *without* a mirror twin (straight, roundabout,
U-turn) was correct; every code *with* one (37/35, 19/41) was wrong.

**Our side is proven correct** — the catalogue drawables were rendered at the
desk and `maneuver_turn_normal_left` genuinely is a left arrow, matched at
distance 1. The fault is in the cluster code table.

**Hypothesis, not acted on:** 35↔37 and 19↔41 are transposed.

**The test — under a minute, first thing next session:**

```
adb shell "am broadcast -n com.gnyaneshwar.rideconnectx/com.eshwar.rideconnectx.debug.NavTestReceiver -a com.eshwar.rideconnectx.TEST_CODE --ei code 37 --ei dist 37"
```

If 37 draws a **right** turn, swap all four and navigation is done. The maneuver
table is locked by the rider — do not change it without this confirmation.

---

## The arrival rule — confirmed twice, ready to build

Maps ships `maneuver_destination` but **never switches to it in the
notification** — on arrival it keeps the previous arrow. So arrival cannot be
detected from the picture.

```
RAW  title='' | text='SRI DEVI RESIDENCY' | subText='0 min · 40 m'
RAW  title='' | text='Ramani Nilayam'     | subText='0 min · 10 m'
```

Rule: **`title` carries no distance, and `subText` remaining has collapsed to a
few metres.** That frame is currently thrown away entirely — `parse()` returns
null when neither field holds a distance — which is why arrival was invisible in
every earlier log.

---

## Two theories killed by measurement — do not re-investigate

**Screen-off does NOT stall navigation.** Over 1,873 frames: screen on averaged
a 4.3 s gap, screen off **3.1 s**. Off was *faster*. A keep-screen-on feature
would cost battery and fix nothing.

**Notification throttling is not the cause either.** Maps' Navigation channel is
`mImportance=3` (Default), not Silent. The 450 stalls are Maps updating lazily
when far from a turn. Normal, not a defect.

---

## The method lesson, and it cost two rides

Each earlier round tested **one narrow guess** — first the text fields, then a
single icon — instead of dumping the whole notification and then narrowing.
Both were wrong, and each cost the rider a real ride.

The dump now lists **every** extras key and **both** icons, so a field cannot be
missed again. When the location of data is unknown: **capture everything once,
then narrow.** Same discipline the cluster-code sweep already used.

The rider's screenshot of the notification shade is what exposed the large icon.

---

## Still open — needs the scooter (ONE short session, not three)

Nothing here blocks the arrow work. Do it all in one visit.

| Task | Needs | Time |
|---|---|---|
| Alert lamps — `'Y'` vs `'N'` polarity, via `TEST_FLAGS` | ignition on, stationary | 30 sec |
| Distance field reads `9999m` instead of km | ignition on, stationary | 30 sec |
| Roundabout exit codes 20–26 | **engine running** | ~15 min |

The `9999m` is photographed: the remaining-distance field is four digits plus a
unit character and clamps at 9999, so a 38 km destination reads as a meaningless
`9999m`. **The km encoding has never been observed** — every distance captured
from the official Suzuki app is under 1000 m — so it must be tested, not guessed.

⚠ **Run the engine during any sweep.** The 18 August sweep flattened the scooter
battery by sitting on ignition for 45 minutes.

---

## Held, at the rider's request — Trip B mileage

Built and compiling, **no UI wired**, so it is invisible in the app:

- `RefuelEntity` / `RefuelDao` / `RefuelRepository`, Room **v7** + migration
- `RefuelCalculator` + `FillEconomy` — measured km/L from the refuel log
- `MileageEstimate` gained `source` and `lastFillKmPerLitre`
- Dashboard prefers the measured figure, falls back to the fuel-bar estimate
- **20 tests**, all passing (suite is now **90/90**)

One deviation from the rider's spec, flagged: the odometer delta is preferred
over Trip B once there are two fills, because it cannot be broken by a forgotten
reset. Trip B is kept as the cross-check and is still the only source on the
first fill. A disagreement between the two is flagged, not averaged.

Still to build: the refuel entry UI on the Service screen.

---

## Environment notes from this session

- **Lockito** (`fr.dvilleneuve.lockito`) now holds mock-location; GPS Joystick
  was revoked so the two cannot fight. OnePlus **blocks `appops set` from adb**
  (`uid 2000 does not have MANAGE_APP_OPS_MODES`) — it must be changed by hand
  in Developer Options.
- Lockito must be **"Follow roads for car"**, and Maps must navigate to the
  **same destination**, or Maps reroutes continuously and the log is worthless.
  Its mode setting applies only to **newly added** points.
- Navigation can be started from the desk:
  `adb shell am start -a android.intent.action.VIEW -d "google.navigation:q=<place>&mode=d"`
  This is how the arrow catalogue was verified with no ride at all.
- Maps version **26.33.02**. If Maps updates and the arrows change shape, re-run
  the catalogue dump and re-check the fingerprints.
- `tools/ride-logs/` holds **~1 GB** of logcat archives — four near-identical
  dumps of the same buffer. Keep one.

---

## Standing instructions from the rider

- **Decide, don't ask.** Anything not visual design — libraries, architecture,
  data — just build it and report afterwards.
- **Never make design decisions.** Colours, opacity, blur, spacing and motion
  come from Figma. If something looks wrong, write a `.md` brief instead.
- **Test at the desk before asking for the phone or the scooter.** The 20 August
  session proved a whole decode can be verified over the cable with no ride.
- **Say what you are about to do on the phone before doing it.** The rider has
  given full access to laptop, phone and internet and wants them used — but
  wants to be told first.
- **Do not waste the 5-hour limit.** Batch adb calls, avoid screenshots where
  text will do, one emulator instance.
