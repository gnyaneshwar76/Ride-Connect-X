# Test matrix — everything in the app

Every feature, its evidence status, and how to settle it. The goal is a release where each line below reads **proven**, backed by a text record in [`Hardware-Evidence-Log.md`](Hardware-Evidence-Log.md).

**Status has three values, and only one of them is a claim:**

| | |
|---|---|
| **PROVEN** | Observed working on real hardware and written down |
| **UNTESTED** | Written and compiles. Nobody has seen it work |
| **PARTIAL** | Some of it observed, a named part not |

23 screens, 15 view models, 9 repositories. As of 20 September 2026: **6 proven, 3 partial, the rest untested.**

---

## A. Vehicle link and cluster — the core

| # | Feature | Status | How to settle it |
|---|---|---|---|
| A1 | BLE scan finds the cluster | UNTESTED | Open pairing screen with the scooter on. Does `SAS210217219` appear? |
| A2 | First pairing connects | UNTESTED | Tap it. Does the dashboard go live? |
| A3 | Auto-reconnect on app start | **PARTIAL** | Force-stop, reopen. Does it reconnect, or ask to pair? *(Showed the pairing screen after reinstall — cause unknown)* |
| A4 | Reconnect after walking out of range | UNTESTED | Walk 30 m away, come back. Does it recover unaided? |
| A5 | Telemetry decode | **PROVEN** | Cluster read 002427 km, app parsed 2427 km — two sources agreeing |
| A6 | Checksum rule | **PROVEN** | Live frame: sum `0x8F`, complement `0x70`, byte 28 `0x70` |
| A7 | Rider name on the cluster (WELCOME line) | UNTESTED | Set a rider name, reconnect. Does the cluster greet you by it? |
| A8 | Phone battery shown on cluster | UNTESTED | Compare cluster battery bars to the phone's actual level |
| A9 | Phone signal shown on cluster | UNTESTED | Compare signal bars |
| A10 | Heartbeat cadence | UNTESTED | `tools/evidence.sh heartbeat` — one pulse per beat, not bursts |
| A11 | Foreground service survives screen-off | UNTESTED | Lock the phone 5 minutes. Is the link still up? |
| A12 | Link survives a phone call | UNTESTED | Take a call. Does the cluster keep working after? |
| A13 | Bluetooth switched off mid-ride | UNTESTED | Toggle Bluetooth. Does the app recover, or wedge? |

## B. Navigation

| # | Feature | Status | How to settle it |
|---|---|---|---|
| B1 | All 18 maneuver codes draw correctly | **PROVEN** | Photographed 19 Sep, each screen self-labelled |
| B2 | Live Maps turns reach the cluster | **PROVEN** | Codes 1 and 4 fired with real distance countdown |
| B3 | Distance counts down in real time | **PROVEN** | 240m to 230m to 210m to 200m observed |
| B4 | Code 36 direction | **PARTIAL** | Photographed but direction never confirmed by eye. Hold 34 then 36 — do they curl opposite ways? |
| B5 | Display clears when navigation ends | UNTESTED | `tools/evidence.sh navend`. **Fix installed but never run** |
| B6 | Stale data times out if Maps freezes | UNTESTED | **Not yet written.** Maps can freeze with its notification posted |
| B7 | Arrival / destination-reached behaviour | UNTESTED | Complete a route. What does the cluster show? |
| B8 | Re-route mid-journey | UNTESTED | Deliberately miss a turn. Does the arrow follow the new route? |
| B9 | Navigation screen in-app | UNTESTED | Does it mirror what the cluster shows? |
| B10 | Saved places / favourites | UNTESTED | Save one, reopen the app, is it there? |
| B11 | Distance unit setting (km/miles) | UNTESTED | Switch to miles. Does the cluster follow? |

## C. Notifications

| # | Feature | Status | How to settle it |
|---|---|---|---|
| C1 | Missed-call lamp | UNTESTED | `tools/evidence.sh call`. **Filter rewritten 19 Sep, never seen a real call** |
| C2 | WhatsApp call lights the lamp | UNTESTED | Same capture, WhatsApp call |
| C3 | Message lamp — WhatsApp | UNTESTED | `tools/evidence.sh message` |
| C4 | Message lamp — SMS | UNTESTED | Same |
| C5 | Untrusted app is rejected | UNTESTED | The security fix. Any other app's notification must NOT reach the cluster |
| C6 | Ongoing notifications ignored | UNTESTED | Play music. The lamp must not pin on |
| C7 | In-app notification list | UNTESTED | Do entries appear, and does the unread badge clear? |
| C8 | Notification access revoked mid-use | UNTESTED | Revoke it. Does the app tell you, or fail silently? |

## D. Safety and SOS

| # | Feature | Status | How to settle it |
|---|---|---|---|
| D1 | Add emergency contact by typing | UNTESTED | Add one. Does it persist across restart? |
| D2 | Add contact from the picker | UNTESTED | Pick from address book. Is the number sanitised? |
| D3 | Primary contact assignment | UNTESTED | First contact becomes primary automatically |
| D4 | Deleting the primary reassigns it | UNTESTED | Delete it. Does another take over? |
| D5 | Contact limit enforced | UNTESTED | Add past the cap. Does it refuse clearly? |
| D6 | SOS dials the contact | UNTESTED | Tap call. Dialer should open pre-filled, **not** dial by itself |
| D7 | Location share builds a correct link | UNTESTED | Tap share. Open the link — does it point at where you are? |
| D8 | Location denied fails safely | UNTESTED | Deny permission. Must say "unavailable", never invent coordinates |
| D9 | Approximate-location warning | UNTESTED | With coarse location only, is the amber warning shown? |
| D10 | Emergency number (112) dial | UNTESTED | Dialer opens, nothing dialled |

