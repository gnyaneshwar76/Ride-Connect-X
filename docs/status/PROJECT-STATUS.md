# RideConnectX — Project Status

> **⭐ PROMISE TO THE RIDER (11 Sep 2026) — do not lose this.**
> When the app is finished — signed release build, scooter tests passed, no open
> bugs, human testing done — **remind the rider and teach them how the whole app
> works**, then quiz them interview-style until they can explain it without help.
> Not before; finish the app first. Plan: BLE packet + checksum, Maps arrow
> decode, Screen→ViewModel→Repository→Room (fuel log), the `00000001` write-
> characteristic bug, odometer-vs-Trip-B tradeoff, then one feature built alone.

> **17 September 2026 — first SECURITY AUDIT done. No code was changed.**
> Cloudflare's `security-audit-skill` (installed at `~/.claude/skills/security-audit/`)
> was run over the whole app, read-only. Result: **six leads, zero confirmed
> vulnerabilities** — and that is a limit of the method, not a clean bill of health:
> the skill only allows "confirmed" after a check is *observed* in an OS-enforced
> sandbox, which this Windows host does not have, so nothing was executed.
> Full write-up: `docs/status/HANDOFF.md` (17 Sep section) and
> `C:\Users\eshwa\security-audit-skill\RideConnetX\run-1\REPORT.md`.
> Headline: finding 4 explains the stuck **6,001,923 km** odometer card — telemetry
> checksum never verified and the odometer cache only ratchets up, never cleared.
> **Do before the first push:** add `tools/ride-logs/` and `tools/maps-apk/` to
> `.gitignore`; they hold real device logs and are not ignored today.
> **Order is unchanged: the turn arrows come first.**
>
> **11 September 2026 — mileage DROPPED by the rider.** The scooter already
> shows its own mileage on Trip A/B, so the app was duplicating it. The fuel log
> and the dashboard Mileage card are removed (the Figma dashboard never had that
> card). Room tables `refuels`/`fuel_samples` are left unused to avoid a schema
> bump before ship. **69/69 tests.** Next: the turn arrows, then everything else.
>
> **5 September 2026 — navigation WORKS on the cluster.**
> The arrow decode was proven on hardware 21 August: 100% of frames identified
> from the notification's arrow bitmap, seven manoeuvres, zero fallbacks.
> Distance fields, ETA, signal, battery and telemetry are all **signed off by
> the rider**. **Only the turn arrows remain open** — which cluster code draws
> which shape, a 5-minute held-code test.
> **Ship date 30 August has passed; ask the rider for the new target.**
> **Read `docs/status/HANDOFF.md`, then `docs/testing/Cluster-Test-2026-08-21.md`.**
>
> **20 August 2026 — THE NAVIGATION UNKNOWN IS SOLVED.**
> Google Maps carries the turn direction **only in the notification's arrow
> picture**, never in the words. 88% of the 19 August ride went to the cluster
> as STRAIGHT because of it. All 67 of Maps' manoeuvre arrows are now
> identifiable and mapped to the rider's calibrated cluster codes.
> **Read `docs/protocol/Maps-Arrow-Decode-2026-08-20.md` first, then `docs/status/HANDOFF.md`.**
> Not yet wired in — held for the rider's signal.


**Living document. Update at the end of every working session.**

> **New chat? Read this first, then `docs/protocol/RideConnectX-Knowledge-Base.md`.**
> Between them they hold the BLE protocol, the architecture, everything fixed and
> everything still open. Do not ask the user for data that is already in here.

Last updated: **17 August 2026**

---

## 🎯 THE GOAL — do not ask the rider to repeat this

**The app must be complete and functional by 30 August 2026.** That is the date
the rider's Pro subscription ends. Everything is planned around it. It is the
rider's goal and it is the assistant's goal — treat it as the deadline for all
remaining work.

### Current order of work (set by the rider, 13 August 2026)

1. ~~**Screens first.**~~ **DONE 13 August — all 23 screens now exist.**
   Screens 18-23 were built in one session; see "13 August session" below.
2. ~~**Emulator pass.**~~ **DONE 13 August.** All six new screens walked end to
   end on a running emulator. Four defects found and fixed, all re-verified on
   the device. Full write-up: `docs/testing/Emulator-Test-Report-2026-08-13.md`.
3. **Next: the rider's own phone.** Everything BLE is untested — the emulator
   has no radio.
4. **Then** return to navigation / the maneuver-code work, when the rider says.

### ⏸ ON HOLD — navigation & the 1-52 cluster sweep

The 1-52 code sweep and all further arrow work are **paused by the rider**.
Do not start them, do not ask for the scooter, do not re-plan them. The rider
will say when to resume — the phrase to wait for is *"continue navigation"*.
Everything needed to restart is already written up under "Next session" below.

### Design rules for the remaining screens

- The design is **final and fixed**: <https://hazel-mummy-35461748.figma.site/>
  and `figma design explanation.md` in the repo root (screens 18-23 are at
  lines 3640-5061).
- **Do not redesign anything.** Port the design as it is — same layout, same
  copy, same colours, same order of elements. Build the frontend to match.
- Colours and text styles come from `Rcx.colors` / `RcxType` only. No hardcoded
  hex in screens.
- Follow the shape of `NotificationsScreen.kt` (screen 17): `BackHeader`,
  `WindowInsets.safeDrawing`, `widthIn(max = 600.dp)`, private composables per
  card, real state from a Hilt ViewModel where the screen has state.

> **11 August — arrow calibration done for codes 31-45, and 🔒 LOCKED.**
> Every one photographed on the rider's Access 125, one at a time. Left is 37,
> right 35, straight 40, U-turn 39, roundabout 45 — the 4 August table was wrong
> in almost every entry. Installed and verified end-to-end on the phone.
> **Do not change these values unless the rider asks for a re-calibration.**
>
> Still open: **codes 1-30 have never been swept**, and that is where the
> destination flag, merge, ramp and lane-guidance icons almost certainly are.
> Coverage today is 13 of 30 Maps maneuvers exact, 8 approximated, 9 missing.

> **6 August — the first real ride happened, and it found the big one.**
> Navigation was reading the wrong fields of the Maps notification, so the whole
> trip relayed only `STRAIGHT` and `LEFT`. Fixed and verified against a live
> notification. **The maneuver-code table is now suspect** — the cluster drew a
> U-turn for what we were sending as straight. Re-calibrate before trusting it.
> Ride log: `adb pull /sdcard/Android/data/com.gnyaneshwar.rideconnectx/files/ride-log.txt`

