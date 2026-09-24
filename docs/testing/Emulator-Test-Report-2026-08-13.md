# Emulator test report — 13 August 2026

Screens 18-23, walked end to end on a running emulator
(`sdk_gphone16k_x86_64`, 1344×2992, Android 16). Every claim below was
observed on the device, not inferred from the code.

**Result: all six screens work. Three real defects were found, fixed and
re-verified on the device.** One more was found by reading and fixed.

---

## What was exercised

| Screen | Exercised | Outcome |
|---|---|---|
| 18 Service | Empty state, add record, save, edit/delete, validation, reminders toggle | ✅ after fixes |
| 19 Safety | SOS sheet, contacts, add/duplicate/invalid phone, dialer intent, first-aid card | ✅ |
| 20 Settings | All six sections, Units picker, disabled rows, version row | ✅ after fix |
| 21 Profile | Account card, vehicle card, account actions | ✅ |
| 22 Appearance | Theme switch, text size, preview, accent swatches | ✅ |
| 23 About | Version/build from BuildConfig, information and support sections | ✅ |

Also confirmed:

- **The Room migration path works.** The emulator had a v2 database from the
  previous build; it upgraded to v4 (service records + emergency contacts) with
  no crash and no data loss.
- **No app crash anywhere in the session.** `logcat -b crash` shows one native
  abort — it is inside the emulator's own Bluetooth stack
  (`libbluetooth_jni.so`, `com.android.bt` apex), not our process. The
  emulator has no BLE radio, which is already a known trap.
- **Theme and text size survive a reinstall and a cold start.**
- **The dialer opens, and nothing dials itself.** Tapping "Call Amma" launched
  `com.google.android.dialer` via `ACTION_DIAL`. No call was placed, exactly as
  the screen promises.

---

## Defect 1 — a mistyped service record could not be undone 🔴

**Found:** a record entered as 160,290 km was deleted, and the screen still read
**"Overdue · 155,688 km over"** with `Last 1,602 km / Due 4,602 km`. The
Dashboard's Service Reminder card showed the same. Nothing in the app could
recover it short of clearing app data.

**Cause:** `ServiceRepository.addRecord` wrote the record's reading into
`ServicePreferencesStore.lastKnownOdometerKm`, and that cache only ever moves
forward — deliberately, so a garbled telemetry frame cannot rewrite a good
reading. Deleting the record removed the row but not the cached number.

**Fix:** a manually entered reading is no longer written to the cache. Nothing
is lost: `status` already takes the higher of the cache and the latest record,
so a record still raises the current reading for as long as it exists. The
cache stays what it claims to be — what the *vehicle* last reported.

**Verified:** deleting the bad record now restores `Last 995 mi / Due 2,860 mi`
and "On schedule".

## Defect 2 — the Units setting was stored and never honoured 🟠

**Found:** selecting **Miles** in Settings persisted (the radio stayed on
Miles), but every distance in the app still read `km` — Service, Upcoming
Tasks, history, reminders and the Dashboard card.

**Cause:** `AppSettingsStore` saved the preference; no screen read it.
`ServiceScreen` had a private `formatKm` that hardcoded `"%,d km"`.

**Fix:** `DistanceUnit` now converts and formats (1 mile = 1.609344 km,
exactly), and a `LocalDistanceUnit` composition local carries the rider's choice
into the cards. **Distances are still stored in kilometres** — converting on
the way into storage would corrupt the data the moment the rider changed their
mind.

**Verified:** with Miles selected, 3,000 km → **1,864 mi**, 1,602 km →
**995 mi**, 4,602 km → **2,860 mi**, throughout Service and the Dashboard.

## Defect 3 — the pre-filled odometer field appended instead of replacing 🟠

**Found:** the Add Service Record sheet pre-filled the odometer with the last
known reading (1602). Tapping the field and typing `900` produced **160,290 km**
— the caret lands where the finger lands, so the digits were appended and then
truncated to six.

**Fix attempts, in order — recorded because the first two failed on device:**

1. Selecting the text at construction — lost, because the tap that focuses the
   field also places the caret.
2. Selecting on first focus via `onFocusChanged` — also lost; the touch's caret
   placement runs after the focus callback.
3. **Removing the pre-fill.** The field now starts empty. The last known
   reading is shown *under* the field, where it informs without being typed
   into. This was a convenience invented during the build, not something the
   design asked for.

**Verified:** typing `900` now gives exactly 900, and the validation fires as
designed — *"Must be at least 995 mi — the odometer cannot go backwards."*

## Defect 4 — the splash screen lied about the version 🟡

Found by reading, after noticing the splash said **v2.5.0** while About said
**1.0 (1)**. It was a hardcoded string in `SplashScreen.kt`. It now reads
`BuildConfig.VERSION_NAME`, so the two screens cannot disagree again.

---

## Confirmed working, with evidence

- **Service validation.** Empty odometer → *"Enter the odometer reading."*
  Lower odometer → *"Must be at least 995 mi…"*. Future dates rejected.
- **Service schedule maths.** One record at 1,602 km on 13 Aug produced
  `REMAINING 3,000 km`, `DAYS LEFT 90`, `Last 1,602 km`, `Due 4,602 km`, and
  Upcoming Tasks reading "in 3,000 km" / "in 6,000 km" per item.
- **Delete confirmation** names the record it is about to remove.
- **Emergency contacts.** `123` → *"That does not look like a phone number."*
  A second contact with the same number → *"Amma already has this number."*
  The first contact added is promoted to the SOS contact automatically.
- **SOS sheet** reads "SOS reaches Amma first", offers the contact, 112, and
  location sharing, and states plainly that it does not place calls.
- **Appearance** applies instantly — no restart — and the whole app follows,
  including the Dashboard and every card.
- **Settings** disabled rows (Language, Default navigation app, Marketing)
  render dimmed with the reason, as the design asks.
- **About** reads version and build from `BuildConfig`.

## Known, not defects

- **The system splash is always dark**, even with the Light theme selected.
  That is the Android 12+ splash `windowBackground`, fixed at install time and
  not something a runtime theme can change. Cosmetic, and only for ~1 second.
- **"No vehicle selected"** on Settings and Profile is correct — this emulator
  account genuinely has no vehicle chosen.

## Not testable on an emulator

Everything BLE: pairing, telemetry, the odometer feeding the service schedule
from a real frame, and the cluster itself. The emulator has no BLE radio.
Those need the rider's phone and the scooter.

---

## Test suite

**60 unit tests, 0 failures** (was 31). Two new files pin what the emulator
found:

- `ServicePlanTest` — schedule arithmetic, overdue by distance *and* by time,
  progress taking whichever is further along, unknown odometer staying unknown
  rather than reading zero, and the km↔mi conversion.
- `SafetyRepositoryTest` — phone normalisation (four spellings of one number
  collapsing to one key) and the length rule.
