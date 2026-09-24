# RideConnectX — Test Evidence

Every feature of RideConnectX, and whether it has been verified on the real device: a Suzuki Access 125 cluster paired with a OnePlus Nord CE 3.
Each result is recorded by the owner running the test by hand. Photos, screen recordings and log excerpts are added to the folder for each section as they are captured.

Source code: [`main`](../../tree/main)

## Summary

| Verified | Partly verified | Failing | Not yet tested | Not in this release | Total |
|---|---|---|---|---|---|
| 44 | 7 | 3 | 31 | 5 | 90 |

## A. Vehicle link and cluster

| ID | Feature | Status | Verified on |
|---|---|---|---|
| A1 | Scan finds the cluster | ⏳ Not yet tested | — |
| A2 | First pairing connects | ⏳ Not yet tested | — |
| A3 | Auto-reconnect on app start | 🟡 Partly verified | — |
| A4 | Reconnect after out of range | ⏳ Not yet tested | — |
| A5 | Telemetry decode | ✅ Verified | 19 Sep 2026 |
| A6 | Checksum rule | ✅ Verified | 19 Sep 2026 |
| A7 | Rider name on cluster | ⏳ Not yet tested | — |
| A8 | Phone battery on cluster | ⏳ Not yet tested | — |
| A9 | Phone signal on cluster | ⏳ Not yet tested | — |
| A10 | Heartbeat cadence | ⏳ Not yet tested | — |
| A11 | Survives screen locked | ⏳ Not yet tested | — |
| A12 | Survives a phone call | ⏳ Not yet tested | — |
| A13 | Bluetooth off mid-ride | ⏳ Not yet tested | — |

## B. Navigation

| ID | Feature | Status | Verified on |
|---|---|---|---|
| B1 | All 18 turn codes draw right | ✅ Verified | 19 Sep 2026 |
| B2 | Live Maps turns reach cluster | ✅ Verified | 19 Sep 2026 |
| B3 | Distance counts down live | ✅ Verified | 19 Sep 2026 |
| B4 | Code 36 direction | 🟡 Partly verified | — |
| B5 | Nav clears when route ends | ⏳ Not yet tested | — |
| B6 | Stale nav clears if Maps freezes / after arrival | ⏳ Not yet tested | — |
| B7 | Arrival behaviour | ⏳ Not yet tested | — |
| B8 | Re-route after missed turn | ⏳ Not yet tested | — |
| B9 | In-app nav screen matches cluster | ⏳ Not yet tested | — |
| B10 | Saved places | ⛔ Not in this release | — |
| B11 | km / miles setting | ⏳ Not yet tested | — |

## C. Notifications

| ID | Feature | Status | Verified on |
|---|---|---|---|
| C1 | Missed-call lamp | ⏳ Not yet tested | — |
| C2 | WhatsApp call lamp | ⏳ Not yet tested | — |
| C3 | Message lamp — WhatsApp | ⏳ Not yet tested | — |
| C4 | Message lamp — SMS | ⏳ Not yet tested | — |
| C5 | Unknown apps rejected (security) | ⏳ Not yet tested | — |
| C6 | Music etc. ignored | ⏳ Not yet tested | — |
| C7 | In-app notification list | ⏳ Not yet tested | — |
| C8 | Notification access revoked | ❌ Failing — fix in progress | 24 Sep 2026 |

## D. Safety and SOS

| ID | Feature | Status | Verified on |
|---|---|---|---|
| D1 | Add contact by typing | ✅ Verified | 20 Sep 2026 |
| D2 | Add contact from picker | ✅ Verified | 20 Sep 2026 |
| D3 | First contact becomes primary | ✅ Verified | 20 Sep 2026 |
| D4 | Deleting primary reassigns it | ✅ Verified | 24 Sep 2026 |
| D5 | Contact limit | ✅ Verified | 24 Sep 2026 |
| D6 | SOS opens dialer, never auto-calls | ✅ Verified | 20 Sep 2026 |
| D7 | Location share link | ✅ Verified | 20 Sep 2026 |
| D8 | Location off fails safely | ✅ Verified | 20 Sep 2026 |
| D9 | Approximate-location warning | ❌ Failing — fix in progress | 24 Sep 2026 |
| D10 | 112 emergency dial | ✅ Verified | 20 Sep 2026 |

## E. Service

