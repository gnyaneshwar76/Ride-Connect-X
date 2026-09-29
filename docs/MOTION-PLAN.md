# RideConnectX — Motion plan

Branch: `motion/app-motion`, built on `backup/app-before-motion-2026-09-29`.
If anything here feels wrong on the phone, that backup branch is the untouched app.

Rules every motion follows:

- **It explains something.** Where a screen came from, that a value changed, that a tap landed, that input was refused. No motion only for decoration.
- **One vocabulary.** All timings and helpers live in `presentation/theme/Motion.kt`: `enterRise` (stagger in), `pressScale`, `shake`, `rememberBreath` (slow glow), `animatedCount` (count-up), `rememberShakeKey`.
- **Plays once per visit.** Entrances do not replay on scroll or recomposition.
- **Honours "Remove animations".** Compose scales every animation by the phone's animator setting, so it drops to instant.

## Phase 1: built on this branch (126/126 tests)

| Screen | Motion | Why |
|---|---|---|
| **All screens** | New screen slides in from the right; the one behind drifts back and dims. Back reverses it. Leaving the splash only fades. | You always know where you came from. |
| **Dashboard** | Sections rise in one after another. | The screen builds instead of popping. |
| | Scooter rolls onto its stage the first time. | The vehicle is the hero. |
| | Floor light breathes; swells and warms when connected. | Connected is felt, not just read. |
| | Light sweeps across the scooter while scanning or connecting. | Shows work in progress. |
| | Connected pill fades green and a ring pings from its dot. | Live link at a glance. |
| | ODO / Trip A / Trip B count up to the new reading. | A fresh reading is noticed. |
| | Fuel bars fill left to right, one after another. | Like the gauge sweeping on ignition. |
| | Pair tile morphs to Connected (colour + live dot pops in). | State change is smooth. |
| | Quick-action tiles press in and spring back. | Taps feel physical. (existing, kept) |
| | Overdue service card slowly breathes its red border. | Noticed without shouting. |
| **Navigate** | Route line draws itself from you to the destination. | Sets the scene. |
| | Your position dot pulses. | "You are here". |
| | Maneuver card drops in; each new turn slides up into it. | The next action is what moves. |
| | Relay dot fades between states and pings while sending to the cluster. | Shows the cluster link is live. |
| **Pairing** | Found scooters rise into the list as they are found. | No sudden jumps. |
| | Connect → spinner → tick, each swapping in with a pop. | Clear progress. |
| **Stats** | Sections rise in; period tab colour glides. | |
| | Distance bars grow one after another whenever the period changes. | Numbers feel earned. |
| | Ride rows rise in; added/removed rows animate. | |
| **Service** | Hero, tasks, status and history rise in; added/deleted records slide/fold. | |
| **Safety** | Sections rise in; contacts animate in/out. SOS pulse (existing, kept). | |
| **Notifications** | Rows rise in one after another; read/removed rows animate. | |
| **Profile** | Cards rise in; scooter hovers slowly and glows when connected. | On display, alive. |
| | Photo viewer: picture zooms up from the avatar size. | Not a jump cut. |
| **Settings** | Groups rise in. Switches animate (Material, existing). | |
| **Forms** | Refused input (N18 name rules, contact fields, profile names) shakes once. | Tells you "no" without a popup. |
| | Primary buttons press in and spring back. | |
| **Intro / onboarding** | Page dots stretch and glide between pages. | The pill travels. |

## Phase 2: proposed, not built yet

| Screen | Idea |
|---|---|
| Splash | Wordmark light sweep (as in the web mock). |
| Intro pager | Parallax: illustration moves slower than the text while swiping. |
| Permissions | Granted card pops a tick and its border turns green. |
| Vehicle gallery | Selected model scales up, others dim; colour swatch morphs the paint. |
| Dashboard | Unread badge on the bell, bell rings once when a call/message reaches the cluster. |
| Navigate | Arrival sheet with a drawn tick when Maps finishes the route. |
| Service | "Saved" tick on the add/edit sheet button before it closes. |
| Safety | SOS: hold-to-send ring that fills, instead of a single tap. |
| Empty states | Illustrations float gently. |
| Appearance | Crossfade the whole app when switching Glass/Flat or light/dark. |
| Haptics | Short vibration on connect, refused input, SOS. |

Owner review: try Phase 1 on the phone, mark each row keep / too much / too slow, then pick Phase 2 rows.