## E. Service, fuel and reminders

| # | Feature | Status | How to settle it |
|---|---|---|---|
| E1 | Service record saves | UNTESTED | Add one, restart, still there? |
| E2 | Odometer floor rejects a too-low record | UNTESTED | Enter a reading below the last. Clear refusal expected |
| E3 | Odometer plausibility guard | UNTESTED on device | Unit-tested (7 cases). Needs the 6,001,923 km case observed |
| E4 | Service-due calculation | UNTESTED | Does "next service in X km" match the interval? |
| E5 | Default tasks seeded on first run | UNTESTED | Fresh install — are the six checks present? |
| E6 | Editing a task interval | UNTESTED | Change one, does the forecast follow? |
| E7 | Refuel entry and mileage | UNTESTED | Add two refuels. Is the figure sane? |
| E8 | Reminder fires | UNTESTED | Needs a service interval to actually elapse |

## F. Rides and statistics

| # | Feature | Status | How to settle it |
|---|---|---|---|
| F1 | A ride is recorded | UNTESTED | Ride, then check the list |
| F2 | Distance / duration / speed are right | UNTESTED | Compare against the scooter's own trip meter |
| F3 | Statistics filters (today/week/month/year) | UNTESTED | Do the totals change correctly? |
| F4 | Chart buckets | UNTESTED | Does the chart match the underlying rides? |
| F5 | Rides sync to cloud | UNTESTED | Sign in, ride, check Firestore |

## G. Account and profile

| # | Feature | Status | How to settle it |
|---|---|---|---|
| G1 | Google sign-in | UNTESTED | Completes and lands on the right screen? |
| G2 | Email sign-up and sign-in | UNTESTED | Both directions |
| G3 | Guest mode | UNTESTED | Usable without an account? |
| G4 | Guest to account migration keeps the profile | UNTESTED | The dialog promises this. **Changed 19 Sep** |
| G5 | Plain sign-out clears the profile | UNTESTED | The security fix. Profile must NOT carry to the next account |
| G6 | **Account data separation** | UNTESTED | Contact under account A, sign out, sign in as B. B must NOT see it |
| G7 | Profile photo from gallery | UNTESTED | Pick, crop, save |
| G8 | Profile photo from Google account | UNTESTED | **Known bug** — fails silently for images under 1080 px |
| G9 | Profile restored from cloud on new sign-in | UNTESTED | Sign in elsewhere — does the profile come back? |
| G10 | Vehicle and colour selection | UNTESTED | Persists and shows on the dashboard? |
| G11 | Delete local data | UNTESTED | Wipes what the dialog promises, nothing more |
| G12 | Delete account | **NOT WIRED** | No screen calls it. Fix before building one |

## H. Settings, appearance, onboarding, legal

| # | Feature | Status | How to settle it |
|---|---|---|---|
| H1 | Light / dark / system theme | UNTESTED | All three, including a restart |
| H2 | Language switch | UNTESTED | Does it apply without a restart? |
| H3 | Auto-connect toggle | UNTESTED | Off means no silent reconnect |
| H4 | Onboarding runs once | UNTESTED | Not shown again after completion |
| H5 | Permission flow | UNTESTED | Each grant and each denial handled |
| H6 | Background-run access prompt | UNTESTED | OEM battery settings reachable |
| H7 | Terms and privacy acceptance | UNTESTED | Recorded and not re-asked |
| H8 | About screen version | UNTESTED | Matches the build |

## I. Security and release readiness

| # | Item | Status | How to settle it |
|---|---|---|---|
| I1 | 15 audit fixes | UNTESTED on device | Source-level only. None observed running |
| I2 | Release build (R8) runs | UNTESTED | **Blocked — needs a release keystore** |
| I3 | Release build has no debug logs | UNTESTED | Blocked on I2 |
| I4 | App Check enforcement | NOT ENABLED | Console setting. Code reports, console blocks |
| I5 | Published Firestore rules match the repo | UNCHECKED | **Only you can check this** — Firebase console |
| I6 | Database migration on a real upgrade | **PARTIAL** | Did not crash on 19 Sep; columns never verified |
| I7 | Play data-safety declaration | NOT DONE | Must declare the advertising-ID permissions |
| I8 | Privacy policy hosted at a public URL | NOT DONE | Written, not hosted. Play requires a live link |

---

## Suggested order

**Session 1 — the link and the lamps** (engine running, ~20 min)
A3, A10, C1–C4, B4, B5. Settles whether the 19 Sep changes broke anything.

**Session 2 — riding** (a real ride)
A4, A11, A12, B7, B8, F1, F2, E4.

**Session 3 — sitting at a desk** (no scooter)
D1–D5, D7–D9, E1–E3, E5–E7, G1–G11, H1–H8.

**Session 4 — release**
Create the keystore, then I2, I3, I4, I5, I7, I8.

**Before publishing publicly, these are non-negotiable:** I5 (published rules), G6 (account separation), I2 and I3 (the release build actually runs), I8 (privacy policy hosted).