> **Earlier note:** Everything below is built, installed and tested
> except one thing — the turn arrows have never been watched on a live route.
> That is the first job, and it needs the scooter on and near the phone.

---

## TL;DR

**The two long-standing unknowns are solved.** Both established on the rider's
own Access 125 (`SAS210217219`) in a single session at the scooter:

- **Maneuver arrows calibrated.** Codes 34-53 swept one at a time against the
  dashboard and recorded. Left is 38, not 40; straight is 39, not 46.
- **Telemetry decoded.** ODO / Trip A / Trip B read exactly against a
  photographed cluster. Fuel bar too.
- **The greeting is explained.** Byte 27 is not a constant — it says whether
  this is the same cluster as last time, and hardcoding it is why the dashboard
  said `ESHWAR P CONNECTED`.
- **Heartbeat implemented** — phone battery, signal bars, clock, and the
  message / missed-call lamps now have a sender. There wasn't one before.

All of it then **verified on the scooter the same evening**:

- Cluster printed **`WELCOME ESHWAR P`** once byte 27 sent `'R'`.
- Battery bars, charging (blinking bar), signal bars and the clock all appear on
  the dashboard.
- Auto-reconnect brought the link up unprompted on app launch.

31/31 tests pass. Installed and crash-free on the phone.

**Battery is signed off by the rider — locked, do not touch.** Four levels, 88%
lights three, the fourth blinks while charging. Identical to the official app.

**Signal is settled — we already matched.** The official app's own logged packet
carries `'3'` at full strength, byte for byte the same as ours. The four-bar
report was a miscount.

**Two real bugs the capture found**, both now fixed and pinned by a test that
reproduces the official packet exactly:

- Notification / missed-call flags were **inverted**. `'Y'` is the alert, `'N'`
  is idle. Every idle packet had been announcing a permanent alert.
- The cluster clock is **12-hour**, not 24.

**Still to prove:** the turn arrows on a live route.

---

## The protocol (all hardware-confirmed)

| Item | Value |
|---|---|
| Service | `0000fefb-0000-1000-8000-00805f9b34fb` |
| **Write** | `00000001-…008025000000` (**WRITE_NO_RESPONSE**) |
| Notify | `00000002-…008025000000` |
| Frame | 30 bytes, `A5` … checksum@28 … `7F` |
| Checksum | Access family: `~(sum bytes 1..27) & 0xFF` |
| Nav packet | `0x31`, maneuver code as **raw byte at index 2** |
| Profile packet | `0x36`, name at 2..21, `FF` at 22-26, `'F'` at 27 |

Three of these were wrong earlier and each cost a session:

1. **Write characteristic.** Was `00000003` (plain WRITE) because it looked
   right. The official app uses `getCharacteristics().get(0)` = `00000001`.
   Packets were accepted by the stack and silently ignored by the cluster.
   Switching to `00000001` is what made the dashboard light up **and** made the
   scooter start replying.
2. **Maneuver position.** Was written as two ASCII digits at bytes 23-24. It is a
   raw byte at index 2; 23-24 are GPS/airplane status characters.
3. **Checksum.** Was a plain sum. Suzuki's `SuzukiApplication.m8569a()` is
   `flag ? 255 - sum : sum`, and the flag is the vehicle model, taken from the
   BLE name. `SAS21…` → Access → complement branch.

---

## The calibration, and what it overturned

Two translations sit between Maps and the dashboard:

1. **Maps phrasing → cluster code** — ours, in `MapsNotificationParser`.
2. **Cluster code → the icon drawn** — *cluster firmware*, in no APK.

Mapping 2 was established the only way it can be: by sending each code and
looking. Codes 34-53, one at a time, rider reporting in plain words. Result in
`tools/cluster/cluster-codes.csv` and the knowledge base.

It overturned two things that had been treated as settled:

- `TURN_LEFT = 40 / TURN_RIGHT = 41 / U_TURN = 48 / ROUNDABOUT = 51` were all
  wrong. Left is **38**, right **42**, U-turn **41**, roundabout **45**.
- **46 is not straight — it draws nothing.** Straight is **39**. `46` had also
  been the parser's fallback for unrecognised Maps phrasing, so any wording the
  regexes did not know silently blanked the cluster.

A useful accident: codes **54-58 are weather icons** (fog, showers, storms,
snow), found in `C4941q0`. That is how the official app puts weather on the
cluster — free groundwork for that feature.

---

## Next session

### Step 0 — Collect the ride log first ⭐

The rider is testing navigation on a **real ride on a route they know**, with no
laptop attached. The app now records every maneuver to a file so the evidence
survives:

```
adb pull /sdcard/Android/data/com.gnyaneshwar.rideconnectx/files/ride-log.txt
```

Format — one line per maneuver, `code` is what the cluster was told to draw:

```
===== route started 2026-08-06 08:12:03 =====
08:12:44  code=38  dist=300m  sent=yes  "Turn left onto MG Road"
```

Compare each `code` against what the rider says the dashboard actually drew.
A slip of one across codes 39-45 is the specific thing to look for — those came
from batched verbal answers during the sweep, and the spot-check on 41 was never
completed.

### Step 0 — READY TO RUN: full sweep of codes 1-52

The rider wants every code confirmed rather than spot-checks. Everything is
prepared; this only needs the scooter on and the phone cabled.

**Worksheet:** `tools/cluster/sweep-1-52-worksheet.csv` — every code with its predicted
meaning from the APK, ready to fill in. The 15 photographed codes are marked
LOCKED and can be skipped, so the real work is **36 codes**.

**Method — the two rules that matter:**

1. **Send `dist` equal to the code.** The distance field then prints the code
   number on the cluster itself, so every photograph is self-labelling and an
   answer can never be matched to the wrong code. This is what went wrong on
   4 August.
2. **One code at a time, held until the rider answers.** Run the send loop in
   the background so the icon stays up indefinitely; stop it only when the
   answer is in.

```powershell
# hold one code on the cluster (dist = code, so the screen labels itself)
$cmp="com.gnyaneshwar.rideconnectx/com.eshwar.rideconnectx.debug.NavTestReceiver"
while ($true) {
  adb shell "am broadcast -n $cmp -a com.eshwar.rideconnectx.TEST_CODE --ei code 19 --ei dist 19"
  Start-Sleep -Milliseconds 800
}
```

Order to work in: **19, 18, 16, 17, 15** first — the arc set, where a match
proves the whole APK inference and hands us slight-left. Then 1-9, then the
roundabouts.

### Step 1 — Six spot-checks, if time is short (~6 min)

