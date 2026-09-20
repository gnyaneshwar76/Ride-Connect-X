# Hardware evidence log

What was **seen on the vehicle**, written down as text so it survives. Photographs vanish with a temp folder; a dated line does not.

Every turn-code entry was captured with the distance field deliberately set to the code number, so the cluster labelled its own screen and an answer can never be attached to the wrong code.

---

## Session — 19 September 2026

Vehicle: Suzuki Access 125, cluster `SAS210217219`. Engine running, phone on cable, official Suzuki app off. Odometer read **002427 km** throughout, on both the cluster and in the app's own decode of the telemetry frame.

### Turn glyphs

| Code | Distance field | What the cluster drew |
|---|---|---|
| 40 | — | Straight-ahead arrow |
| 1 | `1m` | Shaft rising vertically, bending **left** at the top. Square junction shape, not a flat sideways arrow |
| 4 | `4m` | Mirror of 1 — shaft rising, bending **right** |
| 46 | `46m` | Arrow area **blank**. Sent after a left arrow was on screen, and it cleared it |
| 19 | `19m` | Straight diagonal arrow pointing **up-left**, no bend |
| 41 | `41m` | Straight diagonal arrow pointing **up-right**, no bend. Mirror of 19 |
| 34 | `34m` | Tight curved arrow hooking back, head toward upper-left — hairpin left |
| 36 | `36m` | Curved hairpin. **Direction not confirmed by eye** — see open items |
| 31 | `31m` | Y-shaped fork, **left** branch carrying the arrowhead |
| 32 | `32m` | Y-shaped fork, **right** branch carrying the arrowhead |

### Roundabout exits — the full set

All seven previously unverified predictions were correct. The exit arrow rotates cleanly through the range, which is itself corroboration.

| Code | Distance field | Exit arrow direction |
|---|---|---|
| 20 | `20m` | Down-left (sharp left) |
| 21 | `21m` | Horizontal left |
| 22 | `22m` | Diagonal up-left (slight left) |
| 23 | `23m` | Straight up |
| 24 | `24m` | Diagonal up-right (slight right) |
| 25 | `25m` | Horizontal right |
| 26 | `26m` | Down-right (sharp right) |
| 45 | `45m` | Plain ring — no entry stem, no exit arrow |

### Live Google Maps navigation

Route played through Lockito with Maps navigating. Real maneuvers reached the cluster:

```
RAW title='240 m' text='Gayathri Nagar Rd' icon=maneuver_on_ramp_normal_left
Relay Gayathri Nagar Rd code=1 dist=240m delivered=true
Relay Gayathri Nagar Rd code=1 dist=230m delivered=true
Relay Gayathri Nagar Rd code=1 dist=210m delivered=true
Relay Gayathri Nagar Rd code=1 dist=200m delivered=true
```

Codes fired during the run: **1** (14x), **4** (8x), **40** (2x). Rider's verdict: *"all turns came good."*

### Live telemetry frame

```
a5 37 3030303032343237 303036313839 303030333038 01 35 000000 70 7f
```

Decoded: odometer **2427 km**, trip A **618.9 km**, trip B **30.8 km**, fuel **5 bars**.

**Cross-check:** the cluster's own display read `002427 km` in the same photograph. Two independent sources agreeing — the app's decode is right, not merely self-consistent.

**Checksum confirmed:** sum of bytes 1-27 = `0x8F`, one's complement = `0x70`, byte 28 was `0x70`. The rule previously rested on historical captures; it now holds against a live frame.

### Faults observed

- **Navigation text does not clear when a route ends.** Distance zeroed to `0m` and the arrow cleared, but the ETA clock stayed on screen. Cause: `clearCluster()` sent `navActive = '1'`, telling the cluster "still navigating, values empty". Fixed in code, **not yet re-tested on hardware.**
- **Maps can freeze without removing its notification.** Lockito kept moving while Maps stopped updating; no route-ended event fired, so the cluster held stale values indefinitely. Needs a staleness timeout — not yet written.
- **App showed the pairing screen after reinstall.** Unverified whether this is a regression from gating the session save on service discovery, or a normal cold start.

### False alarm, recorded so it is not re-diagnosed

The cluster appeared to "blink" between two arrows. Cause was a stray background test loop still sending code 40 while a second code was being tested — two codes alternating twice a second. Nothing wrong with the cluster or the code. **Kill stray loops before trusting a reading.**

---

## Open items — arrangements for the next session

Ordered so one sitting with the engine running clears the most.

### 1. Missed-call lamp — highest priority
The call-filtering logic was rewritten on 19 Sep and has **never run against a real call.** It now accepts only the default dialer, a system-image telephony package, or a known VoIP app. If the filter is wrong, calls silently stop reaching the cluster.

*Arrangement:* have someone ring the phone while the cluster is connected. Watch for the missed-call lamp. Then repeat with a WhatsApp call.
*Capture:* `Call from <pkg> who='<name>' - flagging cluster` in the log, or `Ignoring call-category notification from untrusted pkg=` if the filter rejected it.

### 2. Message lamp
*Arrangement:* send yourself a WhatsApp message and an SMS.
*Capture:* `Message from <pkg> who='<name>' - flagging cluster`.

### 3. Navigation-ended fix
*Arrangement:* start navigation, then end it. The whole nav block — arrow, distance, ETA, remaining km — should clear.
*Capture:* whether the ETA clock disappears this time.

### 4. Heartbeat cadence
*Arrangement:* none — just capture the log while connected.
*Capture:* interval between `a533...` packets. Expect one per beat, not bursts.

### 5. Pairing-screen question
*Arrangement:* force-stop the app, reopen it, see whether it reconnects on its own or asks to pair.
*Capture:* whether `saveSession` appears in the log on first connect.

### 6. Account separation
*Arrangement:* add a dummy emergency contact, sign out, sign in with a second throwaway account, open Safety.
*Capture:* whether the first account's contact is visible.

### 7. Release build
Blocked — needs a release keystore, which does not exist yet.

### 8. Code 36 direction
*Arrangement:* hold 34, then 36, and say whether they curl in **opposite** directions.
