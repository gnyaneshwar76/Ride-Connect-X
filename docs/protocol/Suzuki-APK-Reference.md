# Suzuki Ride Connect APK — extracted reference

Everything worth knowing that was pulled out of the official app, so the APK
never has to be opened again. Written 11 August 2026.

**Source:** `Suzuki Ride Connect (1).apk`, 184 MB, obtained by the rider from
the Play Store on 11 August 2026. Decompiled sources of an earlier build live at
`C:\Users\eshwa\suzuki-ride-connect-analysis\jadx-code\sources`.

**Not recorded here, deliberately:** Suzuki's API keys, tokens and account
secrets. They exist in `res/values/strings.xml` and the Mappls SDK config inside
the APK. They are not needed to talk to the cluster, and copying someone else's
credentials into this repo would be wrong and would make the repo unpublishable.
If a genuine need ever arises, they can be read straight from the APK.

---

## 1. How the icons were decoded

The maneuver icons are Android vector drawables at `res/drawable-nodpi-v4/
ic_step_<mapplsId>.xml`, compiled to binary XML. The arrow geometry is in
`pathData` strings inside each file's string pool, which is readable without any
external tooling.

Scripts used (kept in the session scratchpad, reproduce in minutes if needed):

- `axml.py` — reads the string pool out of compiled AXML, prints every `pathData`
- `classify.py` — finds the arrowhead (the 3-point polyline whose middle point is
  the tip) and reports which way it points

55 icons exist: mappls ids `0-8, 10-25, 36, 37, 40, 41, 50-75`.

---

## 2. Mappls maneuver id → cluster code

From `ViewOnClickListenerC4857A0`, the chain that sets `f17223e0`. This is the
value that ends up at `bytes[2]` of the `0x31` navigation packet.

```
 0→1    1→2    2→3    3→4    4→5    5→6    6→7    7→8   8/9/10→9
75→10  11→11  12→12  13→13  14→14  53→15  54→16  55→17  56→18  57→19
65→20  66→21  67→22  68→23  69→24  70→25  71→26  19→27  20→28  17→29
18→30  15→31  16→32  21→33  22→34  23→35  24→36  25→37  73→38  41→39
50→40  51→41  52→42  72→45  26/27/28→31  30/31→32
59→47  60→48  61→49  62→50  63→51  64→52
```

**51 mappings, cluster codes 1 to 52.** That is the entire range the official app
can ever send — the "length of the road". Not 256.

Mappls ids are *not* sequential (they jump 53-57, 59-71, 75), which is why no
pattern was ever guessable from the cluster side.

`i = 46` is hardcoded as the value sent when GPS is not locked — a deliberate
blank. Our own sweep independently found 46 draws nothing. Source and hardware
agree.

---

## 3. What each icon is

Read from arrowhead bearing. **Confidence is marked** — photographed entries are
ground truth, the rest are inference from the APK.

### The basic set — cluster 1-9

| Cluster | Meaning | Confidence |
|---|---|---|
| 1 | turn left | inferred (mappls 0) |
| 2 | slight left | inferred |
| 3 | sharp left | inferred |
| 4 | turn right | inferred (mirror of 0) |
| 5 | slight right | inferred |
| 6 | sharp right | inferred |
| 7 | U-turn | inferred (tip down) |
| 8 | straight | inferred (tip centre-up) |
| 9 | roundabout | inferred (concentric circles) |

### The arc set — cluster 15-19, 40-42 ⭐

The eight `big-arc` icons (mappls 50-57) are one family covering eight bearings.
**Three of them match photographs exactly**, which is what makes this table
trustworthy.

| Cluster | Tip bearing | Meaning | Confidence |
|---|---|---|---|
| 40 | centre-up | straight | ✅ **photographed** |
| 41 | right-up | slight right | ✅ **photographed** |
| 42 | right-middle | right | ✅ **photographed** |
| 15 | right-down | sharp right | inferred |
| 16 | centre-down | U-turn | inferred |
| 17 | left-down | sharp left | inferred |
| 18 | left-middle | left | inferred |
| 19 | left-up | **slight left** | inferred |

**Cluster 19 is the slight-left that appeared not to exist.** The 11 August sweep
covered 31-45 only and concluded no up-left diagonal was drawn; it is at 19.

### Roundabouts — cluster 20-26 and 47-52

Two complete sets of roundabout icons, each with the exit arrow at a different
bearing.