The APK was decoded on 11 August (`Suzuki-APK-Reference.md`), which turned the
30-code sweep into six confirmations. The full range is **cluster 1-52** and most
meanings are already predicted from the icon geometry — with 40/41/42 matching
our photographs exactly.

| Code | Predicted | Why it matters |
|---|---|---|
| 19 | **slight left** | the piece we thought did not exist |
| 18 | left | proves the arc-set table |
| 16 | U-turn | " |
| 8 | straight | proves the basic set 1-9 |
| 1 | turn left | " |
| 23 | roundabout, straight-ahead exit | proves the roundabout sets |

If these land, we go from 15 known icons to roughly 45 without sweeping the rest.

### Step 1b — Full sweep of 1-30, only if the spot-checks fail

**The biggest remaining gap.** The 11 August calibration covered 31-45 only;
31 turned out to be keep-left and the sweep started there arbitrarily. The
**destination flag, merge, ramp and lane-guidance** icons are almost certainly
below it — the rider's report shows the official app drawing a chequered flag on
arrival, so the cluster has one and we simply do not know its number.

Same method, one code at a time, held until the rider answers:

```
adb shell "am broadcast -n com.gnyaneshwar.rideconnectx/com.eshwar.rideconnectx.debug.NavTestReceiver \
  -a com.eshwar.rideconnectx.TEST_CODE --ei code 30 --ei dist 200"
```

Then re-check 46-53: "blank" for those comes from the discredited 4 August
sweep and has never been verified.

Coverage against the 30 standard Maps maneuvers today: **13 exact, 8
approximated, 9 missing** — see the knowledge base for the full table.

### Step 2 — Ride a real route The ride log now names the expected icon in plain English, so
a mismatch is obvious:

```
[08:12:44]  Maps said : "Turn left onto MG Road"  (300 m)
            Cluster should show : LEFT   (code 37)
```

Two things to watch specifically:
- **Slight left** is drawn as a full left turn (no up-left diagonal exists).
  Confirm that reads acceptably at a real junction.
- **Right** uses 35 (curved). 42 is a second, flatter right arrow — swap if the
  rider prefers it.

### Step 1b — Previous plan, for reference

The only thing calibrated but never watched end to end. Needs the scooter **on
and within Bluetooth range of the phone** — a `status=147` in the log means it
is not.

1. Disable the official Suzuki app first; it holds the link exclusively.
2. Open RideConnectX, wait for auto-reconnect (`GATT connected` in the log).
3. Drive a Maps route — the rider's **GPS joystick app** does this from a desk,
   no riding — or inject maneuvers directly:

```
adb shell "am broadcast -n com.gnyaneshwar.rideconnectx/com.eshwar.rideconnectx.debug.NavTestReceiver \
  -a com.eshwar.rideconnectx.TEST_NAV -e title 'Turn left onto MG Road' -e text '300 m'"
```

4. Rider confirms the arrow matches the instruction. Codes 39-45 came from
   batched verbal answers during the sweep and the spot-check on 41 was never
   completed, so a slip of one is the thing to look for.

### Step 2 — Confirm the stored cluster name (one connect, no effort)

`lastDeviceName` used to store `"Suzuki Scooter"` — the placeholder used when no
scan result is cached, which is always the case on auto-reconnect. That name
feeds `configureForDevice`, which picks the checksum branch from the model, so
it was landing on the Access branch by luck rather than by reading `SAS21…`.

**Fixed 5 August but not yet seen on hardware.** The service resolves the name
from the connected device; the repository now prefers that, then the previously
stored name, and only then the placeholder. Confirm with:

```
adb logcat -s RCX-BLE | findstr "flag="
```

The line prints both cluster names. It should read `SAS210217219`, not
`Suzuki Scooter`.

### Step 3 — Capture the official app's navigation packets

**The official app prints every packet it sends to logcat** — no root, no
sniffer. That one trick replaced the whole HCI-snoop plan (which is a dead end
here: OnePlus writes an encrypted `.cfa`, and the bugreport copy was 16 bytes).

```
adb logcat -c
adb logcat -d | findstr "Data Packet"
```

Run a route in the official app — the rider's GPS joystick app can drive it from
a desk — and the `A5 31 …` frames give Suzuki's own maneuver codes. That would
confirm the observed arrow table from their side, not just ours.

Clear logcat *before* pairing to also catch the `A5 36` profile packet.

**Disable the official app afterwards** — it takes exclusive BLE access and
respawns itself. Never uninstall; the rider needs it for real journeys.

### Step 2 — Confirm the stored cluster name (needs one connect)

`lastDeviceName` is `"Suzuki Scooter"`, a placeholder, not `SAS210217219`.
The greeting comparison is unaffected, but `configureForDevice` cannot read the
model from it and silently falls back to the Access checksum branch — correct
for this vehicle by luck, wrong for an e-ACCESS or TFT model.

### Step 2 — Fake route test

The rider has a **GPS joystick app** that moves their location, driving a full
Maps route from a desk with no riding. This is the real proof the arrows are now
correct.

### Step 4 — Remaining screens

Parked. The rider will say when. Settings, Rider Profile, bottom nav, the
five-segment fuel bar and weather chip (telemetry and weather codes are both
available now).

---

## ▶ START HERE — read `docs/status/HANDOFF.md` first

**`docs/status/HANDOFF.md` has the live state.** As of 17 August the build is green,
Haze is verified blurring on a device, and the tablet layout bug is fixed. The
next piece of work is the remaining 11 screens of translation.

---

## 17 August 2026 — build recovered, Haze verified, tablet layout fixed

**The two 16 August blockers are cleared.** The build is green again (70/70
tests, APK installs and runs), and **Haze 1.6.10 compiles against Compose BOM
2024.12.01 exactly as written** — no downgrade to 1.2.0 was needed and that
note is retired.

**The frosting is confirmed on a running emulator (API 37).** On Statistics in
Glass the sunset band and mountain ridge behind the "No rides yet" card are
visibly smeared inside the panel while staying sharp outside it, and the label
text stays crisp — smeared text was the exact failure mode of `Modifier.blur`,
so that is the proof it is a genuine *backdrop* blur.

### The 600dp cap had never worked — fixed on all 20 screens

`widthIn(max = 600.dp)` was a no-op everywhere it appeared, because it came
*after* `fillMaxSize()`:

```kotlin
.fillMaxSize()          // pins MIN width to the whole screen
.widthIn(max = 600.dp)  // therefore cannot shrink it
```

