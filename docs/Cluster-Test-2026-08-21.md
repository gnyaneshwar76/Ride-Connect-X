# Live cluster test — 21 August 2026, evening

**Lockito + Google Maps + real cluster, scooter stationary, engine running.**
First end-to-end test of the arrow decode built on 20 August.

Read with `docs/Maps-Arrow-Decode-2026-08-20.md`.

---

## Result: the arrow decode WORKS. One thing to fix.

Every navigation frame was identified from the arrow picture. Nothing fell back
to STRAIGHT. Two days earlier, 88% of a real ride went out as STRAIGHT because
only the words were read.

```
Maps icon : maneuver_on_ramp_normal_left  (+/-1)  ->  LEFT (code 37)
We sent   : LEFT   (code 37)   [from ICON]
```

`[from ICON]` on 100% of frames. Zero `*** NO DIRECTION ANYWHERE ***` markers.

Seven distinct manoeuvres were sent, **all from frames whose text carried no
direction at all** — bare road names like `"Dammaiguda Rd"`,
`"E Maredpally Rd"`, `"towards Hi-Tension Rd"`:

| Manoeuvre | Code | First ever sent live? |
|---|---|---|
| Straight | 40 | no |
| Turn left | 37 | no |
| Turn right | 35 | no |
| **U-turn** | **39** | **yes** |
| **Slight left** | **19** | **yes** |
| **Slight right** | **41** | **yes** |
| **Roundabout** | **45** | **yes** |

Four codes drawn on this cluster for the first time. Slight left (19) matters
most: it was found in the 18 August sweep and had been impossible to trigger,
because Maps announces that turn as a road name and nothing in the words says
"slight" or "left".

---

## Photographed on the dashboard

| Arrow | Code | Photo | Log correlation |
|---|---|---|---|
| Right turn (curved) | 35 | 20:29 | `"Dammaiguda Rd"` -> RIGHT at 20:29:03 |
| Roundabout (ring) | 45 | 20:37 | `"E Maredpally Rd"` -> ROUNDABOUT |
| Slight left (up-left diagonal) | 19 | 20:39 | `"W Marredpally Rd"` 600m -> 200m |

The rider also observed Maps and the cluster showing a **U-turn at the same
instant** — the cleanest verification available, since it compares Google's own
arrow against the cluster with no timing inference.

---

## ⚠ THE OPEN BUG — left and right appear SWAPPED

**Reported by the rider comparing Maps' on-screen arrow against the cluster
side by side**, which is the reliable way to do it:

- Maps shows **left** -> cluster draws an **up-right** shape
- Maps shows **slight left** -> cluster draws **up-right**
- Maps shows **slight right** -> cluster draws **up-left**

### Our side is proven correct

The catalogue drawables were rendered and drawn out at the desk.
`maneuver_turn_normal_left` **is** a left arrow — shaft rising from the bottom
right, arrowhead pointing left. The matcher identifies it at distance 1 and
sends code 37. There is no mirror error in the fingerprinting.

### The pattern points at the code table

| Manoeuvre | Has a left/right twin? | Result on the cluster |
|---|---|---|
| Straight (40) | no | ✅ correct |
| Roundabout (45) | no | ✅ correct |
| U-turn (39) | no | ✅ correct |
| Left (37) / Right (35) | **yes** | ❌ swapped |
| Slight left (19) / Slight right (41) | **yes** | ❌ swapped |

**Every code without a mirror twin is correct. Every code with one is wrong.**
That is not coincidence, and it is not consistent with a fault in the decode.

### Hypothesis — NOT yet acted on

`35` and `37` are transposed in the cluster table, and so are `19` and `41`.
Left should send **35**, right **37**; slight left **41**, slight right **19**.

The 11 August calibration recorded 37 = turn left, photographed one code at a
time. Either that transcription swapped the pair, or the codes mean something
other than recorded.

**Nothing has been changed on this reasoning.** The maneuver table is locked by
the rider and only re-calibration on hardware may alter it.

### The test that settles it — 10 seconds

Scooter stationary, ignition on, **engine running**. Hold one code and look:

```
adb shell "am broadcast -n com.gnyaneshwar.rideconnectx/com.eshwar.rideconnectx.debug.NavTestReceiver -a com.eshwar.rideconnectx.TEST_CODE --ei code 37 --ei dist 37"
```

If **37 draws a RIGHT turn**, swap all four and navigation is finished. Confirm
35, 19 and 41 the same way — four codes, under a minute.

Send `dist` equal to the code so the cluster prints its own number and no answer
can be attributed to the wrong code. That rule is what made the 18 August sweep
trustworthy.

---

## Also confirmed — the distance fields

The rider's request from 20 August, now verified from the bytes on the wire:

