# RideConnectX — Master Knowledge Base

Consolidated from months of prior reverse-engineering (JADX decompile of the
official Suzuki app, nRF Connect live discovery, logged packet captures) plus
the work done in this repository.

**Do not repeat work recorded here.** Only the items under
[Open problems](#open-problems) genuinely need the scooter.

---

## 1. What RideConnectX is

Not another navigation app. A **bridge**.

```
Google Maps  →  RideConnectX  →  BLE  →  Suzuki cluster
 (routing)      (translation)          (turn-by-turn display)
```

Google Maps keeps doing what it is good at — search, routing, traffic, ETA,
re-routing, voice. RideConnectX reads its guidance and speaks Suzuki's BLE
protocol to the dashboard.

The goal is to replace the official Suzuki Ride Connect app (which uses Mappls
navigation) while keeping the cluster working exactly as it does today.

**Primary vehicle:** Suzuki Access 125 with Suzuki Smart Connect System (SSCS).

### Product principles
- Feel premium; behave like an OEM app.
- Pair once, then never think about pairing again.
- Never implement protocol assumptions — only confirmed behaviour.
- Unknown packets: log, never crash, never guess.
- Build architecture → foundation → feature → QA. Never feature-first.

---

## 2. Environment

| Thing | Value |
|---|---|
| Scooter BLE name | `SAS210217219` |
| Scooter BLE address | `74:02:E1:5C:5C:0F` |
| Test phone | OnePlus Nord CE 3 5G (`CPH2569`), Android 15, adb id `fe7d8c39` |
| Official app package | `suzuki.com.suzuki` |
| Analysed APK | `C:\Users\eshwa\Downloads\Mobile Devices\Suzuki Ride Connect.apk` |
| APK SHA-256 | `C7E3466BEEA3D9512C5B6F79A79377389B1BBC57DFC6986E43EFA727E729F5CF` |
| Decompiled sources | `C:\Users\eshwa\suzuki-ride-connect-analysis\jadx-code\sources` |

### This project
| Thing | Value |
|---|---|
| applicationId | `com.gnyaneshwar.rideconnectx` |
| Kotlin namespace | `com.eshwar.rideconnectx` |
| Firebase project | `rideconnectx-89528dd3` (Mumbai, production mode) |
| Auth enabled | Google + Email |
| Min SDK | API 28 |

> The applicationId and the namespace differ on purpose. Firebase is registered
> against the **applicationId**. Launch with:
> `adb shell am start -n com.gnyaneshwar.rideconnectx/com.eshwar.rideconnectx.MainActivity`
> An older install under `com.eshwar.rideconnectx` may linger on a device and is
> easy to mistake for the current build — uninstall it if present.

---

## 3. Official app internals (from JADX)

| Class / member | Role |
|---|---|
| `com.suzuki.services.MyBleService.f(byte[], int)` | **Final BLE transmit.** Logs `Data Packet-Main`, then FastBLE write. |
| `com.suzuki.application.fragment.C0940y.run()` | Periodic status packet builder. Logs `Data Packet-Status`. |
| `com.suzuki.services.NotificationService.o(...)` | Builds the 30-byte notification packet. |
| `com.suzuki.services.NotificationService.p(...)` | Missed-call handling. |
| `com.suzuki.activity.HomeScreenActivity.i0` | Notification flag → status byte 14. |
| `com.suzuki.activity.HomeScreenActivity.j0` | Missed-call flag → status byte 15. |
| `com.suzuki.broadcaster.CallReceiverBroadcast.e(...)` | Sets `j0`. |
| `com.suzuki.broadcaster.IncomingSms.c(String)` | Sets `i0`. |
| `com.suzuki.application.fragment.C0915i0` | Search / navigation UI fragment. |
| `com.clj.fastble` | Obfuscated FastBLE — the BLE transport. |
| `com.mappls.sdk.navigation.*` | Mappls navigation SDK (what we replace with Google Maps). |

Flag values: `78 = 'N'` = present, `89 = 'Y'` = cleared.

JADX reported ~18 decompilation errors. `NotificationService.onNotificationPosted`
could not be reconstructed — smali would be needed if that path ever matters.

---

## 4. BLE — confirmed

### GATT (live, via nRF Connect)

Service **`0xFEFB`**

| Characteristic | Property |
|---|---|
| `00000001-0000-1000-8000-008025000000` | WRITE NO RESPONSE |
| `00000002-0000-1000-8000-008025000000` | NOTIFY |
| `00000003-0000-1000-8000-008025000000` | WRITE |
| `00000004-0000-1000-8000-008025000000` | INDICATE |

The official app does **not** hardcode these. It discovers by index:
`gatt.getServices().get(3)`, characteristic `0` = write, `1` = notify.
It requests **MTU 250**. CCCD is the standard `00002902-…`.

RideConnectX matches by UUID first and falls back to those indices.

> **The write target is `00000001` (WRITE_NO_RESPONSE), not `00000003`.**
> `MyBleService.f()` uses `getCharacteristics().get(0)`, which is index 0 =
> `00000001`. Writing to `00000003` is accepted by the BLE stack and silently
> ignored by the cluster — confirmed on hardware. Getting this wrong is what made
> the dashboard show nothing for two whole sessions.

### Frame

All command/status packets are **30 bytes**:

```
byte 0     0xA5   start
byte 1     type
byte 28    checksum
byte 29    0x7F   end
```

### Checksum — SOLVED and source-confirmed

`SuzukiApplication.m8569a(byte[])`:

```java
byte b = 0;
for (b2 = 1; b2 <= 27; b2++) b += bArr[b2];
return C3055K.f9282g ? (byte)(255 - (b % 256)) : (byte)(b % 256);
```

**Two branches, selected by vehicle model** from the advertised BLE name in
`DeviceListingScanActivity`:

| Name chars 1-4 | Model | Checksum |
|---|---|---|
| `AS21` / `AS01` / `AS02` | Access, Access 125 | `255 - sum` |
| `AS11` / `AS12` | Burgman Street | `255 - sum` |
| `CE..` | e-ACCESS | `sum` |
| `CS0.` / `CS1.` | Access-TFT / Burgman-TFT | `sum` |

`SAS210217219` → `AS21` → **Access → complement branch**.

First recovered by sweeping every plain-sum, XOR and CRC-8 variant against two
captured packets (`0x31` → `0xD9`, `0x37` → `0x61`); only one rule matched both.
Later confirmed verbatim against the source above. Implemented as
`ProtocolEngine.configureForDevice(deviceName)`; pinned in `ProtocolEngineTest`.

### Packet types

| Type | Meaning |
|---|---|
| `0x06` | Notification (`W` WhatsApp, `N` SMS, `X` calendar) |
| `0x31` | Navigation / turn-by-turn |
| `0x33` | Heartbeat — carries the `i0`/`j0` flags |
| `0x36` | Rider profile — **source of the cluster's WELCOME line** |
| `0x37` | Scooter → phone telemetry |

### 0x31 navigation layout

Every field is ASCII, not binary.

> **Corrected from the official builder** (`ViewOnClickListenerC4857A0.m8576D`).
> The maneuver code is a **raw byte at index 2**, not two ASCII digits at 23-24.
> Bytes 23-24 are the GPS / airplane status characters.

| Bytes | Meaning | Example |
|---|---|---|
| 0 | `A5` | |
| 1 | `31` | |
| 2 | **maneuver code, raw byte** | `28` = 40 |
| 3 | `FF` | |
| 4–8 | trip metadata | `0100M` |
| 9–14 | clock | `0613PM` |
| 15–17 | spacer | `FF FF FF` |
| 18–22 | distance to maneuver | `0299M` |
| 23 | GPS status: `0` airplane, `4` GPS off | `1` |
| 24 | nav active | `1` |
| 25–27 | spacer | `FF FF FF` |
| 28 | checksum | `D9` |
| 29 | `7F` | |

Reference capture:
```
A5 31 04 FF 30 31 30 30 4D 30 36 31 33 50 4D FF FF FF
30 32 39 39 4D 31 31 FF FF FF D9 7F
```

Distances seen: `0184M 0299M 0296M 0294M 0309M 0476M 0468M 0489M 0623M`
Clocks seen: `0553PM 0556PM 0557PM 0604PM 0613PM`

### ✅✅ 18 August 2026 — the APK inference is CONFIRMED. 8/8 predictions correct.

Swept on the rider's Access 125 with the ignition on, one code at a time, each
held on the cluster until answered and **photographed**. `dist` was set equal to
the code so the cluster printed its own code number — every photo is
self-labelling and no answer can be mis-attributed.

**Every single prediction from `Suzuki-APK-Reference.md` was right.** The icon
geometry decode is therefore trustworthy for the codes not yet swept.

| Code | Cluster draws | Predicted? |
|---|---|---|
| 1 | **turn left** — road-shaped: vertical shaft, then bends left | ✅ |
| 8 | **straight** — plain up arrow | ✅ |
| 15 | **down-right** diagonal arrow | ✅ |
| 16 | **straight down** arrow (reverse / back) | ✅ |
| 17 | **down-left** diagonal arrow | ✅ |
| 18 | **left** — flat horizontal arrow | ✅ |
| 19 | **slight left** — diagonal up-left arrow | ✅ |
| 23 | **roundabout**, exit arrow straight up through the circle | ✅ |

#### ⚠ TWO distance fields — and we are driving the wrong one

Established by the rider reading the cluster while codes were held, 18 August:

| Cluster shows | Frame bytes | Our builder calls it | Rider says it is |
|---|---|---|---|
| the number beside the **clock** | 18-22 | `distanceMetres` | **distance remaining** |
| the standalone **`0m`** before ETA | 4-8 | `speed` (+ `"0M"`) | **distance to the next turn** |

Proven: sending `dist=350` put **350m** beside the clock. Bytes 4-8 are
hardcoded `000` in `sendRawCode`, which is why the next-turn field has read
`0m` all night.

**This contradicts the note above that calls bytes 18-22 'distance to
maneuver'.** One of the two labels is wrong, and the rider is the one looking at
the dashboard. If their reading is right, live navigation currently puts the
next-turn distance into the remaining-distance slot and shows a permanent `0m`
for the turn — a real defect, not cosmetic.

**Not yet tested** (needs a test hook, so a rebuild): driving bytes 4-8 to see
the next-turn field change, and the missed-call / message lamps in the 0x33
heartbeat bytes 14-15 (no test action exists for those yet).

#### Where the sweep stopped

15 codes confirmed on 18 August. **Still unswept:** 10-14, 20-22, 24-30, and
46-53. The "46-53 draw nothing" claim still comes only from the discredited
4 August sweep and remains unverified. 10-14 is the most interesting remaining
block now that 9 turned out to be the destination marker.

---

#### The cluster has THREE arrow families, not one list

This is the structural finding, and it is what the earlier sweeps missed.

**1. Bare compass bearings — a complete 8-point set.**

```
      19 ↖    40 ↑    41 ↗
      18 ←            42 →
      17 ↙    16 ↓    15 ↘
```

Every bearing Google Maps can express now has a correctly-angled arrow. There
is no longer any need to approximate.

**2. Road-shaped junction turns at 1-9.** Code 1 is visibly *different* from
code 18: 18 is a bare horizontal arrow, 1 is an arrow that travels up a shaft
and bends left at a junction — the shape Maps itself uses. Code 8 (straight) is
the member of this family that happens to look like a plain arrow, which is why
8 and 40 appear identical.

**3. Roundabouts by exit at 20-26.** Code 23 draws the circle with a
straight-ahead exit, confirming the set is indexed by exit direction rather
than being one generic roundabout icon.

#### What this overturns

- **"There is no slight-left" was WRONG.** It is code **19**. The parser has
  been sending 37 (a full curved left) for slight-left — correct direction,
  overstated angle. That can now be exact.
- **16 is a plain down arrow, not the curved U-turn.** Code 39 remains the
  curved U-turn glyph. The cluster has *two* reverse icons and they are
  different drawings; 16 reads as "go back", 39 as a U-turn manoeuvre.
- **Roundabout guidance can be directional.** We currently send 45 for every
  roundabout regardless of exit.

Raw log: `tools/sweep-1-52-worksheet.csv` (Observed / Confirmed columns).

---

### 🚨 18 August 2026 - alert lamps: the phone is correct, the CLUSTER ignores it

Tested on hardware with full logging. **Every stage on the phone works.** The
log is unambiguous:

```
alert? pkg=com.google.android.dialer category=call ongoing=true isCall=true
Call from com.google.android.dialer who='Grand Mother' - flagging cluster
Alert pushed: who='Grand Mother' call=true 0x06=true 0x33=true
```

And the actual bytes that left the phone:

```
a5 06 43 'Grand Mother' ...            <- 0x06, 'C' = call, name resolved
a5 06 57 'Grand Mother: 3 new messa'   <- 0x06, 'W' = WhatsApp
a5 33 31 4E FFFFFF 33 '113739' 4E 59   <- 0x33, byte15 = 0x59 'Y' = missed call
```

The cluster displayed **nothing**. It did show the profile greeting, 3 signal
bars and the correct battery from the same heartbeats, so the link, the frame
and the checksum are all fine - only the alert interpretation fails.

#### Prime suspect: the flag polarity, and this file contradicts itself

| Source | Claim |
|---|---|
| Line ~84, from the **JADX decompile** | `78 = 'N'` means an alert is **PRESENT**, `89 = 'Y'` is cleared |
| Line ~563, reasoned from a **packet capture** | `'Y'` is the alert, `'N'` is idle |

`ProtocolEngine.FLAG_PRESENT` implements the second. **The 18 August test is the
first real experiment, and it favours the first:** a correctly formed `'Y'`
missed-call flag produced nothing on the dashboard.

The capture-based reasoning was: the official app's idle packet carried `'N'`,
so `'N'` must mean idle. That only holds if the captured packet really was idle,
which was assumed rather than established.

#### How to settle it in 30 seconds - `TEST_FLAGS`

A debug hook now forces bytes 14/15 directly, bypassing `ClusterAlerts`, so
polarity is the only variable and no rebuild is needed:

```
adb shell "am broadcast -n com.gnyaneshwar.rideconnectx/com.eshwar.rideconnectx.debug.NavTestReceiver -a com.eshwar.rideconnectx.TEST_FLAGS --es msg N --es call N"
```

Try `N`, then `Y`, then `N`/`Y` mixed. Whichever lights a lamp is the answer.
If **neither** does, the flags are not the problem and the next suspect is that
this cluster only renders alerts while a route is active, or that `0x06` needs a
field we have not identified.

#### What was fixed on the phone side (all verified in the log)

- **Incoming calls were being discarded.** `relayClusterAlert` opened with
  `if (sbn.isOngoing) return`, and a ringing call posts an *ongoing*
  notification, so it never reached the category check. Calls are now exempt.
- **The name had never been sent at all.** `buildNotificationPacket` (0x06,
  carrying app + sender + message) existed in the protocol code and **nothing
  called it**. Third instance of that pattern in this codebase.
- **Alerts waited up to 5s** for the next heartbeat tick. They are now pushed
  the instant they arrive.
- Caller name needs **no READ_CONTACTS**: the dialer resolves it into
  `EXTRA_TITLE` before posting, giving the contact name when saved and the bare
  number when not - exactly what the rider asked for.

---

### 🔒 Maneuver codes — LOCKED by the rider, 11 August 2026

**Do not change any value in this table without the rider saying so explicitly.**
Every entry was photographed on the vehicle. Re-deriving, "correcting" or
re-ordering it from source code, from the Mappls id table, or from a later
sweep is forbidden unless the rider asks for a re-calibration.

### ✅ Maneuver codes — photographed 11 August 2026

Re-calibrated properly: one code at a time, held on the cluster until the rider
answered, every entry photographed. Raw log in `tools/cluster-codes.csv`.

| Code | Cluster draws |
|---|---|
| 31 | keep left — Y-fork, left branch |
| 32 | keep right — Y-fork, right branch |
| 33 | crossroads, straight through |
| 34 | **sharp left** (hairpin) |
| 35 | **turn right** (curved road) |
| 36 | **sharp right** (hairpin) |
| 37 | **turn left** (curved road) |
| 38 | left with one lane bar (fork/exit variant) |
| 39 | **U-turn** |
| 40 | **straight** |
| 41 | **slight right** (diagonal up-right) |
| 42 | right, plain horizontal arrow (second right icon) |
| 43 | ferry (boat) |
| 44 | keep left, two lane bars |
| 45 | **roundabout** |
| 46-53 | nothing drawn |
| 54-58 | weather icons (from source, not observed) |

### Coverage against the 30 standard Maps maneuvers

13 exact · 8 approximated · 9 missing. The binding constraint is the **cluster's
icon set**, not Google — Maps can express 30 concepts, this dashboard draws
about 15 in the range swept so far.

| Maps maneuver | Code | Status |
|---|---|---|
| Straight | 40 | exact |
| Turn left / right | 37 / 35 | exact |
| Slight right | 41 | exact |
| Slight left | 37 | **no icon exists** — full left instead |
| Sharp left / right | 34 / 36 | exact |
| U-turn (either side) | 39 | one icon only |
| Keep / fork left | 31 | exact |
| Keep / fork right | 32 | exact |
| Roundabout (any) | 45 | generic — no exit numbers |
| Merge left / right | 40 | **not handled** |
| Ramp left / right | 37 / 35 | falls through to a plain turn |
| Lane guidance / keep / merge | 40 | **not handled** — 44 may be lane-keep-left |
| Off-route / re-route | — | **nothing** |
| Destination / arrived | 40 | **no flag** — see below |

**Update, 11 August:** the APK has since been decoded — see
`Suzuki-APK-Reference.md`. The full range is **cluster codes 1-52**, and most of
their meanings are now predicted from the icon geometry. Highlights:

- **Slight left is cluster 19**, not missing as concluded below.
- Cluster 15-19 and 40-42 are one family of eight bearings; 40/41/42 match our
  photographs exactly, which is what makes the rest credible.
- Roundabout exits exist twice over, at 20-26 and 47-52.
- A full basic turn set sits at 1-9.

Six spot-checks (19, 18, 16, 8, 1, 23) would confirm the lot.

**The gap is almost certainly codes 1-30, which have never been swept.** 31 is
keep-left and the sweep started there arbitrarily; the destination flag, merge,
ramp and lane-guidance icons very likely sit below it. The rider's report shows
the official app drawing a chequered flag on arrival, so the cluster has one —
its number is simply unknown. 46-53 also need re-checking; "blank" comes from
the discredited 4 August sweep.

Notes worth keeping:

- **The two sides are not mirror images.** Left (37) is drawn as a curving road;
  right exists twice, as a curve (35) and as a flat arrow (42).
- **There is no slight-left.** No plain up-left diagonal appears anywhere in
  31..45. `SLIGHT_LEFT` maps to 37 — correct direction, overstated angle, which
  beats a code the cluster ignores.
- **34 was misread at first** as "up left" and re-shown before recording; it is
  a hairpin. Re-checking rather than trusting the first answer is the whole
  method here.

### ⚠ The superseded 4 August sweep — why it was wrong

A real ride proved two things at once:

1. **The Maps notification was being read from the wrong fields** (see below), so
   the app sent nothing but `STRAIGHT` (1060×) and `LEFT` (92×) for the entire
   trip. No RIGHT, no U-TURN, no ROUNDABOUT was ever transmitted.
2. **The cluster drew a U-turn for almost every one of those.** If the app was
   sending `39` and the dashboard drew a U-turn, then `39` is not straight
   ahead — and the sweep table below is wrong by at least one position.

The one datum that survives is **`38`**, which is photographed and which the
rider described on the ride as a diagonal/slight left — consistent both times.

The ride also implies **`39` draws a U-turn**: the parser bug meant `39` was
being sent almost continuously, and the rider reported a U-turn on the dashboard
throughout.

**The sweep is still a valid method** — checked afterwards, the debug sweep path
(`NavTestReceiver.sendRawCode`) and the live navigation path (`NavigationRelay.
relay`) both call the same `buildNavigationPacket`, with the same 12-hour
`hhmma` clock. So a code calibrated by sweeping is the same code real navigation
sends. What went wrong was the *answers*, not the mechanism: they were collected
in batches while codes were being sent rapidly. One code, one confirmation, next.

The sweep answers for 39-45 were given in batches while codes were being sent
rapidly, twice arriving mid-run, and the spot-check on 41 was never answered.
**Treat the table below as unverified.** Re-calibrate one code at a time, with
an explicit confirmation for each, before trusting any of it.

### Maneuver codes — first sweep, 4 August 2026 (SUSPECT, see above)

Established by stepping codes 34..53 one at a time against the rider's
Access 125 and recording what the dashboard drew. Raw log:
`tools/cluster-codes.csv`. Code 38 is photographed.

| Code | Cluster draws |
|---|---|
| 34-37 | roundabout-exit variants |
| **38** | **turn left** (arrow up-left beside a vertical bar) |
| **39** | **straight** |
| **40** | **slight right** |
| **41** | **U-turn** |
| **42** | **turn right** |
| 43 | ferry ("a ship") |
| **44** | **slight left** |
| **45** | **roundabout** |
| 46-53 | nothing drawn |
| 54-58 | **weather icons** — fog 54, showers 55, t-storms 56, 57, snow 58 |

Two earlier claims here were wrong and are now retired:

- `TURN_LEFT = 40, TURN_RIGHT = 41, U_TURN = 48, ROUNDABOUT = 51` — never
  checked against a dashboard. This is what made every turn but straight show
  the wrong icon.
- **`46` is not straight.** It draws nothing. `39` is straight. The old note
  came from the builder's `i = 46` fallback, which is a *Mappls* id, not a
  cluster code. `DISTANCE_INFO = 46` was also the parser's fallback for
  unrecognised phrasing, so unfamiliar Maps wording silently blanked the
  cluster.

The weather band at 54+ was found in `C4941q0` — the same field carries
ambient-weather icons, which is how the official app puts weather on the
cluster.

### Other captures

**0x33 heartbeat** — `A5 33 30 59 FF FF FF 33 30 35 35 35 35 34 4E 4E …`
Trailing `4E 4E` = ASCII `N N` = `i0`/`j0`. Counter increments `3055554 → …7`.

**0x36 profile** — carries the rider name in ASCII:
`47 4E 59 41 4E 45 53 48 57 41 52` = `GNYANESHWAR`.

**0x37 telemetry — SOLVED 4 August 2026.** Digits at bytes 2..22 split 9/6/6.
Confirmed by capturing a frame and photographing the cluster in the same minute:

```
a5 37 | 000001602 | 001987 | 010792 | 01 | 35 | 00 00 00 | 6d 7f
         odo 1602   A 198.7  B 1079.2      fuel
```

| Bytes | Meaning |
|---|---|
| 2-10 | odometer, 9 ASCII digits, whole km |
| 11-16 | Trip A, 6 digits, tenths of a km |
| 17-22 | Trip B, 6 digits, tenths of a km |
| 23 | `0x01`, meaning not established |
| 24 | fuel bar as ASCII `'0'`..`'5'` (five-segment E-F bar) |
| 25-27 | zero padding |

The same split reproduces the older archived capture
(`000000897 002188 003739` → ODO 897, A 218.8, B 373.9); both are pinned in
`ProtocolEngineTest`.

**Not transmitted:** speed, fuel economy (km/L) and battery voltage. The
cluster displays all three, but they are not in the frame — so the app's
dashboard must not claim to know them.

### 0x33 heartbeat layout

From `C4956y.run()`:
`"?3" + battery(2) + speed(3) + signal(1) + clock(6) + padding`

| Bytes | Meaning |
|---|---|
| 2 | phone battery bars, `'0'`..`'3'` — **confirmed on the dashboard** |
| 3 | charging flag, `'Y'` / `'N'` — **confirmed** |
| 4-6 | speed `%03d`; all `FF` when zero |
| 7 | signal bars `'0'`..`'3'` — **confirmed**; zero on no SIM or airplane mode |
| 8-13 | clock `HHmmss`; all `FF` when `"000000"` |
| 14 | notification flag — `'N'` present, `'Y'` cleared |
| 15 | missed-call flag — same convention |
| 16-27 | `FF` |

**Battery — SETTLED, do not change.** `3` → three bars, `4` → also three, `5` →
blank. Mapping percentage onto 0..3 makes the indicator work. An earlier 0..9
bucket sent `8` for an 88% phone — out of range — which is why it drew empty.

The indicator has four levels: at 88% three light and the fourth **blinks
because the phone is charging**. That blinking segment is the charging
animation, not a fourth charge level. Verified by the rider against the official
app on the same cluster — both render identically. A note here previously
claimed our battery was "incomplete" because the official app looked full; that
was a misreading of the photo and is retracted.

**Signal — SETTLED, we already matched.** The official app's own logged packet
carries `'3'` at byte 7 at full strength, identical to ours. The reported
"four bars on the official app, three on ours" was a miscount; nothing was
wrong and nothing needed changing.

**Charging is rendered as a blinking bar.** With byte 3 = `'Y'` the dashboard
draws the battery bars with the top one flashing — the same animation a phone
uses. Confirmed on hardware with the phone on USB.

`BatteryManager.isCharging` returns false on the test phone (OnePlus,
Android 15) while `dumpsys battery` reports `status: 2`. Read
`BATTERY_PROPERTY_STATUS` with an `ACTION_BATTERY_CHANGED` fallback instead.

### ⭐ Google Maps notification field layout — captured, not assumed

From the rider's phone, 6 August 2026:

```
title   = "0 m"                                 ← distance to the next turn
text    = "Head north"                          ← THE INSTRUCTION
subText = "1 hr 12 min · 38 km · 1:38 am ETA"   ← duration, remaining, ETA
```

`bigText`, `infoText`, `summaryText`, `titleBig` and `tickerText` are all null.

The parser had this **exactly inverted** — a comment in the source read *"Title
carries the instruction"* — so it searched `"350 m"` for the word "left", found
none, and fell back to straight ahead every time. One whole ride relayed nothing
useful because of it, and the ETA was read from the wrong field too.

**The unit tests encoded the same mistake**, passing the instruction as `title`.
All 42 passed while the feature was completely broken. They now go through a
`maps(instruction, distance, journey)` helper so they cannot drift back.

Lesson worth keeping: a test written from the same assumption as the code proves
only that the assumption is self-consistent.

### ⭐ The official app logs its own packets — read them with logcat

**The single most useful discovery of the session.** The release build still
calls `Log.e("Data Packet-Main", …)` with the full hex of every packet it
writes. No root, no HCI snoop, no sniffer hardware:

```
adb logcat -c
adb logcat -d | findstr "Data Packet"
```

The HCI snoop route is a **dead end on this phone** — OnePlus writes an
encrypted `.cfa` file and the one recovered via `adb bugreport` was 16 bytes.
Don't spend time on it again.

Captured status packet, 4 August 2026, paired to `SAS210217219`:

```
A5 33 32 59 FF FF FF 33 31 30 35 31 33 38 4E 4E FF×12 4F 7F
      ↑  ↑           ↑  └ clock 105138 ┘ ↑  ↑
   batt 2 charging  signal 3          idle N N
```

What it established:

- **Signal `'3'` at full strength** — our encoding already matched exactly.
- **Bytes 14-15 rest at `'N'`.** We had the flags inverted: `'Y'` (89) is the
  alert, `'N'` (78) is idle. `m8617s()` holds `89` for three heartbeats then
  reverts. Every idle packet we sent was announcing a permanent alert.
- **The clock is 12-hour** (`hhmmss`): `105138` captured at 22:51:38.
- **Bytes 16-27 are all `FF` on this model** — so the weather branch below is
  not used by the Access, and blanket-filling that range costs nothing here.
- **Our checksum is right.** Sum of bytes 1..27 complemented reproduces `0x4F`
  on their packet exactly.

`ProtocolEngineTest` now pins our builder against this captured frame.

### The status builder has TWO branches — only one was read

`C4956y.run()` branches on `C3055K.f9282g`. The branch read first sets bytes
16-27 to `FF`. **The other branch does not:**

```java
for (i = 16; i <= 20; i++) iArr[i] = 255;
for (i = 24; i <= 27; i++) iArr[i] = 255;
iArr[21] = f17340M;         // weather code, 1..10, set from weather text
iArr[22] = (int) f17341N;    // temperature
iArr[23] = 1;
iArr[28] = f9282g ? 255 - (sum % 256) : sum % 256;
```

Consequences for our implementation:

1. **Bytes 21-23 carry weather and temperature.** `buildHeartbeatPacket`
   blankets 16-27 with `FF` and therefore erases them. This is how the official
   app puts ambient weather on the cluster.
2. **The status checksum is branch-dependent** — plain sum here, complement in
   the other branch. We use the complement everywhere.

### Best remaining capture — the official app's navigation packets

Same logcat trick, during a real route in the official app. `A5 31 …` frames
carry the maneuver byte at index 2, so a route with a few turns would confirm
the observed arrow table (38 left, 42 right, …) against Suzuki's own values —
without riding, if the rider drives it with their GPS joystick app.

Also still uncaptured: the `A5 36` profile packet, which fires on connect. Clear
logcat *before* pairing to catch it.

**Send the status packet before the profile burst.** The profile repeats ten
times at one-second intervals; starting the heartbeat after it left the cluster
with no battery or signal for ten seconds after connecting. Status now goes out
~0.5 s after `GATT connected`.

### Profile flag, byte 27 — SOLVED

Not a constant. From `C5028d`:

```java
if (prefs.getString("prev_cluster","").equals(currentClusterName))
     C3055K.f9294s = false;   // bytes[27] = 82 = 'R'  same cluster as last time
else C3055K.f9294s = true;    // bytes[27] = 70 = 'F'  new cluster
```

Hardcoding `'F'` announced "brand new vehicle" on every connect and the cluster
answered `<NAME> CONNECTED`. Reconnecting to the same scooter sends `'R'` —
the greeting path.

---

## 5. Useful commands

```bash
# launch (note: applicationId, not namespace)
adb shell am start -n com.gnyaneshwar.rideconnectx/com.eshwar.rideconnectx.MainActivity

# BLE log during a real connection — this is what unblocks the remaining unknowns
adb logcat -c && adb logcat -s RCX-BLE

# navigation log
adb logcat -s RCX-Nav

# grant notification access to the Maps listener
adb shell cmd notification allow_listener \
  com.gnyaneshwar.rideconnectx/com.eshwar.rideconnectx.data.nav.MapsNotificationListener

# drive navigation without Maps (debug builds only)
adb shell "am broadcast -n com.gnyaneshwar.rideconnectx/com.eshwar.rideconnectx.debug.NavTestReceiver \
  -a com.eshwar.rideconnectx.TEST_NAV --es title 'Turn left onto MG Road' --es text '400 m, 12 min'"
```

---

## 6. Open problems

Everything here genuinely needs the scooter.

1. **The stored cluster name is a placeholder.** `lastDeviceName` holds
   `"Suzuki Scooter"`, not `SAS210217219`. The same-cluster comparison still
   works (it compares like with like), but `configureForDevice` cannot read the
   model from it and falls back to the Access checksum branch — right by luck
   for this vehicle, wrong for an e-ACCESS or a TFT model. Store the advertised
   name.
2. **Fourth bar on the battery / signal indicators.** The field tops out at 3
   and the fourth segment only lights during the power-on self-test. If the
   official app is ever seen driving four bars, this needs revisiting.

Resolved since this list was written:

- ~~What each cluster code draws~~ — swept and recorded, see the maneuver table.
- ~~0x37 telemetry field mapping~~ — solved, see above.
- ~~Heartbeat bytes 2-3~~ — battery bars 0..3 and a `Y`/`N` charging flag, both
  confirmed on the dashboard, charging shown as a blinking bar.
- ~~Does `'R'` at byte 27 produce WELCOME?~~ **Yes.** Confirmed on hardware:
  reconnecting to the same cluster sends `'R'` and the dashboard printed
  `WELCOME ESHWAR P`. `'F'` gives `<NAME> CONNECTED`.
- ~~Handshake / send order~~ — the `0x36` profile packet alone triggers the
  greeting, provided it waits on `servicesReady` and repeats on a timer
  (mirrors `C4956y.run()`). No heartbeat or session prelude needed.
- ~~Co-existence~~ — confirmed **exclusive**. The official app must be disabled
  while testing; force-stop is not enough, it respawns.
- ~~Live GATT confirmation~~ — confirmed on hardware. `00000001`
  (WRITE_NO_RESPONSE) accepts the 30-byte write and the cluster reacts;
  `00000003` is accepted by the stack and silently ignored.

### Known limitations of past capture attempts
- Android HCI snoop on this phone produced only 16-byte `btsnoop` headers — no payloads.
- Frida sees the device and process but cannot attach on non-rooted Android 15.

---

## 7. Rules for anyone continuing this work

- Search this file and `PROJECT-STATUS.md` **before** asking for new captures.
- Never promote an assumption to a fact. Label Confirmed / Unverified / Unknown.
- Never fake a successful transmission. If nothing left the phone, report false.
- Do not break what works — auth (Google/email/guest) is done and must stay done.
- One thing at a time; do not bundle unrelated changes.
- Evidence before code: state the exception, class, method and line first.