**Invisible on a phone**, where the screen is narrower than 600dp anyway — which
is why it survived every phone review. On tablet metrics (≈1484dp wide) the
vehicle hero became a letterbox with a marooned scooter, the ODO/TRIP/FUEL cards
stretched full width with values stranded in the corner, and the quick-action
tiles became huge boxes around tiny content.

Fixed by capping before filling — `.fillMaxHeight()` … `.widthIn(max = 600.dp)`
`.fillMaxWidth()`. Verified on tablet metrics (correctly capped and centred,
cards proportioned as on a phone) and on the phone (pixel-identical to before).
70/70 tests pass, no fatal exceptions.

The feared "dead space either side" is not a problem: on the photo-backed
screens the image is full-bleed behind the column and carries the whole width.

---

## 16 August 2026, the rider's phone review

The rider walked the whole app on their phone and reported twelve areas.
**Full write-up: `docs/archive/Review-2026-08-16-Response.md`** — read that before
anything else; it records what each report turned out to be and what is left.

**The headline:** the two privacy reports were real, and worse than one screen.
`READ_CONTACTS`, `READ_MEDIA_IMAGES` and `READ_EXTERNAL_STORAGE` were declared
in the manifest and **never used by any code** — the pickers are system UIs that
need no permission. So the app asked for three sensitive permissions it did not
need, while appearing to bypass them. All three are removed, both pickers now
explain themselves before opening, and the permission screen says so.

**Eleven fixes, plus four bugs found by testing that the rider had not seen** —
including Back from the Dashboard landing on the finished Permissions screen.

**Still open:** app localization (421 strings still hardcoded — the Language row
is disabled and says so), the glass/animation design items, and the
service-centre directory, which needs real data the rider has to supply.

---

## ▶ 15 August 2026 handoff

**Where we are:** all 23 screens built and reviewed twice. **All 23 images are
processed and wired into the app**, and every one has been seen rendering on a
running emulator. 70/70 tests pass, debug APK builds.

### Do these next, in order

1. ~~**Verify the vehicle photo on screen.**~~ **DONE 15 August.** Selected
   Access 125 / Solid Ice Green on the emulator: the photograph renders in the
   model picker, in Create Profile at full size, and on the Dashboard hero
   standing on its lit plate.
2. ~~**Process the other 15 images.**~~ **DONE 15 August.**
   `tools/images/prepare_scenes.py` — 111 MB of PNGs → **707 KB of WebP**.
3. ~~**Wire the images into the screens.**~~ **DONE 15 August.** All 14 in-app
   images placed and walked on the emulator in both themes.
4. **Test on the phone.** ⭐ **The only remaining step.** Nothing since
   13 August has been on hardware, and everything BLE is still untested.

### APK size — answered, and it is not a problem to fix

74 MB, and **the images are not the cause**: the whole of `drawable-nodpi` is
**1.16 MB** (657 KB vehicles + 532 KB scenes). The APK is **~71 MB of
`classes*.dex`** — an unminified debug build of Compose + Firebase + Hilt.
R8 in a release build is what shrinks that, so there is nothing to hunt down
here. Re-measure on the first `assembleRelease`; do not spend time on it
before then.

Breakdown, if it is ever needed again:

```
41.66 MB  classes.dex        11.44 MB  classes16.dex
 7.78 MB  classes17.dex       6.79 MB  classes18.dex
 1.16 MB  res/drawable-nodpi  0.57 MB  resources.arsc
```

### The images

**Originals** live at `C:\Users\eshwa\Downloads\App images\Images` (23 files,
161 MB) and **must not be edited** — the rider also has them in their Gemini
chat and can re-download. Everything is generated *from* them into the app.

All 23 came out excellent: photoreal, correct aspect ratios, unbranded
vehicles, dark studio with the cyan rim that matches the app.

**The 8 vehicles are done**: background removed with `rembg`
(`isnet-general-use` — `u2net` left a grey patch in the scooter's step-through
gap), trimmed to the subject, a soft contact shadow added so they sit rather
than float, resized to 900px and encoded WebP. **49 MB → 657 KB.** They are in
`frontend/app/src/main/res/drawable-nodpi/`.

**Known trade:** a photograph carries its own paint, so the vehicle no longer
recolours with the rider's chosen colour. `VehicleArtwork` documents this. The
chosen colour still drives the accents around it. Models without a photo fall
back to the original line art, which does recolour.

### 3D / 360° — decided and closed

**Dropped.** A static rendered still gives ~90% of the impact for ~5% of the
work, and the Gemini stills beat every 3D route tried. Do not reopen this
before 30 August.

What was tried, so nobody repeats it:
- **Scripted Blender scooter** (`tools/blender/build_scooter.py`) — free and
  works, but blocky.
- **Meshy / Tripo** — good quality, **export is paywalled** on free tiers.
- **Krea** — free download and the only one that gave a usable GLB. But the
  mesh came in as **1,280 disconnected shells**: a surface reconstruction, not
  a modelled bike, with invented geometry at the rear where the AI could not
  see. Rendering tricks improve how it reads; they cannot add detail.
- **Local ComfyUI + Hunyuan3D** — ruled out on hardware: the laptop has an
  **RTX 3050 with 4 GB VRAM**, and Hunyuan3D-2 wants ~12 GB.
- **Untried and still the best free 3D route if it ever matters:**
  `huggingface.co/spaces/tencent/Hunyuan3D-2mv` — official, free, no paywall,
  takes 4 view images which is what fixes the invented-rear problem.

The tooling all still works if needed:
- `tools/blender/build_scooter.py` — generates a scooter from scratch
- `tools/blender/turntable_from_model.py` — 36-frame turntable from any
  GLB/FBX/OBJ, with `--clean` to repair AI meshes, `--bg` to judge on the app's
  dark card, and `--save-blend` to write a scene you can open
- `tools/blender/gixxer_sf_scene.blend` — opens ready to look at
- Blender 5.2 is installed and runs headless; **no MCP addon needed**

### Figma review — 15 August

Figma delivered roughly 70% of the brief. Export reviewed at
`Design system and onboarding flow updated.zip`.

**Done:** motion system with real numbers (`SPRING_SNAPPY` 380/32,
`SPRING_GENTLE` 220/26, `SPRING_SHEET` 260/30, `EASE_OUT_EXPO`
[0.16, 1, 0.3, 1]); press feedback at scale 0.96 — matching what was already
built; screens expanded 13 → 23; quick actions restructured 2-big-4-small as
built; MILEAGE tile; navigation background as an animated conic gradient;
fonts wired (Outfit / DM Sans / JetBrains Mono).

