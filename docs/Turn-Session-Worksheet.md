# Turn session worksheet

**Goal:** settle every turn arrow in one sitting, so turns can be signed off.
The rider's order (11 Sep 2026): turns first, perfectly, before anything else.

## Before starting

- **Engine running.** The 18 August sweep flattened the battery on ignition alone.
- Phone on the USB cable, our app holding the Bluetooth link (official Suzuki app off).
- Don't charge the phone during the session. It hit 49 °C on 21 August.

## Method — two rules

1. **`dist` = the code number.** The cluster prints the distance, so every screen
   labels itself and an answer can't be matched to the wrong code.
2. **One code at a time, held until the rider answers.** Stop with Ctrl-C.

```
tools/sweep-hold.sh 37
```

For each code, the rider says **what the arrow looks like** in plain words:
"curves left", "diagonal up-right", "circle, exit on the left" — not what they
think it should be.

## Session 2 plan (after the 11 Sep test)

**Step 0 — confirm the new turn glyphs (1 min).** Hold each, rider says what it looks like.
The rider wants "straight up, then curve to the side".

| Code | Expected | Rider sees | OK? |
|---|---|---|---|
| 1 | junction turn LEFT (shaft up, bends left) — now sent for Maps ↰ | | |
| 4 | junction turn RIGHT (mirror of 1) — now sent for Maps ↱ | | |
| 46 | nothing — sent when navigation ends | | |

If 1/4 are wrong, revert `TURN_LEFT`/`TURN_RIGHT` in `ProtocolEngine.Maneuver` to 37/35.

**Step 1 — the Dammaiguda loop again** (`tools/rcx-hyd-loop.gpx`, saved in Lockito as
"RCX Hyderabad loop"). Lefts and rights should now draw 1/4. At the end, close Maps:
the dashboard arrow must disappear (bug 1 fix).

**Step 2 — every other turn, held one at a time** (`tools/sweep-hold.sh <code>`):

| # | Code | Sent for | Rider sees | Correct? |
|---|---|---|---|---|
| 1 | 19 | slight left | | |
| 2 | 41 | slight right | | |
| 3 | 34 | sharp left (hairpin) | | |
| 4 | 36 | sharp right (hairpin) | | |
| 5 | 31 | keep left / fork left | | |
| 6 | 32 | keep right / fork right | | |
| 7 | 45 | roundabout, exit unknown | | |
| 8 | 20 | roundabout, sharp-left exit *(predicted)* | | |
| 9 | 21 | roundabout, left exit *(predicted)* | | |
| 10 | 22 | roundabout, slight-left exit *(predicted)* | | |
| 11 | 23 | roundabout, straight-on exit *(confirmed)* | | |
| 12 | 24 | roundabout, slight-right exit *(predicted)* | | |
| 13 | 25 | roundabout, right exit *(predicted)* | | |
| 14 | 26 | roundabout, sharp-right exit *(predicted)* | | |

Already good on 11 Sep, no need to repeat: straight (40), U-turn (39), arrival bullseye (9).

## After the session

Whatever the rider reports goes straight into `MapsArrowCatalog.kt` and
`tools/cluster-codes.csv`. Then one Lockito route through a few turns and a
roundabout, to see it end to end.