| ID | Feature | Status | Verified on |
|---|---|---|---|
| E1 | Service record saves | ✅ Verified | 20 Sep 2026 |
| E2 | Odometer minimum enforced | ✅ Verified | 20 Sep 2026 |
| E3 | Bad-reading guard (6,001,923 km) | ⏳ Not yet tested | — |
| E4 | Service-due maths | ✅ Verified | 20 Sep 2026 |
| E5 | 6 default tasks | ✅ Verified | 20 Sep 2026 |
| E6 | Edit task interval | ✅ Verified | 20 Sep 2026 |
| E7 | Refuel entry | ⛔ Not in this release | — |
| E8 | Reminder fires | ⏳ Not yet tested | — |
| E9 | Odometer card | ✅ Verified | 20 Sep 2026 |

## F. Rides

| ID | Feature | Status | Verified on |
|---|---|---|---|
| F1 | Ride recorded | ⏳ Not yet tested | — |
| F2 | Distance / time right | ⏳ Not yet tested | — |
| F3 | Stats filters | ✅ Verified | 24 Sep 2026 |
| F4 | Chart | ✅ Verified | 24 Sep 2026 |
| F5 | Rides reach the cloud | ⏳ Not yet tested | — |

## G. Account and profile

| ID | Feature | Status | Verified on |
|---|---|---|---|
| G1 | Google sign-in | ✅ Verified | 23 Sep 2026 |
| G2 | Email sign-up / sign-in | 🟡 Partly verified | 24 Sep 2026 |
| G3 | Guest mode | ✅ Verified | 24 Sep 2026 |
| G4 | Guest → account keeps data | ❌ Failing — fix in progress | 24 Sep 2026 |
| G5 | Sign-out clears profile | ✅ Verified | 24 Sep 2026 |
| G6 | Account separation | ✅ Verified | 24 Sep 2026 |
| G7 | Photo from gallery | ✅ Verified | 24 Sep 2026 |
| G8 | Photo + name from Google | ✅ Verified | 21 Sep 2026 |
| G9 | Profile restored on sign-in | ✅ Verified | 23 Sep 2026 |
| G10 | Vehicle and colour | ✅ Verified | 24 Sep 2026 |
| G11 | Delete local data | ✅ Verified | 24 Sep 2026 |
| G12 | Delete account | ✅ Verified | 21 Sep 2026 |

## H. Settings and onboarding

| ID | Feature | Status | Verified on |
|---|---|---|---|
| H1 | Light / dark / system | ✅ Verified | 23 Sep 2026 |
| H2 | Language switch | ⛔ Not in this release | — |
| H3 | Auto-connect toggle | ✅ Verified | 24 Sep 2026 |
| H4 | Onboarding runs once | ✅ Verified | 23 Sep 2026 |
| H5 | Permission flow | 🟡 Partly verified | 24 Sep 2026 |
| H6 | Battery / background prompt | ✅ Verified | 24 Sep 2026 |
| H7 | Terms and privacy recorded | ✅ Verified | 23 Sep 2026 |
| H8 | About screen version | ✅ Verified | 23 Sep 2026 |

## I. Security and release

| ID | Feature | Status | Verified on |
|---|---|---|---|
| I1 | Audit fixes on device | ⏳ Not yet tested | — |
| I2 | Release build runs | ⛔ Not in this release | — |
| I3 | No debug logs in release | ⏳ Not yet tested | — |
| I4 | App Check enforced | ⛔ Not in this release | — |
| I5 | Firebase rules published | ✅ Verified | 21 Sep 2026 |
| I6 | Database upgrade | 🟡 Partly verified | 19 Sep 2026 |
| I7 | Play data-safety form | ⏳ Not yet tested | — |
| I8 | Privacy policy public link | ✅ Verified | 21 Sep 2026 |

## R. Rider requests

| ID | Feature | Status | Verified on |
|---|---|---|---|
| R1 | SOS share continues by itself after permission / location-on, and says what it waits for | 🟡 Partly verified | 24 Sep 2026 |
| R2 | Service centre: past centres as suggestions + "Find nearby on Maps" (India-wide list needs paid Places API — not built) | 🟡 Partly verified | 24 Sep 2026 |
| R3 | Odometer rule follows the service date (back-fill old services) | ✅ Verified | 24 Sep 2026 |
| R4 | Suggested notes chips | ✅ Verified | 24 Sep 2026 |
| R5 | Brighter, larger centre/notes text | ✅ Verified | 24 Sep 2026 |
| R6 | "—" when no reading | ✅ Verified | 24 Sep 2026 |

## How evidence is filed

Files are named `<ID>-<short-description>`, for example `B3-roundabout-exit-2.jpg`, and kept in a folder per section (`A-vehicle-link/`, `B-navigation/`, …). Long recordings are published as [release](../../releases) attachments and linked from the table.

_Last updated: 24 Sep 2026_