**Missing:** accent colour picker (we built it anyway — 8 colours); the 3D
hero is a CSS `rotateY` on a flat SVG, not a turntable; **no photographs at
all**; no type sheet or component sheet; `guidelines/Guidelines.md` is the
untouched Figma boilerplate.

**Ignore `STATUS_REPORT.md` in that zip** — dated 25 July, describes 13
screens, and does not match the build.

**Worth porting:** those four motion constants into Compose. Not done yet.

---

## Done and verified

### 15 August 2026 — the photographs are in the app

The remaining 15 images processed and all 14 in-app slots wired, then every one
walked on a running emulator in both light and dark. 70/70 tests still pass.

**Processing — `tools/images/prepare_scenes.py`** (new; the companion to
`prepare_vehicles.py`, which stays as it is). Scenes are not cut-outs, so it
never touches `rembg`: it crops, resizes and encodes, nothing else.
**111 MB → 707 KB.**

- **The Gemini watermark is at a fixed offset, and that solves it exactly.**
  Measured across all 23 originals, the ✦ sparkle occupies the same absolute
  box every time — its outer edge is **95 px from the right edge and 95 px from
  the bottom**. Cropping a 96 px band off those two sides removes it on every
  image with no inpainting and no artefacts. (Worth knowing if more images are
  ever generated: it is not a per-image search, it is one constant.)
- Each image is then cropped to the ratio its slot expects, centred on the
  *original* centre so the watermark band coming off one side does not drift a
  centred subject, and sized to what the slot actually displays.
- **`img_promo_keyart` is written to `brand/store/`, not to `res/`.** It is Play
  Store artwork and has no slot in the app, so it must not sit in the APK.

**Wiring — new `presentation/components/Photo.kt`.** Three composables:
`RcxPhoto` (sized by its own ratio), `RcxPhotoFill` (fills a given box, with an
optional `darken`), and `RcxHeroBanner` (the fixed-height band that opens a
feature screen, fading into the screen background).

| Screen | Placement |
|---|---|
| 02 Welcome | full hero, the design's fade and LIVE chip kept on top |
| 03-05 Onboarding | full illustration slot, fading into the title block |
| 10 Permissions | header band with the title on the photo |
| 13 Dashboard | the lit plate **behind the vehicle**, so it stands on the light |
| 14 BLE Pairing | inside the "Nothing found nearby" card |
| 15 Navigation | full-bleed backdrop, dimmed 72%, design grid over it |
| 16 Statistics · 18 Service · 19 Safety | hero banner above the first card |
| 17 Notifications | replaces the icon tile in the empty state |
| 21 Profile | cover banner with the avatar overlapping it |
| 23 About | behind the app information card |

**Decisions worth keeping:**

- **The Figma vector illustrations were not deleted.** `WelcomeHero` and
  `ObIllustration` now draw photographs; the artwork they replaced is still
  there as `WelcomeHeroVector` / `ObIllustrationVector`. The design is fixed and
  the photographs are an addition on top of it, so going back is one line at
  each call site. Nothing here is in git yet, which is the other reason not to
  delete it.
- **Nothing went behind the SOS button.** The safety photograph sits *above*
  the emergency card, not behind it — the SOS control is the one thing on that
  screen that has to be unmistakable.
- **The pairing photograph carries no caption.** It illustrates the "nothing
  found" card, but how the Access cluster is put into pair mode is not recorded
  anywhere in this project, so no instruction was written for it. Inventing one
  would cost the rider a trip to the scooter. See "Still open" below.
- Two tunings after seeing them on the device: the safety band is 170 dp
  (at 132 the 16:9 crop took the helmet's top and bottom off until it read as
  an abstract shape), and the pairing photograph is dimmed 18% — it is the only
  daylight image in the set and arrived as a bright rectangle on the dark theme.

**Still open from this:** the pair-mode procedure itself. The original app
explains it with an arrow; if the rider describes it, the card can carry the
real instruction under the photograph.

### 13 August 2026 — second review pass (items 10-12)

Walked on the emulator afterwards; no crashes, 70/70 tests pass.

**Safety (10).**
- **Shared location was landing streets away.** The cause was accepting any
  cached fix — one can be an hour old and half a kilometre out. A cached fix is
  now only used when it is **under a minute old and accurate to 30 m**;
  otherwise the GPS is polled and the **most accurate fix of the wait** is
  kept, not the first one to arrive. The message carries its own accuracy
  ("accurate to about 12 m") so nobody reads a vague position as a doorstep.
  If the rider granted **Approximate** instead of Precise, the SOS sheet says
  so and links to the one place it can be changed.
- **The expanding circle on the accident card** was the Material ripple
  fighting the size change. Ripple off, `animateContentSize` on.
- **Contacts can be picked from the phone** — `ACTION_PICK` on the phone-number
  URI, which needs **no `READ_CONTACTS` permission**: the system grants access
  to the single chosen row. Typing a number by hand is still there, secondary.
- **Capped at 3 contacts**, enforced in the repository.

**Service (11).** Upcoming tasks were a hardcoded enum of four, which is why
nothing could be added, edited or removed. They are a Room table now (db v6),
seeded with the same four on first run so nothing looks empty. Every row opens
an edit sheet with a Remove button, and "Add a task" is part of the list. Icons
are matched from the task's name — "Air filter" gets a filter, anything
unrecognised gets a wrench.

**Settings, Appearance, About (12).**
- **Account card at the top of Settings**, opening Profile. The screen having
  no sign of who was signed in is why Profile felt missing.
- **Accent colour is real** — eight colours, applied instantly across the whole
  app. Each carries a dark and a light value, because a hue that reads on navy
  is too pale on white. Verified: picking Violet re-tinted every control.
- **Language**: Phone language / English / తెలుగు / हिन्दी.
- **Removed**: the whole Navigation section (Maps owns every one of those
  settings and changing them here did nothing), "Default navigation app",
  "Marketing notifications", "Reset all settings", the changelog, open-source
  licences, and the support section including the personal email.
- **Auto connect and auto reconnect merged** into one switch — they described
  the same behaviour.
- **"Background connection" moved to Permissions**, where it belongs.
- **New Permissions section** in Settings.

**Still open from item 11:** reading a service booklet from a photo. Attaching
the photo is straightforward; reliably parsing an arbitrary booklet into
structured records is not, so it should assist rather than pretend to be
automatic — OCR extracts text, the rider confirms. Not started.

### 13 August 2026 — polish pass from the rider's review

Nine items raised after reviewing the app on the phone. Built, compiled,
70/70 tests passing. **Installed on the phone? Not yet** — it disconnected
before the install; the APK is built and waiting.

