# RideConnectX

**Turn-by-turn navigation on your Suzuki Access 125's own instrument cluster.**

Google Maps tells your phone where to turn. RideConnectX tells your *scooter* — the arrow, the distance and the ETA appear on the cluster itself, so you keep your eyes on the road instead of a handlebar mount.

Built for the Suzuki Access 125 and its Bluetooth cluster, by reverse-engineering the packet format the official app uses.

---

## What it does

- **Turn arrows on the cluster** — left, right, slight, sharp, forks and every roundabout exit, drawn from live Google Maps navigation
- **Distance to the next turn**, counting down in real time, plus ETA and remaining distance
- **Notification and missed-call lamps** — know something arrived without reaching for the phone
- **Live telemetry** — odometer, trip A/B and fuel read straight off the vehicle
- **Service reminders** based on real distance ridden, which keep working when the scooter is out of range
- **Safety screen** — emergency contacts and one-tap location sharing

## Built with

Kotlin · Jetpack Compose · Hilt · Room · DataStore · Firebase Auth & Firestore · Bluetooth Low Energy

Clean architecture, `minSdk 28`, `targetSdk 35`.

---

## The interesting part: the protocol

The cluster speaks a 30-byte framed protocol over BLE. None of it is documented, so every field here was recovered by capturing packets from the official app and checking them against what the dashboard actually drew.

```
a5 37 | 000002427 | 006189 | 000308 | 01 | 35 | 00 00 00 | 70 | 7f
 │  │       │          │        │      │    │       │       │    └ end byte
 │  │       │          │        │      │    │       │       └ checksum
 │  │       │          │        │      │    │       └ padding
 │  │       │          │        │      │    └ fuel bars, ASCII '0'–'5'
 │  │       │          │        │      └ unknown, always 0x01
 │  │       │          │        └ trip B, tenths of a km
 │  │       │          └ trip A, tenths of a km
 │  │       └ odometer, 9 ASCII digits, whole km
 │  └ packet type (0x37 = telemetry)
 └ start byte
```

**The checksum** is the one's complement of the low byte of the sum of bytes 1–27. That rule was solved against captured packets and later confirmed against a live frame from the vehicle: sum `0x8F` → complement `0x70`, and byte 28 was `0x70`.

**Maneuver codes** were established one at a time on real hardware — a single code held on the cluster while the rider described the arrow drawn, with the distance field deliberately set to the code number so every screen labelled itself. Codes 20–26 turn out to rotate a roundabout exit arrow cleanly from sharp-left through straight to sharp-right. The full table lives in [`tools/cluster/cluster-codes.csv`](tools/cluster/cluster-codes.csv).

---

## Privacy, in one paragraph

Notification text is read on the phone and relayed only to your scooter — it is never uploaded. Location is used when *you* tap share, and is not stored or tracked. Contacts are read one at a time, only when you pick one. The app deliberately does **not** request permission to read SMS, read your call log, or place calls. Cloud backup and device transfer are switched off. Full detail: [`docs/PRIVACY-POLICY.md`](docs/PRIVACY-POLICY.md).

## Security

The app has been through two source-level security audits. Fifteen issues were found and fixed, covering BLE peer trust, notification spoofing, cross-account data on a shared phone, and release-build logging. Some items remain open and are tracked in the commit history rather than hidden.

No secret, key or vehicle identifier has ever been committed to this repository.

---

## Building it

```bash
git clone https://github.com/gnyaneshwar76/Ride-Connect-X.git
cd Ride-Connect-X/frontend
./gradlew assembleDebug
```

Open the `frontend/` folder (not the repository root) in Android Studio.

You will need your own `frontend/app/google-services.json` from a Firebase project — it is deliberately not committed. Release builds additionally need a `keystore.properties` in `frontend/`; without it the build fails loudly rather than quietly producing an unsigned APK.

---

## Repository layout

| Folder | What's in it |
|---|---|
| [`frontend/`](frontend) | The Android app — Kotlin, Compose, Gradle build |
| [`backend/`](backend) | Firebase: Firestore rules, hosting config, and the public site (`public/`) |
| [`tools/`](tools) | Cluster capture and sweep scripts (`cluster/`), GPX test routes (`gpx/`), image prep (`images/`) |
| [`docs/`](docs) | `protocol/` · `testing/` · `design/` · `status/` · `archive/` |
| [`brand/`](brand) | Logos and store artwork |

Test evidence — photos and recordings of each feature working on the scooter — lives on the separate [`proofs`](../../tree/proofs) branch.

Deploy the backend from `backend/`: `firebase deploy --only firestore:rules,hosting`.

---

## Status

Turn arrows are confirmed on hardware and the navigation relay works end to end. Active development continues on cloud sync and account scoping.

## Licence

Proprietary — see [LICENSE](LICENSE). The source is published for reference and review, not for reuse.

Suzuki and Access are trademarks of Suzuki Motor Corporation. Google, Google Maps and Firebase are trademarks of Google LLC. This is an independent project and is not endorsed by, affiliated with or supported by either company.

**Safety:** what this app draws on your cluster is an aid, not a substitute for your own attention to the road. Do not interact with your phone while riding.
