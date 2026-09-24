# RideConnectX — Proofs

Evidence that each feature works on the real scooter: photos of the cluster,
screen recordings, and log excerpts. The code lives on
[`main`](../../tree/main); this branch only holds proof and is never merged.

## How files are named

`<test-id>-<short-name>.<ext>` inside the folder for its section, for example
`B-navigation/B3-roundabout-exit-2.jpg`. Test IDs match
[`docs/testing/Test-Matrix.md`](../../blob/main/docs/testing/Test-Matrix.md).

| Folder | Section |
|---|---|
| [`A-vehicle-link/`](A-vehicle-link) | Pairing, reconnect, rider name, battery and signal on the cluster |
| [`B-navigation/`](B-navigation) | Turn arrows, distance, ETA, re-routing |
| [`C-notifications/`](C-notifications) | Call and message lamps |
| [`D-safety/`](D-safety) | SOS, emergency contacts, location sharing |
| [`E-service/`](E-service) | Service records and reminders |
| [`F-rides/`](F-rides) | Ride recording and stats |
| [`G-account/`](G-account) | Sign-in, profile, account separation |
| [`H-settings/`](H-settings) | Theme, onboarding, permissions |
| [`I-security/`](I-security) | Release build and security checks |

## Large videos

GitHub refuses files over 100 MB. Longer recordings are attached to a
[GitHub Release](../../releases) and linked from the section they prove.