```
a5 31 23 ff "1500M" "0830PM" ffffff "9999M" "11" ffffff a4 7f
         |          |                  |
      code 35   next turn 1500m    remaining CLAMPED
```

1. **Next-turn distance shows metres, never km.** `2500m` where the rider wants
   `2.5 km`. Confirmed counting down correctly (1500M -> 1400M), so this is
   formatting, not a fault.
2. **Remaining distance clamps at `9999M`.** The destination was 43 km away; the
   field is four digits plus one unit character, so it cannot express it and
   sticks at 9999 until within 10 km.

Both have the same fix: send kilometres with a `K` unit rather than metres with
`M`. **The km encoding has never been observed** — every distance captured from
the official Suzuki app is under 1000 m — so the digit layout must be tested,
not guessed. Needs a debug hook that pushes raw field values.

### The rider's rule, stated exactly (21 August)

> Under 1000 m, show metres. 1000 m and above, show kilometres.

```
   900 m   ->  900m      (metres, unchanged)
   800 m   ->  800m
 1,000 m   ->  1.0 km
 2,500 m   ->  2.5 km
 3,700 m   ->  3.7 km
43,000 m   ->  43 km
```

Applies to **both** distance fields — next-turn (bytes 4-8) and
remaining-to-destination (bytes 18-22).

**Confirmed the same evening:** once the destination fell below 10 km the
remaining field started counting normally (`3700m` at 20:56). So the `9999M`
was purely the four-digit clamp, exactly as diagnosed — not a fault in the
builder.

---

## Also confirmed — alert lamps still dead, with a NEW clue

A real call was placed from the rider's grandmother's phone, and a WhatsApp
message sent. The phone received both. **The cluster showed nothing.**

`TEST_FLAGS` was fired with `N`/`N` during the session; the bytes left the phone
correctly (`... 4e 4e ...` at 14/15). No lamp.

**The new information:** pressing the second button on the cluster shows a
**missed-call count**, and it reads **0**.

That reframes the problem. A *counter* implies the cluster expects a **number**,
not the `'Y'`/`'N'` flag we have been sending in one byte. If so, no polarity of
a single flag will ever satisfy it.

### Next move on this — read Suzuki's own answer

The official app prints every packet it sends to logcat. That trick solved the
checksum, the greeting and the telemetry, and it applies here:

1. Re-enable the official Suzuki Ride Connect app
2. Connect it to the cluster
3. Place a real call to the phone
4. `adb logcat -d | findstr "Data Packet"`
5. Copy the bytes it sends

Turns a guess into a capture. ~10 minutes at the scooter.

---

## ⚠ BUG — the ETA field shows the CURRENT time, not the arrival time

Spotted by the rider, 20:59. The cluster prints this field under the label
**ETA**, and we have been filling it with the phone's clock:

```
   550m   ETA
 0858Pm  2500m        <- cluster clock read 8:59. "0858Pm" is NOW, not arrival.
```

Bytes 9-14 of the `0x31` navigation packet are labelled "clock" in the knowledge
base and `buildNavigationPacket` fills them with the current time. The cluster
evidently renders them as the **estimated time of arrival**.

**The fix is cheap because the data is already parsed.** Maps' subText carries
it — `"47 min · 22 km · 9:35 pm ETA"` — and `MapsNotificationParser` already
extracts `etaMinutes`. Send **now + etaMinutes**, formatted `hhmma`, rather than
the current clock.

Open questions before changing it:

- Does the **heartbeat** (`0x33`, bytes 8-13) want the real clock while only the
  **navigation** packet wants the ETA? The 0x33 clock is confirmed correct on
  the dashboard, so almost certainly yes — change only `0x31`.
- What should bytes 9-14 hold when no ETA is known? Probably the current time,
  as now.

---

## ✅ Confirmed working — heartbeat display

Observed by the rider on the dashboard during the session:

- **Signal: full bars, correct.** The rider reports **four** bars lit at full
  strength. Worth noting against the knowledge base's open question #2, which
  said the field tops out at `'3'` and the fourth segment only lights during the
  power-on self test. Whatever the internal mapping, the indicator reads
  correctly and matches the phone — **this can be closed.**
- **Battery: correct, with the charging animation.** Phone at 25%: one segment
  lit solid and the second **blinking**, because the phone was on USB to the
  laptop. That is exactly the behaviour signed off in August — the blinking
  segment is the charging animation, not a fourth charge level.
- **Bluetooth icon and clock** both live throughout.
- **Telemetry returning**: odometer read 1902 km on the cluster, and `a537`
  frames were seen arriving from the cluster in logcat.

So the whole heartbeat path — battery, charging flag, signal, clock — is
verified end to end on hardware, at the same time as navigation was running.

---

## Notes on method

