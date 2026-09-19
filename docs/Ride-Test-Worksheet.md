# Ride Test Worksheet — navigation on a real route

**Reusable. Copy this file per ride, or just refill the tables.**
First use: 19 August 2026, commute to KL University Hyderabad (~35 km, cross-city
— plenty of turn types, which is what makes it a good first route).

The point of this sheet is that **the cluster and the log can be compared
afterwards**. You do not have to remember anything while riding.

---

## 1. Before setting off — 60 seconds

| Check | How |
|---|---|
| App says **Connected** | Dashboard, top right. Not "Offline" |
| Notification access on | Already granted; only re-check after a reinstall |
| Official Suzuki app | Must be **disabled or absent** — it takes BLE exclusively |
| Location | On, **High accuracy** |
| Maps | Start the route *after* the app shows Connected |

Then ride normally. **Do not stare at the cluster to test it.** The log records
every manoeuvre by itself.

---

## 2. What the app will send — the confirmed code table

Everything here was photographed on the rider's own Access 125. The 18 August
sweep confirmed 15 codes and overturned two long-standing errors.

### Bare bearing arrows — a complete 8-point compass

```
      19 ↖    40 ↑    41 ↗
      18 ←            42 →
      17 ↙    16 ↓    15 ↘
```

### Road-shaped junction turns (1–9)

| Code | Draws |
|---|---|
| 1 | turn left — shaft bending left at a junction |
| 2 | slight left — shaft then diagonal head |
| 3 | curving left (a bend, not a hairpin) |
| 4 | turn right — mirror of 1 |
| 5 | sharp right / hairpin |
| 6 | curving right — mirror of 3 |
| 7 | U-turn — up, over the top, back down |
| 8 | straight (same glyph as 40) |
| 9 | **destination — a ring with a filled centre** |

### Roundabouts (20–26)

| Code | Draws |
|---|---|
| 23 | roundabout, exit straight ahead |
| 20–22, 24–26 | other exits — **not yet swept** |

### Previously locked set (31–45, photographed 11 August)

31 keep left · 32 keep right · 33 crossroads · 34 sharp left · 35 turn right ·
36 sharp right · 37 turn left · 39 U-turn · 40 straight · 41 slight right ·
42 right (flat) · 43 ferry · 44 keep left w/ lanes · 45 roundabout

---

## 3. What Maps phrasing maps to which code

This is what the parser will actually send. **Two of these are new and have
never been ridden.**

| Maps says | Code sent | Cluster should draw |
|---|---|---|
| "Turn left" | 37 | curved road bending left |
| "Turn right" | 35 | curved road bending right |
| **"Slight left"** | **19** ⭐ | **up-left diagonal** — was 37 (a full left) until 18 Aug |
| "Slight right" | 41 | up-right diagonal |
| "Sharp left" | 34 | hairpin left |
| "Sharp right" | 36 | hairpin right |
| "Make a U-turn" | 39 | U-turn |
| "Keep left" / fork | 31 | Y-fork, left branch |
| "Keep right" / fork | 32 | Y-fork, right branch |
| "At the roundabout…" | 45 | generic roundabout (no exit number yet) |
| "Head north" / continue | 40 | straight up |
| **"Arriving…" / destination** | **9** ⭐ | **bullseye** — never seen on a route |
| "Merge" | 40 | straight — **not handled properly yet** |

⭐ = first real-world test.

---

## 4. Fill this in afterwards

Only for turns where something looked **wrong**. If it all looked right, say so
and move on — the log has the detail.

| # | Where (roughly) | Maps said | Cluster drew | Wrong how? |
|---|---|---|---|---|
| 1 | | | | |
| 2 | | | | |
| 3 | | | | |
| 4 | | | | |
| 5 | | | | |

**Specific things worth noticing:**

- **Slight left** — does 19 read correctly at a real junction, or is the shallow
  diagonal harder to read at speed than the old full-left arrow was?
- **Arrival** — does the bullseye (9) appear at college?
- **Timing** — does an arrow ever appear *late*, or show a turn already passed?
- **Roundabouts** — the generic icon gives no exit number. Is that a problem in
  practice, or good enough?

---

## 5. Known issues — expected, do NOT report these as new

| What you will see | Why |
|---|---|
| **`0m` in the next-turn field** | The cluster has two distance fields; we only drive one (bytes 18–22). The counting-down number appears in the *remaining* slot instead. The ride confirms which field is which. |
| **No message / call lamp** | The cluster ignores our alert bytes. Separate problem, needs a stationary test. |
| **Mileage says "Learning…"** | Needs the fuel bar to drop a segment. Not testable in one ride. |
| Signal bars | Field is 0–3; rider reports the dash draws 4. Open question, not a fault. |

---

## 6. Afterwards — pull the evidence

Plug the phone in and run these. **The ride log is the primary record**; logcat
is a bonus and may have rotated on a long ride.

```bash
"$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" pull /sdcard/Android/data/com.gnyaneshwar.rideconnectx/files/ride-log.txt
```

```bash
"$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" logcat -d -s RCX-Nav RCX-BLE > ride-logcat.txt
```

### Ride log format

Better than a bare list - it names the expected icon in plain English and
leaves a blank for you to fill in, so the log doubles as the worksheet:

```
==================================================
 ROUTE STARTED  2026-08-19 08:12:03
==================================================

[08:12:44]  Maps said : "Turn left onto MG Road"  (300 m)
            Cluster should show : LEFT   (code 37)
            Reached the scooter : yes
            What it ACTUALLY showed : ______________________
```

So after the ride you can fill the blanks straight into the file for any turn
that looked wrong, and leave the rest.

**`Reached the scooter : NO` matters more than anything else in the file.** It
means the packet never left the phone, so a missing arrow was a link problem,
not a wrong code. Scan for that line first.

---

## 7. Why this file exists

The 4 August sweep was thrown away because answers were collected in batches and
could not be matched to codes reliably. The fix was making each test
self-labelling. This sheet does the same job for a moving ride: the log records
what was sent, the rider records only what looked wrong, and the two are compared
afterwards rather than trusted to memory.
