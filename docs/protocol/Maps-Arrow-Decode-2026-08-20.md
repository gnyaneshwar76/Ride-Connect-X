# Google Maps arrow decode - SOLVED, 20 August 2026

**The last unknown in navigation is closed.** RideConnectX can now read the
manoeuvre direction for every turn Google Maps can express.

Read with `docs/protocol/RideConnectX-Knowledge-Base.md` and `docs/status/PROJECT-STATUS.md`.

---

## TL;DR

Google Maps does **not** put the turn direction in the notification's words. It
puts the road you are turning *onto* and counts the distance down. The direction
is carried **only by the arrow picture** in the notification's large-icon slot -
a field this project had never read.

Consequence, measured on the 19 August evening ride: **1,643 of 1,873 frames
(88%) were sent to the cluster as STRAIGHT (code 40) when they were real turns.**
That is what the rider saw on the dashboard, and code 40 is photographed as a
straight arrow, so the log and the rider's eyes agree.

Fixed by reading the arrow bitmap, reducing it to a shape fingerprint, and
matching it against Google's own manoeuvre drawables extracted from the Maps
APK. **67 arrows, all mappable onto the rider's calibrated cluster codes.**

---

## Why the earlier attempts failed

| Attempt | What was read | Result |
|---|---|---|
| Text fields (title / text / subText) | the words | direction absent on 88% of frames |
| Small icon | `nav_notification_icon` | one generic name for every manoeuvre |
| **Large icon** | **the arrow bitmap** | **the direction, on every frame** |

The root cause was method, not luck: each round tested one narrow guess instead
of dumping the whole notification and then narrowing. The dump now lists every
extras key and both icons, so a field cannot be missed again.

The rider's screenshot of the notification shade is what exposed it - the arrow
is plainly visible on the right of the card.

---

## How the arrow is read

The large icon is a **90x90 bitmap**, not a named resource, so it cannot be
identified by name. It is reduced to a fingerprint instead:

1. Render to 32x32
2. Take the **alpha channel only** - Maps tints the arrow white, so colour is noise
3. Reduce to a **16x16 on/off grid**, 4 pixels per cell
4. Express as 64 hex characters

The same reduction is applied to Google's own drawables, loaded by name from the
Maps package with `getResourcesForApplication`. Same renderer both sides, so the
fingerprints are directly comparable.

Matching is **nearest-neighbour by Hamming distance**, not exact equality. Two of
the five live arrows did not match any catalogue entry exactly, but every one
matched the correct entry as its nearest neighbour.

---

## Verification - all five live arrows matched

Captured on a Lockito-simulated route, 20 August, then matched against the
catalogue:

| Live arrow | Matched | Distance | Nearest WRONG answer |
|---|---|---|---|
| #1 | `maneuver_depart` | **7/256** | 43-44 away |
| #2 | `maneuver_turn_normal_left` | **1/256** | 43-44 away |
| #3 | `maneuver_turn_normal_right` | **0/256** | 43-44 away |
| #4 | `maneuver_straight` | **7/256** | 43-44 away |
| #5 | `maneuver_turn_slight_left` | **0/256** | 43-44 away |

The margin is the point: the correct answer is 0-7 squares different out of 256,
the nearest wrong answer is 43+. Not a coin flip.

**Critically, #3 was captured on a frame where Maps said only a road name** -
"Dammaiguda Rd / Dammaiguda X Rd / Nagaram Rd". That is the 88% case, and the
arrow carried the direction correctly.

### The five arrows, drawn from their fingerprints

**#1 -> `maneuver_depart`** - seen while Maps said "Head north"

```
              ####
            ########
          ############
        ####  ####  ####
              ####
              ####
              ####

              ####

              ####
```

**#2 -> `maneuver_turn_normal_left`** - seen while Maps said "Turn left"

```
          ##
        ####
      ######
    ######
    ########################
    ######              ####
      ######              ####
          ##              ####
                          ####
                          ####
                          ####
                          ####
```

**#3 -> `maneuver_turn_normal_right`** - seen while Maps said "Dammaiguda Rd (road name only)"

```
                    ##
                    ####
                    ######
                      ######
    ########################
    ####              ######
  ####              ######
  ####              ####
  ####
  ####
  ####
  ####
```

**#4 -> `maneuver_straight`** - seen while Maps said "Netaji Nagar Cross Rd"

```
              ####
            ########
          ############
        ####  ####  ####
              ####
              ####
              ####
              ####
              ####
              ####
              ####
```

**#5 -> `maneuver_turn_slight_left`** - seen while Maps said "towards Prem Nagar Colony Rd"

```
        ############
        ############
        ########
        ##########
        ####  ######
        ####    ######
                  ####
                    ####
                      ##
                      ####
                      ####
                      ####
```

---

## The full catalogue - 67 arrows

Extracted from `Maps.apk` v26.33.02 pulled off the phone with `adb`, listed with
`aapt2 dump resources`. Resource **names** are intact in Maps (the file paths are
obfuscated, the names are not), which is what makes this possible.

Cluster codes are the rider's own calibration, photographed on the Access 125.

