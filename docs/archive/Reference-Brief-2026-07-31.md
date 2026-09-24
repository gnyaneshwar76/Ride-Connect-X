# RideConnectX — PM Reference Brief (31 July 2026)

Companion to `the reference report PDF (kept locally, not in the repo)`, which is the PM's own
annotated walkthrough of the **official Suzuki Ride Connect app**, photographed
screen by screen, plus criticism of the RideConnectX build at that date.

**Standing rule: be inspired by the original, do not clone it.** Match the flow
and the logic. Do not match the visual style — the original is dated.
RideConnectX must look premium.

---

## Part A — How the official app works

### A1. Onboarding carousel
Runs once on first launch, roughly 20 pages, each with dot indicator, SKIP and
NEXT; the final page has FINISH. Page sequence: pairing with the console →
pairing in the app → ambient weather → calendar alerts → weather alerts →
traffic alerts → turn-by-turn (set destination) → turn-by-turn (start) → view
trip records → trip statistics → favourite locations (up to 10) → custom POI
categories → digital wallet and service alerts → renewal alerts → fuel
consumption → 360° vehicle view → user guide and support → permissions.

Each page pairs a screenshot of the feature with a photo of the scooter's
instrument cluster showing how it appears there.

### A2. Permission requirements
| Permission | Stated reason |
|---|---|
| Contacts | display caller name and number on the cluster |
| Make/manage calls | auto-disconnect incoming calls while riding |
| Call logs | display missed call count on the cluster |
| Location | real-time turn-by-turn navigation on the cluster |
| Photos/media/files | set and edit the rider's profile picture |
| Bluetooth | exchange data between cluster and application |

Notification access is granted through Android system settings, not a runtime
dialog.

### A3. Permission pattern
The original **never** fires a bare system permission dialog. Every time it
shows its own styled rationale sheet first, then the system dialog. Example for
location: explains auto-detecting the city during profile setup, enabling
turn-by-turn on the cluster over Bluetooth, and keeping navigation alive when
the phone is locked. Same pattern for SIM incoming calls, SMS, WhatsApp call
log and contacts — each naming what the cluster will display.

### A4. Profile creation — runs BEFORE the dashboard
Screen title **Create Profile**. Header has an avatar placeholder with a camera
badge, the rider's name and the location.

Fields in order:
1. **Rider's Name** — free text
2. **Location** — read-only with a crosshair icon; tapping it fetches the city
   via GPS. A "Tap Here!" coach mark points at the icon on first run.