| Cluster | Exit bearing | | Cluster | Exit bearing |
|---|---|---|---|---|
| 20 | left-down | | 47 | right-middle |
| 21 | left-middle | | 48 | right-up |
| 22 | left-up | | 49 | centre-up |
| 23 | centre-up | | 50 | left-middle |
| 24 | right-up | | 51 | left-down |
| 25 | right-middle | | 52 | left-down |
| 26 | right-down | | | |

Two sets most likely means clockwise and anticlockwise — left-hand and
right-hand-drive countries.

### Crossroads and ramps — cluster 11-14, 27-30

| Cluster | Meaning | Confidence |
|---|---|---|
| 11 | left at a crossroads | inferred |
| 12 | right at a crossroads | inferred |
| 13 | destination on the left | inferred |
| 14 | destination on the right | inferred |
| 27 | ramp/fork left-up | inferred |
| 28 | ramp/fork right-up | inferred |
| 29 | ramp/fork left-down | inferred |
| 30 | ramp/fork right-down | inferred |
| 10 | possibly the destination flag | weak — square shape, mappls 75 |

### 🔒 Photographed and locked — cluster 31-45

Ground truth from the vehicle, 11 August 2026. **Do not change without the
rider's say-so.** See the knowledge base for the full table; in brief:

31 keep left · 32 keep right · 33 crossroads · 34 sharp left · 35 right ·
36 sharp right · 37 left · 38 left with lane bar · 39 U-turn · 40 straight ·
41 slight right · 42 right flat · 43 ferry · 44 keep left lanes · 45 roundabout

---

## 4. Where the inference disagrees with the photographs

Honesty matters more than a tidy table.

In the **31-37 band** the APK inference and the photographs **conflict**: the
mapping says cluster 35 comes from mappls 23 (tip left-up), but the photograph
of cluster 35 is clearly a right turn.

Two possible causes, unresolved:

1. The mapping extraction used a simple `j == N` regex and will have missed
   branches with compound conditions, so some pairs may be wrong.
2. The phone's icon set and the cluster's icon ROM are not identical.

**Where they overlap and agree (40, 41, 42) the match is exact.** So the
inference is good but not proof. Treat every "inferred" row above as a strong
prediction to be confirmed by looking, not as fact.

---

## 5. Packet formats (already confirmed on hardware)

Full detail in `RideConnectX-Knowledge-Base.md`. Summary:

| Packet | Purpose |
|---|---|
| `0x06` | notification text — how WhatsApp/SMS reach the cluster |
| `0x31` | navigation — maneuver at byte 2, distance, clock |
| `0x33` | status — battery, signal, clock, message/call flags |
| `0x36` | rider profile — the WELCOME line |
| `0x37` | telemetry from the scooter — odometer, Trip A, Trip B, fuel |

Service `0000fefb-…`, write on `00000001` (WRITE_NO_RESPONSE), notify on
`00000002`. 30-byte frames, `A5` … checksum@28 … `7F`.

---

## 6. POI categories (`assets/categories.json`)

The "Custom POI Categories" feature from the rider's report. 20 categories, each
with a Mappls search code. Useful if RideConnectX ever adds nearby-search.

| Group | Categories |
|---|---|
| Utilities | ATMs, EV Charging, Petrol Pump, Post Offices, CNG Station |
| Eat & Drink | Coffee, Restaurant, Pubs & Bars |
| Shopping | Shopping, Groceries |
| Emergencies & Hospitals | Hospitals, Police Stations |
| Transport | Parking, Transport |
| Health and wellness | Pharmacy, Spas |
| Bank | Banks |
| Entertainment | Entertainment |
| Hotels | Hotels |
| *(mislabelled group)* | Toilets |

Codes follow a six-letter scheme — `FODCOF` coffee, `FODIND` restaurant,
`FODPUB` pubs, `FINATM` ATMs. Re-read `assets/categories.json` for the full
code list if the feature is ever built.

Also in `assets/`: `sounds/ding.mp3` (the alert tone) and the Roboto font family.
`classes.dex` is 7.3 MB — decompile that if app *logic* is ever needed again;
everything relevant so far is already captured in this file.

---

## 7. What to verify next, cheaply

Six codes would confirm or kill the whole inference table:

| Code | Predicted |
|---|---|
| 19 | slight left ⭐ the missing piece |
| 18 | left |
| 16 | U-turn |
| 8 | straight |
| 1 | turn left |
| 23 | roundabout, exit straight ahead |

If 19 and 18 come back as predicted, the arc-set table is proven and we gain
slight-left plus a second full turn set. If 1 and 8 also match, the basic set is
proven too and only the roundabouts remain.