- **Lockito must be "Follow roads for car"**, and the mode applies only to
  **newly added** points — changing it does not re-route existing ones. A
  straight line across the map means it is wrong. Delete and rebuild.
- Lockito and Maps must use the **same destination**, or Maps reroutes
  continuously.
- **4x speed is safe** — a 10x burst was survived with zero reroutes — but drop
  to 1x near the destination or arrival will never register.
- **Do not correlate arrows live.** There is lag at four points: adb read ->
  cluster redraw -> rider's glance -> message. The rider comparing Maps' own
  arrow against the cluster in one look is the only reliable check.
- The cluster **latches the last navigation packet**. When navigation stops the
  arrow, distance and clock freeze at their last values. That is not a fault.

---

## FIXES BUILT AFTER THIS TEST — 21/22 August

All three written the same night, **98/98 tests passing**, installed on the phone.

### 1. Roundabouts send the exit direction

`MapsArrowCatalog.kt`. All 42 roundabout variants used to collapse to the bare
ring [ROUNDABOUT] (45). Ridden, that is useless — 61 frames of this run were
`roundabout_enter_and_exit_cw_slight_left`, so Maps knew the exit and the
cluster drew a circle that says nothing about which way to leave it.

Now mapped onto the cluster's own exit-specific icons:

```
..._sharp_left   -> 20   left-down
..._normal_left  -> 21   left-middle
..._slight_left  -> 22   left-up
..._straight     -> 23   centre-up   <- CONFIRMED 18 Aug
..._slight_right -> 24   right-up
..._normal_right -> 25   right-middle
..._sharp_right  -> 26   right-down
```

Constants added to `ProtocolEngine.Maneuver` as `ROUNDABOUT_EXIT_*`. Codes
20-26 are **predicted from the APK icon geometry**, only 23 confirmed — the
same decode that went 8 for 8 in the 18 August sweep, so credible but not
proven. The ride log marks these frames `PREDICTED ONLY`. A bare
`maneuver_roundabout_*` with no exit in its name still sends the generic ring.

### 2. ETA field sends the arrival time

`NavigationRelay.kt`. Bytes 9-14 of the `0x31` packet were fed `clockNow()`.
Now `etaClock(maneuver.etaMinutes)` — now plus the journey time Maps states in
its subText, which the parser already extracted and was throwing away. Falls
back to the current clock when Maps gives no duration.

The `0x33` heartbeat clock is a different field, confirmed correct, untouched.

### 3. Distance formatting — ✅ CONFIRMED GOOD BY THE RIDER

`ProtocolEngine.formatDistance()`, used by both distance fields.

```
  under 1000 m  ->  0900M     metres
  1000 m and up ->  02.5K     kilometres
  43,000 m      ->  0043K
```

**The rider confirmed on 5 September that this reads correctly on the cluster**
— under 1 km in metres, 1 km and over in kilometres. No further change wanted.

The km layout had never been captured from the official app, so three encodings
were made switchable via `ProtocolEngine.kmStyle` and the `TEST_KMSTYLE`
broadcast. **Style 0 is correct**; the switch stays in place but needs no test.

### NOT changed — deliberately

**Left and right were not swapped.** An early report during the test suggested
it, but the rider corrected themselves: slight-left draws up-left and
slight-right draws up-right, both correct, and plain left/right were never
actually compared against Maps side by side. No evidence of a swap, and the
maneuver table is locked by the rider. Still to be confirmed by holding codes.

---

## Rider's verdict, 5 September 2026

> Only turns should be adjusted. Next-turn metres and km-left — I observed
> those and they are good.

So of everything found in this test, **only the turn arrows remain open**. The
distance fields, ETA, signal, battery and telemetry are all signed off.

---

## Still not done

| Item | Needs |
|---|---|
| **Left/right swap** — hold codes 37, 35, 19, 41 | 1 min, engine running |
| **ETA field shows current time** — send now + etaMinutes in `0x31` bytes 9-14 | desk fix |
| **Distance in km above 1000 m** — `2500m`, `9999m` | debug hook + 30 sec test |
| **Arrival bullseye (code 9)** — never fired; route stopped ~500 m short | short 2 km Lockito route |
| **Alert lamps** — capture official app's packets | ~10 min, official app re-enabled |

Two of these — the ETA field and the km formatting — are **desk work** and need
neither the scooter nor a simulation to write. Only the left/right swap, the
bullseye and the alert lamps need hardware.

---

## How the session ended

Stopped ~500 m short of the destination, so **the arrival bullseye never
fired** — it remains the one untested code. Lockito was stopped deliberately:
the laptop battery was flat, the phone was down to 25% and running at **40+ °C**
from four hours of navigation, screen-on and charging simultaneously.

Worth remembering for the next long run: **the phone gets hot**. Navigation,
BLE, mock GPS and charging at once is a heavy load. Let it cool between runs.