**Delivered as documents (for the rider to act on):**
- `docs/design/Image-Prompts-Gemini.md` — 23 photographic image prompts, one per
  slot in the app, each with a file name, aspect ratio and the screen it
  belongs to. All describe **unbranded generic** vehicles: a prompt cannot
  make a protected vehicle design unprotected, so asking for a real model
  would produce something the rider does not own. Icons are excluded
  deliberately — Material Symbols is already bundled, vector and Apache 2.0.
- `docs/design/Figma-Brief.md` — the whole polish brief: locked colour and spacing
  tokens, the font problem (Outfit / DM Sans / JetBrains Mono are specified
  but were never bundled, which is why text looks "boxy"), a motion spec to
  write, the Dashboard 3D hero, and the Navigation background.

**The 3D 360° vehicle, free:** the answer is a **pre-rendered Blender
turntable** — 36 frames at 10° each, swapped as the rider drags. Looks like
real 3D, needs no 3D engine on the phone, costs nothing. Real-time (Filament /
SceneView) would need a `.glb` per vehicle for no visible gain at this size.

**Code changes:**
- **Google sign-in said "no accounts found" on the first tap.** Two causes,
  both fixed: `setAutoSelectEnabled(true)` was asking for a *silent* sign-in,
  and with nothing previously authorised Credential Manager answers
  `NoCredentialException` instead of showing UI. And one pass cannot serve
  both cases — it now asks for authorised accounts first, then falls back to
  showing every account. "No accounts" is only reported after both come back
  empty.
- **Permissions now fire automatically**, in sequence
  (notifications → Bluetooth → location), the moment the screen appears —
  as the official app does. Android allows one dialog at a time, so the
  sequence advances on each result.
- **New Permission details screen** (Settings → Privacy → Permissions):
  every permission with a plain sentence about what it enables, its live
  state, and a working control. Modelled on the original app's "Application
  Permission Requirements" page. Contacts and photos added to the catalogue.
- **`READ_SMS`, `READ_CALL_LOG` and `CALL_PHONE` deliberately not added.**
  The official app takes all three. Ours lights the message and missed-call
  lamps from **notification access**, which is already built — the same
  information from a less invasive permission, and it keeps the app clear of
  Play's restricted-permission review. There is a card on the permissions
  screen that says so.
- **Quick actions restructured**: two large tiles (Navigate, Pair) and four
  small icon tiles (Stats, Safety, Service, Profile), with press-scale
  springs. The four equal rows were competing with the vehicle for attention.
- **Pairing screen**: "Skip for now" removed — the screen is only ever opened
  deliberately, so there was nothing to skip. A finished 30-second scan with
  no result now shows "Nothing found nearby" with the three things actually
  worth checking, and a **Scan again** button.
- **Mileage prediction** (new feature): the scooter transmits no fuel economy,
  so it is derived from how far the vehicle travels as the five-segment fuel
  bar drops. Room table `fuel_samples` (db v5) stores only the transitions.
  The Dashboard card states its confidence and how many readings it is
  averaging, and shows an estimated range. Refuels, stationary readings and
  implausible jumps are all excluded — see `MileageCalculatorTest`.
- **`RcxImagePlaceholder`** — a labelled dashed box naming the exact image
  file each slot is waiting for, so the layout is right before the
  photographs exist.

### 13 August 2026 — screens 18-23, the app is now 23/23

Built from `figma design explanation.md` (lines 3640-5061). **Walked end to end
on the emulator the same day** — see `docs/testing/Emulator-Test-Report-2026-08-13.md`
for what was exercised and what broke. 60/60 unit tests pass (was 31).

- **18 Service.** Room `service_records` (db v3) + `ServicePreferencesStore`.
  Next service is computed from the rider's own records — the cluster has no
  service counter — as 3,000 km / 90 days from the last one, whichever comes
  first. Progress bar shows whichever of distance and time is further along.
  Add/edit/delete records, with the design's rules enforced: no future dates,
  and an odometer reading may never go below one already recorded. The odometer
  is cached from telemetry so the screen still works with the scooter out of
  range.
- **19 Safety.** Room `emergency_contacts` (db v4, unique index on the
  normalised number so duplicates cannot get in) + `SafetyPreferencesStore`.
  Pulsing SOS button, first-aid guide, contacts with one-tap call, and an
  optional "share my location" that builds a maps link from a real fix.
  **Calls go through `ACTION_DIAL`, never `ACTION_CALL`** — no `CALL_PHONE`
  permission, and the app can never dial by itself. The sheet says so out loud.
- **20 Settings.** `AppSettingsStore` (DataStore) covering General, Bluetooth,
  Navigation, Notifications, Privacy and About. Every toggle writes
  immediately. Rows the app cannot honour — language, default nav app,
  marketing — are shown disabled with the reason, not hidden. "Service
  reminders" reads the *Service* store rather than a second key, so the two
  screens cannot disagree. Forget-vehicle disconnects **and** clears the stored
  address, or auto-reconnect would just find it again.
- **21 Profile.** Account card (guest badge, email only for real accounts),
  vehicle card with live connection state, edit profile, change vehicle, sign
  out, delete local data. All writes on `@ApplicationScope`.
- **22 Appearance.** Theme (system/light/dark) and text size, applied instantly
  from `MainActivity` with no restart. Text size **multiplies** the phone's
  accessibility scale rather than replacing it. Accent colour shown disabled —
  the design marks it Future.
- **23 About.** Version and build read from `BuildConfig` (`buildConfig = true`
  added to the Gradle file). What's new, changelog, licenses, privacy, terms,
  and a support mail intent that pre-fills device and version details.

Entry points added: the Dashboard header gained a settings gear, the greeting
opens Profile, Safety joined Quick Actions, and the Service Reminder card from
the Dashboard spec is now built. Notification rows for SERVICE and SAFETY now
open their screens instead of doing nothing.

**Fixed the same day, after the emulator pass:**

- **A mistyped service record could not be undone.** The reading was written
  into the odometer cache, which only moves forward, so deleting the record
  left the app permanently "Overdue". Manual readings no longer touch the
  cache — it stays what the *vehicle* reported. The record still raises the
  current reading while it exists, via `status`.
- **The Units setting did nothing.** Miles persisted but every distance still
  read km. `DistanceUnit` now converts and formats, carried into the cards by
  `LocalDistanceUnit`. **Distances remain stored in kilometres** — converting
  on the way in would corrupt the data the first time the rider changed units.