| Maps drawable | Cluster code | Cluster draws | Note |
|---|---|---|---|
| `arrive_left` | **9** | BULLSEYE | confirmed |
| `ic_arrive_left` | **9** | BULLSEYE | confirmed |
| `ic_arrive_right` | **9** | BULLSEYE | confirmed |
| `maneuver_depart` | **40** | STRAIGHT | confirmed |
| `maneuver_destination` | **9** | BULLSEYE | confirmed |
| `maneuver_destination_left` | **9** | BULLSEYE | confirmed |
| `maneuver_destination_right` | **9** | BULLSEYE | confirmed |
| `maneuver_destination_straight` | **9** | BULLSEYE | confirmed |
| `maneuver_fork_left` | **31** | KEEP LEFT | confirmed |
| `maneuver_fork_right` | **32** | KEEP RIGHT | confirmed |
| `maneuver_keep_left` | **31** | KEEP LEFT | confirmed |
| `maneuver_keep_right` | **32** | KEEP RIGHT | confirmed |
| `maneuver_merge` | **40** | STRAIGHT | confirmed |
| `maneuver_merge_left` | **31** | KEEP LEFT | confirmed |
| `maneuver_merge_right` | **32** | KEEP RIGHT | confirmed |
| `maneuver_name_change` | **40** | STRAIGHT | confirmed |
| `maneuver_off_ramp_keep_left` | **31** | KEEP LEFT | same drawable as plain turn |
| `maneuver_off_ramp_keep_right` | **32** | KEEP RIGHT | same drawable as plain turn |
| `maneuver_off_ramp_normal_left` | **37** | TURN LEFT | same drawable as plain turn |
| `maneuver_off_ramp_normal_right` | **35** | TURN RIGHT | same drawable as plain turn |
| `maneuver_off_ramp_sharp_left` | **34** | SHARP LEFT | same drawable as plain turn |
| `maneuver_off_ramp_sharp_right` | **36** | SHARP RIGHT | same drawable as plain turn |
| `maneuver_off_ramp_slight_left` | **19** | SLIGHT LEFT | same drawable as plain turn |
| `maneuver_off_ramp_slight_right` | **41** | SLIGHT RIGHT | same drawable as plain turn |
| `maneuver_off_ramp_u_turn_left` | **39** | U-TURN | same drawable as plain turn |
| `maneuver_off_ramp_u_turn_right` | **39** | U-TURN | same drawable as plain turn |
| `maneuver_on_ramp_keep_left` | **31** | KEEP LEFT | same drawable as plain turn |
| `maneuver_on_ramp_keep_right` | **32** | KEEP RIGHT | same drawable as plain turn |
| `maneuver_on_ramp_normal_left` | **37** | TURN LEFT | same drawable as plain turn |
| `maneuver_on_ramp_normal_right` | **35** | TURN RIGHT | same drawable as plain turn |
| `maneuver_on_ramp_sharp_left` | **34** | SHARP LEFT | same drawable as plain turn |
| `maneuver_on_ramp_sharp_right` | **36** | SHARP RIGHT | same drawable as plain turn |
| `maneuver_on_ramp_slight_left` | **19** | SLIGHT LEFT | same drawable as plain turn |
| `maneuver_on_ramp_slight_right` | **41** | SLIGHT RIGHT | same drawable as plain turn |
| `maneuver_on_ramp_u_turn_left` | **39** | U-TURN | same drawable as plain turn |
| `maneuver_on_ramp_u_turn_right` | **39** | U-TURN | same drawable as plain turn |
| `maneuver_roundabout_*` **(42 variants)** | **45** | ROUNDABOUT | generic - exit codes 20-26 unswept |
| `maneuver_straight` | **40** | STRAIGHT | confirmed |
| `maneuver_turn_normal_left` | **37** | TURN LEFT | confirmed |
| `maneuver_turn_normal_right` | **35** | TURN RIGHT | confirmed |
| `maneuver_turn_sharp_left` | **34** | SHARP LEFT | confirmed |
| `maneuver_turn_sharp_right` | **36** | SHARP RIGHT | confirmed |
| `maneuver_turn_slight_left` | **19** | SLIGHT LEFT | confirmed |
| `maneuver_turn_slight_right` | **41** | SLIGHT RIGHT | confirmed |
| `maneuver_u_turn_left` | **39** | U-TURN | confirmed |
| `maneuver_u_turn_right` | **39** | U-TURN | confirmed |

---

## Found / not found

### Found - complete

- Every one of the 67 manoeuvre drawables Maps ships
- A cluster code for all 67, from existing calibration
- Verified fingerprint match on 5 of them against live notifications
- The arrival rule (below)

### Not found - the only real gap

**Roundabout exits.** Maps distinguishes 42 roundabout cases (clockwise /
anticlockwise x which exit). The cluster has exit-specific codes at **20-26**,
but only **23** was confirmed in the 18 August sweep.

All 42 currently map to the generic **45**, which works - the rider sees
"roundabout" - it simply does not say which exit. Optional polish.

### Also unswept on the cluster, unrelated to arrows