3. **Choose your vehicle type** — sub-screen: Scooter, Motorcycle
4. **Choose your vehicle model** — sub-screen, one card per model with a photo
5. After a model: a colour swatch row with the colourway name (e.g. "Metallic
   Sonoma Red / Pearl Mirage White") and a render in that colour
6. **T&C and Privacy Policy checkbox** — must be ticked to proceed

Models listed: Burgman Street-TFT Edition, e-ACCESS, Access-TFT Edition,
Access, Access 125, Avenis, Burgman Street, Burgman Street EX.
**The PM's scooter is Access 125.**

### A5. Dashboard
Top row: vehicle name (large), sync status line, rider avatar and name right.
Sync line states: `Last Sync: - Not Paired` or `Paired | 11:26 pm`.

Body: large render of the selected scooter in the chosen colour; **Fuel Level**
as a five-segment E–F bar; weather chip (temperature + icon, city above); four
stat cards in a 2×2 grid — ODO Meter, Total Trips, Trip A, Trip B. Primary
action button reads PAIR WITH SUZUKI with a Bluetooth glyph when unpaired, and
shows the vehicle name when paired. Bottom navigation has four tabs: dashboard,
settings, navigation, more.

Real captured values when paired: ODO 1005 km, Total Trips 0, Trip A 5.7 km,
Trip B 482.6 km, fuel bar full, 26.8 °C.

### A6. Pairing screen
Title CONNECTING TO SUZUKI. Three states on one screen:
1. **Searching** — grey pulsing Bluetooth circle, caption SEARCHING VEHICLES,
   red TAP TO REFRESH at the bottom
2. **Found** — circle turns cyan, caption TAP TO PAIR, and **one** result listed
   by name only: `SAS210217219`
3. **Connected** — cyan circle, device name shown, button becomes red
   TAP TO DISCONNECT

No MAC addresses. No signal strength. No debug controls. Only Suzuki vehicles
ever appear.

### A7. Cluster behaviour on connect
Photographed from the scooter's own display: on a successful connection the
cluster shows

```
WELCOME
GNYANESHWAR
```

then returns to the normal readout. The rider's profile name is sent to the
cluster as part of the connection handshake. The cluster also shows a Bluetooth
glyph and a phone signal indicator while connected.

### A8. Settings
Pair with Suzuki action button; profile card (avatar, name, vehicle model,
chevron to Rider Profile); Dark Mode toggle; Notifications group — master
toggle, SIM Incoming Calls, SIM Incoming SMS, WhatsApp Calls, WhatsApp
Messages, Auto-Reply SMS (standard SMS charges apply), Save All Trips, Speed
Exceeding Alert. A Trip Alert dialog explains that 10 trips are kept for Recent
and Favourites, and the oldest is deleted when an 11th is added.

### A9. Rider Profile screen
Avatar, name, location, a card with the vehicle render and model name, and an
**Overall Stats** section with ODO Meter and Total Trips.

### A10. Background behaviour
A persistent foreground notification reads `Suzuki Ride Connect is running.`

---

## Part B — Issues raised 31 July 2026

### B1. Email/password signup does not persist — **FIXED 2 Aug 2026**
An account created with email and password appeared to succeed, but the address
never showed up in the database. Only Google sign-ins appeared. Guest and Google
were working and were not to be touched.

**Cause:** `AuthViewModel` is scoped to the Sign In nav back stack entry.
`createUserWithEmailAndPassword` signs the user in the moment it returns, which
flips the auth state and navigates away with `popUpTo(SIGN_IN){inclusive}` —
clearing the ViewModel and cancelling its `viewModelScope`. The email path has
one more suspension point than Google does (`updateProfile`), so it was
reliably cancelled there, before the Firestore write was ever issued.

**Fix:** sign-in and sign-up persistence now runs on an application-scoped
coroutine (`@ApplicationScope`), so navigation cannot cancel it. A second,
independent bug was found and fixed at the same time: `UserPreferencesStore`
never stored the Firebase uid, so `UserSession.syncsToCloud` was always false
and every cloud write keyed off the local session was silently skipped.

### B2. Vehicle selection missing — **DONE 2 Aug 2026**
Added `CreateProfileScreen` between permissions and the dashboard, following
A4: rider name, location with GPS crosshair, vehicle type sheet, model sheet
with artwork, colour swatch row with the colourway name and a render in that
paint, and a T&C checkbox gating Continue. The dashboard reads all of it back.

### B3. Cannot connect to the scooter — **wiring fixed, hardware test pending**
The scooter was visible in the scan but tapping Connect did nothing.

**Cause:** `ScanViewModel` handled `SelectDevice` by emitting a navigation
effect that nothing collected. `ConnectToDeviceUseCase` existed but was never
called from anywhere. No GATT connection was ever attempted.

**Also fixed in the same layer:**
- `connectGatt` now passes `TRANSPORT_LE` explicitly (the 3-arg overload
  defaults to `TRANSPORT_AUTO`, which on a dual-mode device — and the cluster is
  dual-mode, it carries calls and SMS — can pick BR/EDR and fail with 133)
- all GATT calls are posted to the main thread (they were running on
  `Dispatchers.IO` via `flatMapLatest`)
- status-133 retry with close + settle delay, up to 3 attempts
- the connection-state `SharedFlow`'s replay cache is cleared before each
  attempt, and the subscription is made before the connect is issued, so a stale
  `DISCONNECTED` is no longer replayed into a fresh attempt
- GATT is closed in `onDestroy` and `onTaskRemoved`
- a crash was found and fixed: starting the `connectedDevice` foreground
  service without a Bluetooth runtime permission threw `SecurityException`
  straight out of `onCreate` and killed the process — reproduced by skipping the
  permission step and opening Pair Vehicle

**Open blocker — the cluster greeting.** Making the cluster display
`WELCOME <name>` requires Suzuki's proprietary GATT service and characteristic
UUIDs and its packet format. These are not published and cannot be guessed. The
service now logs the complete GATT profile on every connect and subscribes to
every notify characteristic, so one real connection to `SAS210217219` produces
the data needed. See "What is needed next" below.

---

## Part C — Older issues

| # | Issue | Status |
|---|---|---|
| 1 | Scanner shows every nearby BLE device | Fixed — filtered to `SAS*`/Suzuki names; MAC and dBm removed from the row |
| 2 | Scanning never stops | Fixed earlier — 30 s timeout in `BleScannerImpl` |
| 3 | No dark mode | Dark is the default palette; a user-facing toggle is still to do |
| 4 | App name under the system navigation bar | To verify |
| 5 | Dashboard shows `--` and "Waiting for…" | Blocked on B3 — needs real telemetry |
| 6 | No way to disconnect | Fixed — Disconnect action on the pairing screen |
| 7 | Back from connect still lists other devices | Improved by the filter; re-check on hardware |
| 8 | Closing from recents leaves the connection alive | Fixed — `onTaskRemoved` closes GATT and stops the service |
| 9 | Scooter powered off does not update state | Partly — disconnect is now surfaced; auto-reconnect not implemented |

---

## Part D — Working rules

- **Do not break what works.** A previous dark-mode change introduced a crash
  that blocked the project for days.
- **One thing at a time.** B1, then B2, then B3. Do not bundle.
- **Evidence before code.** For any crash or connection failure, state the
  exception, class, method and line before proposing a change.
- **Report blockers.** Never invent an architectural decision to work around
  something. Stop and ask.
- **The design is frozen.** Port the approved design faithfully. Do not redesign
  screens while fixing logic.

---

## What is needed next for B3

The connection path is wired and crash-free, but **it cannot be verified on an
emulator** — the Android emulator has no BLE radio (its adapter sits in
`BLE_TURNING_ON` and never completes). One run on the PM's phone next to the
scooter is required.

To capture what is needed:

```bash
adb logcat -c && adb logcat -s RCX-BLE
```

Then open Pair Vehicle, tap Connect on `SAS210217219`, and send the log. It will
contain the full `GATT PROFILE` block — every service and characteristic UUID
with its properties — plus any notification payloads the cluster pushes. That is
what the welcome handshake and the live telemetry parsing have to be built from.

---

## Build note

`applicationId` is `com.gnyaneshwar.rideconnectx` while the Kotlin namespace is
`com.eshwar.rideconnectx`. Launch and clear the app by the **applicationId**:

```bash
adb shell am start -n com.gnyaneshwar.rideconnectx/com.eshwar.rideconnectx.MainActivity
```

An older install under `com.eshwar.rideconnectx` can linger on a device and is
easy to mistake for the current build. Uninstall it if present.