- **The pre-filled odometer field appended instead of replacing** (typing 900
  over a pre-filled 1602 stored 160,290 km). Selecting the text on construction
  and again on focus both lost the race with the tap's caret placement, so the
  pre-fill was removed; the last known reading is shown under the field instead.
- **The splash claimed v2.5.0** while About read 1.0. It reads
  `BuildConfig.VERSION_NAME` now.

### Suzuki protocol
- Checksum solved by sweeping ~25,000 candidates against two captured packets,
  then **confirmed against Suzuki's own function**. Model-dependent branch
  auto-selected via `ProtocolEngine.configureForDevice(deviceName)`.
- `0x31` navigation and `0x36` profile encoders match the official builders.
  Navigation encoder reproduces a captured packet byte-for-byte (pinned as test).
- Real GATT write path on `00000001`, with the official app's
  `services[3]/characteristics[0]` index fallback.
- `sendPacket` reports what actually happened — it never fakes success.

### BLE lifecycle
- `connectGatt` with explicit `TRANSPORT_LE` (3-arg overload defaults to
  `TRANSPORT_AUTO`, which picks BR/EDR on this dual-mode cluster → status 133).
- All GATT calls on the main thread; status-133 retry with close + settle delay.
- Replay cache cleared and subscription made *before* connect, so a stale
  `DISCONNECTED` is not replayed into a fresh attempt.
- Profile packet waits on `servicesReady` — sending on `STATE_CONNECTED` fired
  before the write characteristic existed (`sent=false` on first hardware run).
- Profile sent on a **repeat timer**, mirroring `C4956y.run()`.
- Auto-reconnect to last vehicle; disconnect action; GATT closed on
  `onDestroy`/`onTaskRemoved`; service `START_NOT_STICKY`.
- **Crash fixed:** `connectedDevice` foreground service without Bluetooth
  permission threw `SecurityException` out of `onCreate` and killed the process.

### Navigation
- `MapsNotificationListener` reads Maps' turn-by-turn notification → parses
  instruction / distance / ETA → `NavigationRelay`. Registered and bound.
- Notification-access gate with a button to the settings page.
- Relay status distinguishes *connected* from *actually relaying*.

### Auth
- Google, email/password, guest all work.
- **Fixed:** email sign-up never reached Firestore. `AuthViewModel` is scoped to
  the Sign In nav entry; creating the user flips auth state → navigates →
  cancels `viewModelScope` mid-write. The email path has one more suspension
  point than Google (`updateProfile`), so it lost the race every time.
  Persistence now runs on `@ApplicationScope`.
- **Fixed:** `UserPreferencesStore` never stored the Firebase uid, so
  `syncsToCloud` was always false and cloud writes were silently skipped.

### Permissions
- Detects whether the Bluetooth radio and location services are actually **on**,
  not merely permitted, and offers a button that opens the system UI.
  (Granting permission never turns a radio on.)
- Continue stays secondary until both are genuinely on.

### Create Profile
- Name, city with GPS crosshair, type → model → colour sheets, artwork in the
  chosen paint, T&C gate.
- One "Read them" link → one page with both documents. No "Last updated".
- **Google sign-in auto-fills name and picture** (both editable). Email and guest
  start empty — deliberately; that name is what the cluster greets them by.
- **Photo:** every format including HEIC/WEBP/AVIF via `ImageDecoder`, working
  copy capped at 2400px. `PhotoCropDialog` lets the rider drag/pinch inside the
  real avatar circle; `saveCropped` replays that exact transform at 1080px.

### Dashboard
- Battery card removed — phone battery belongs on the cluster, and only one
  Suzuki model is electric.
- **Fixed:** Pairing said "connected" while Dashboard said "offline".
  `connect()` returned a cold per-caller flow, so each screen only saw a
  connection it started itself — and the Dashboard's was never started.
  `BleRepository` now exposes one app-wide `StateFlow<ConnectionState>`.

### 4 August 2026 — account restore
- **Signing in no longer forgets you.** The cloud copy was always written and
  never read back, so reinstalling and signing in with the same Google account
  demanded a fresh profile every time. `fetchUser` existed, with a comment
  saying it restored the account, and nothing called it.
- Restores rider name, city, vehicle and colourway, then shows an **Account
  found** screen with those details: continue, or set up a new profile. Not
  adopted silently, not discarded silently.
- Parsing extracted to `CloudProfile` so the key names and fallbacks are
  testable without Firestore — `riderName` beats the provider's `name`, and a
  name without a vehicle does *not* skip setup (the dashboard renders the
  vehicle, so that would land the rider on a broken screen).
- **Bug this exposed:** `signInWithGoogle` ran `persist()` on the caller's
  scope, not `@ApplicationScope`. It got away with it while persist was two
  quick writes; adding the profile read gave the navigation time to clear the
  AuthViewModel mid-write and Firestore failed with `JobCancellationException`.
  The same defect that broke email sign-up, still present in the Google path.
  Now fixed for both.
- Verified end to end on the emulator with a real Google account: sign in →
  create profile → wipe app data → sign in again → **Account found**, showing
  Access 125 / Solid Ice Green → dashboard intact.
- **Renaming a profile did not reach Firebase.** `syncSelectionToCloud` read the
  session and selection on the *caller's* scope and only wrapped the writes in
  `appScope`. `saveProfile` navigates away as it returns, clearing the ViewModel
  — if either read was still suspended the write was never queued, so the cloud
  kept the old profile while the device showed the new one. **The same mistake
  as email sign-up and Google sign-in, third occurrence.** Whole function now
  runs on `appScope`, with a success log:
  `RCX-Vehicle: Profile synced: riderName='…'`.
- "Set up a new profile instead" left the previous vehicle and colour selected,
  so starting over arrived with the old scooter already chosen. Now cleared.

**One profile per account, by design.** Firestore stores `users/{uid}`, so a
second profile on the same Google account replaces the first — the screen says
so before the rider commits. Multiple profiles per account would be a new
feature; the rider has not asked for one.

### 4 August 2026 session
- Maneuver codes calibrated on hardware; `ProtocolEngine.Maneuver` rewritten
  from observation, with unobserved cases (sharp, keep, merge, destination)
  falling back to the nearest confirmed arrow rather than to a blank code.
- `parseTelemetry` was a placeholder reading speed from byte 2 and fuel from
  byte 4 — pure invention. Replaced with the real 9/6/6 ASCII split, rejecting
  malformed frames so a bad packet leaves the last good reading standing.
- Dashboard now shows ODO / Fuel / Trip A / Trip B. Speed card removed: the
  scooter does not send speed, so a permanent "—" was worse than nothing.