Codes **10-14, 20-22, 24-30, 46-53**. Only worth sweeping if a Maps arrow ever
needs one of them; nothing in the 67 does today.

---

## Arrival - the rule, confirmed twice

Maps ships `maneuver_destination`, but **does not switch to it in the
notification**. On arrival it keeps the previous arrow. Confirmed on the
19 August evening ride and again on the 20 August simulation:

```
RAW  title='' | text='SRI DEVI RESIDENCY' | subText='0 min - 40 m' | icon=<the LEFT arrow>
RAW  title='' | text='Ramani Nilayam'     | subText='0 min - 10 m' | icon=<the LEFT arrow>
```

So arrival **cannot** be detected from the picture. The rule is:

- `title` carries **no distance** (it is blank), **and**
- `subText` remaining distance has collapsed to a few metres

That frame is currently **discarded entirely** by the parser, because `parse()`
returns null when no distance is found in title or text. That is why arrival was
invisible in every earlier log. Fix: detect it before that return and send
code **9**.

---

## Two theories killed by measurement

**Screen-off does not stall navigation.** Measured over 1,873 frames:

| Screen | Frames | Avg gap | Stalls >=5s | Longest |
|---|---|---|---|---|
| on | 1,175 | **4.3 s** | 331 | 87 s |
| OFF | 698 | **3.1 s** | 119 | 114 s |

Screen off was *faster*. Keeping the screen on would cost battery and fix
nothing.

**Notification throttling is not the cause either.** Maps' Navigation channel
reports `mImportance=3` (Default), not Silent. The 450 stalls are simply Maps
updating lazily when far from a turn.

---

## Still open, needs the scooter (one short session)

Nothing below blocks the arrow work.

| Task | Needs | Time |
|---|---|---|
| Alert lamps - `'Y'` vs `'N'` polarity, via `TEST_FLAGS` | ignition on, stationary | 30 sec |
| Distance field shows `9999m` instead of km | ignition on, stationary | 30 sec |
| Roundabout exit codes 20-26 | **engine running** | ~15 min |

The `9999m` is photographed on the cluster: the remaining-distance field is four
digits plus a unit character and clamps at 9999, so a 38 km destination reads as
a meaningless `9999m`. The km encoding has never been observed - every distance
captured from the official Suzuki app is under 1000 m - so it must be tested,
not guessed.

**Battery warning:** the 18 August sweep flattened the scooter battery by sitting
on ignition for 45 minutes. Run the engine during any sweep.

---

## Built and wired in - 20 August 2026, 02:00

Done, and pinned by tests. **95/95 passing** (5 new). Not yet seen on the
cluster - the phone was disconnected when this landed, so the first job next
session is to install and watch one Lockito route.

| Piece | File |
|---|---|
| 67-name -> cluster-code table | `data/nav/MapsArrowCatalog.kt` |
| Fingerprint + nearest-match | `data/nav/ArrowMatcher.kt` |
| Arrival rule -> code 9 | `MapsNotificationParser.parse` |
| STRAIGHT fallback | same |

### The arrow now outranks the text

A deliberate reversal of the previous order. The arrow is what Maps actually
draws for the rider, it is present on every frame, and it distinguishes slight
from normal from sharp and keep from fork - distinctions the words usually omit.
The text carried a direction on only 12% of the 19 August ride.

Safety rails, so this can never be worse than before:

- A match is accepted only within `ArrowMatcher.MAX_DISTANCE` = **20 of 256**
  cells. Correct matches measured 0-7 and wrong ones 43+, so that sits in the
  gap. An arrow Maps introduces in a later release falls through to the text
  rather than being forced onto the nearest old shape.
- If neither the arrow nor the text says anything: **STRAIGHT**, exactly as
  today.

### What to look for in the next log

```
Maps icon   : maneuver_turn_normal_right (+/-0)  ->  RIGHT (code 35)
We sent     : RIGHT   (code 35)   [from ICON]
```

The `*** NO DIRECTION ANYWHERE ***` marker - 306 of them in the 20 August run -
should be gone.

---

## Reference

```bash
bash tools/cluster/pull-ride-log.sh
```

```bash
adb shell "am broadcast -n com.gnyaneshwar.rideconnectx/com.eshwar.rideconnectx.debug.NavTestReceiver -a com.eshwar.rideconnectx.TEST_ARROWS"
```

| File | What |
|---|---|
| `tools/maps-apk/base.apk` | Maps 26.33.02, pulled from the phone |
| `tools/maps-apk/drawables.txt` | all 1,800 drawable names -> obfuscated paths |
| `tools/maps-apk/maneuvers.txt` | the 67 manoeuvre names |
| `tools/live/cat.txt` | catalogue dump with fingerprints |
| `frontend/app/src/main/java/com/eshwar/rideconnectx/data/nav/MapsArrowCatalog.kt` | the 67 names, in the app |

**Mock location:** Lockito (`fr.dvilleneuve.lockito`) now holds the permission;
GPS Joystick was revoked so the two cannot fight. Lockito must be set to
**"Follow roads for car"**, and Maps must navigate to the **same destination**,
or Maps reroutes continuously and the log is worthless.