- Profile byte 27 driven by the real same-cluster-as-last-time rule;
  `previousClusterName` persisted in `SessionDataStore`.
- `buildHeartbeatPacket` written — there was **no 0x33 sender at all**, which is
  why phone battery, signal and the message lamps never appeared. Repeats every
  5 s while connected, stops on shutdown.
- `PhoneStatusProvider` (battery bucket, charging, signal bars, clock) and
  `ClusterAlerts` (message / missed-call flags). The notification listener now
  also watches WhatsApp / SMS / call notifications, skipping ongoing ones so a
  music player cannot pin the lamp on.
- `READ_PHONE_STATE` added as an **optional** permission — refused, the
  heartbeat reports zero bars and everything else still works.

### Tests — 60/60 passing (13 August 2026)
- `ServicePlanTest` (12) — service schedule arithmetic: overdue by distance and
  by time, progress taking whichever is further along, an unknown odometer
  staying unknown rather than reading a confident zero, and the km↔mi
  conversion at the exact 1.609344 ratio.
- `SafetyRepositoryTest` (6) — phone normalisation (four spellings of one
  number collapsing to a single key, which is what the unique index rests on)
  and the 7-15 digit length rule.

### Tests — 30/30 passing
- `MapsNotificationParserTest` (13) — every maneuver type, imperial units,
  hour+minute ETA, unknown-phrasing fallback, ignoring non-navigation notices.
- `ProtocolEngineTest` (12) — checksum against both real captures, a guard that
  the old plain-sum cannot pass, byte-for-byte reproduction, raw-byte maneuver
  position, status chars at 23-24, padding/clamping, profile layout, model-based
  checksum branch.

---

## The mistake that keeps recurring — check for a fourth

**Work that must outlive a screen cannot be started on that screen's scope.**
Three separate bugs, one cause, found weeks apart:

1. **Email sign-up** — Firestore write cancelled by navigation; email users never
   appeared in the database while Google users did.
2. **Google sign-in** — `persist()` on the caller's scope. Survived only because
   it was fast; adding the profile read made it slow enough to be cancelled.
3. **Profile save** — `syncSelectionToCloud` read session and selection on the
   caller's scope, so a rename silently never reached the cloud.

Anything that writes to Firestore, DataStore or BLE after a user action that
also navigates should run on `@ApplicationScope`. When adding one, check the
*whole* function, not just the write: suspending reads before the write are
equally exposed. Symptom to watch for:
`JobCancellationException: Job was cancelled`.

## Traps that have already cost time

- **`applicationId` ≠ namespace.** Launch with
  `com.gnyaneshwar.rideconnectx/com.eshwar.rideconnectx.MainActivity`. A stale
  install under `com.eshwar.rideconnectx` can linger and be mistaken for the
  current build.
- **Only one app can hold the scooter.** The official Suzuki app takes exclusive
  BLE access and *respawns itself* (Android restarts its notification listener).
  Force-stop is not enough — uninstall or disable it while testing.
  **The rider needs it for real journeys; never leave it uninstalled.**
- **Stale incremental builds.** `NoClassDefFoundError` on
  `RideConnectXApp_GeneratedInjector` = Hilt's generated code missing.
  `./gradlew clean` fixes it.
- **A build killed mid-dex packages an APK that installs and then crashes.**
  Seen 17 August: `NoClassDefFoundError` on `core/di/ServiceEntryPoint`, a
  plain *source* class. The OOM-killed build had finished `compileDebugKotlin`
  but not dexing, so Gradle reported that task `UP-TO-DATE` and shipped a
  **partial dex**. Give-away: the class is in source but not in the dex, and
  the APK is slightly small (76.3 MB vs 76.7 MB after `clean`). Same remedy —
  `./gradlew clean` — as the Hilt case above.
- **`widthIn(max = …)` after `fillMaxSize()` does nothing.** `fillMaxSize()`
  pins the *minimum* width to the parent, so a later cap cannot shrink it. Cap
  first, then fill: `.fillMaxHeight()` … `.widthIn(max = 600.dp)`
  `.fillMaxWidth()`. This was wrong on all 20 screens and invisible on a phone
  — only a display wider than 600dp reveals it.
- **Watch free RAM around the emulator, not just the build.** 15.4 GB total;
  the emulator takes ~4 GB and the build daemons ~1.9 GB, and running both drops
  free memory to ~0.3 GB, which is what OOM-killed the 16 August build.
  **`./gradlew --stop` does not stop the Kotlin compile daemon** — that is a
  separate `java.exe`. Shut the emulator down before a clean build.
- **Tablet layout can be checked without a tablet AVD:**
  `adb shell wm size 2560x1600` + `adb shell wm density 276` (≈1484dp wide),
  then `adb shell wm size reset` / `wm density reset` afterwards.
- **Hilt + Kotlin 2.2.** `@Inject` fields in Activities/Services break
  `hiltJavaCompileDebug`. Use `EntryPointAccessors` with `ServiceEntryPoint`.
- **Emulator has no BLE radio** — adapter sits in `BLE_TURNING_ON` forever.
  Its Bluetooth stack also aborts natively (`libbluetooth_jni.so`) during a
  session. That crash is the emulator's, not the app's; do not chase it.
- **`adb` is not on PATH.** It lives at
  `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`.
- **Do not pipe `adb exec-out screencap -p` into a PowerShell redirect** — it
  corrupts the PNG. Use `adb shell screencap -p /sdcard/x.png` then `adb pull`.
- **Back dismisses a `ModalBottomSheet`, not just the keyboard.** Sending
  keyevent 4 to close an IME during a test throws away whatever was typed.
- **Location services off blocks BLE scanning** entirely (`status=147`).
- **Firebase is configured and working.** Do not recreate the project.

---

## Rejected, with reasons

**"Display over other apps" / accessibility to read Maps directly.** The Maps
notification is already accurate — the parser handles left/right/slight/sharp/
U-turn/roundabout, proven by 13 tests. The inaccuracy is entirely in *which
number the cluster wants*. An overlay would feed the same correct instruction
into the same wrong lookup. It also costs an intrusive permission and breaks
whenever Google restyles Maps. Revisit only if the notification proves to lag or
drop maneuvers on a real ride.

**Guessing the maneuver codes again.** Unverified codes were previously gated
behind a flag that suppressed *every* turn except straight — which is why no
directions appeared at all. The gate is gone; the codes are now source-derived,
and the remaining ambiguity gets resolved by observation, not another guess.

---

## Not in git yet

Deliberately. The rider wants a working, complete app before the first push.
Nothing here is pushed.
