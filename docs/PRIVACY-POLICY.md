# Privacy Policy — RideConnectX

**Last updated:** 21 September 2026
**Contact:** gnyaneshwar76@gmail.com

RideConnectX ("the app") connects your phone to your scooter's instrument
cluster over Bluetooth Low Energy, so the cluster can show turn-by-turn
directions, a notification lamp and your name. This policy explains what the app
collects, where it goes, and what stays on your phone.

> This policy was written from an audit of what the code actually does. It is
> not legal advice.

---

## 1. The short version

- Most of what the app knows **never leaves your phone.**
- Nothing is sold, and nothing is shared with advertisers or data brokers.
- Notification content is read on-device and relayed to your scooter. It is
  **never** uploaded.
- Your location is used only when you ask for it, and is never stored.
- You can **delete your account and its data from inside the app** at any time.

---

## 2. What is stored on your phone only

None of this is uploaded. Each item is tied to the account that created it and
is not shown to any other account signed in on the same phone:

| Data | Why |
|---|---|
| Emergency contacts (name and number you add) | So SOS can offer someone to call — works without internet |
| Ride history, service records, refuelling records | The ride and service screens |
| Notification history | The in-app notification list |
| Paired vehicle address and name | To reconnect to your scooter |
| App settings and appearance | Your preferences |

Backup is disabled, so none of this is copied into Google cloud backup or moved
in a device-to-device transfer.

**Emergency contacts are other people's details.** Only add people who have
agreed to be your emergency contact.

## 3. What is stored in your account

If you sign in, the following is saved to Firebase (Google Cloud) under your
account, and security rules make it readable only by you:

- Your name, email address and sign-in method
- Rider name, nickname and city
- Selected vehicle model and colour
- Profile photo, if you set one, stored at a reduced size
- Your scooter's last odometer, trip and fuel readings, saved when you sign
  out so they are there when you sign back in
- Saved places and, where you enable it, ride records

You can use the app as a **guest**, in which case nothing is uploaded at all.

## 4. Permissions, and exactly what each is for

| Permission | What the app does with it |
|---|---|
| **Bluetooth / nearby devices** | Find and connect to your scooter's cluster. Used for nothing else. |
| **Notification access** | Read the notification that Google Maps posts while navigating, so the next turn can be drawn on your cluster; and detect that a call or message arrived so the cluster's lamp lights. **Content is processed on the phone and sent only to your scooter.** It is never uploaded and never stored. |
| **Location** | Sharing your position when *you* tap share on the SOS screen, and filling in your city on your profile. On Android 11 and older, Android also requires it for Bluetooth scanning. The app does not track or log where you go. |
| **Contacts** | Only when you tap to pick an emergency contact. The app keeps that one contact's name and number. |
| **Photos** | Only when you choose a profile picture. |
| **Phone state** | Only to read your mobile signal strength, which is shown as bars on the cluster. The app does not read your calls, call log or phone number. |
| **Notifications (post)** | Show the ongoing notification Android requires while the app holds the Bluetooth link. |

The app deliberately does **not** request permission to read SMS, read your call
log, or place calls. Calls open your dialer with the number filled in; you press
the call button.

## 5. Analytics

The app includes Google Firebase Analytics, which collects usage and device
information and may use an advertising identifier. This is used to understand
crashes and general usage. It is not used to build an advertising profile, and
no data is sold. Google's own handling is described at
<https://firebase.google.com/support/privacy>.

## 6. What is never collected

- The content of your messages or calls, beyond momentarily relaying a sender
  name to the cluster
- A history of where you have travelled
- Your address book as a whole
- Payment information — the app takes no payments

## 7. Your choices

- **Use it as a guest** — nothing is uploaded.
- **Sign out** — your last readings are saved to your account, and your profile
  and readings are removed from the phone.
- **Delete local data** — wipes rides, notifications, service records, emergency
  contacts, the paired vehicle and cached readings from the phone.
- **Delete your account** — in the app: **Profile → Delete account**. After you
  confirm it is you, your cloud data (profile, photo, readings, saved places,
  rides), your account, and that account's data on the phone are permanently
  deleted. Without the app, request deletion at
  <https://rideconnectx-89528dd3.web.app/delete-account.html>.
- **Revoke any permission** at any time in Android Settings. The app degrades
  rather than breaking: without location, SOS tells you it cannot share a
  position instead of inventing one.

## 8. Your rights

Under India's Digital Personal Data Protection Act, 2023, and similar laws
elsewhere, you can ask to access, correct or erase your personal data, and to
withdraw consent. Most of this you can do yourself in the app; for anything
else, email the contact at the top. Requests are answered within 30 days. If
you are not satisfied, you may complain to the Data Protection Board of India.

## 9. Children

The app is not directed at children under 13 and is not intended for them. It is
a companion to a motor vehicle.

## 10. Data retention

Local data stays until you delete it, sign out, or uninstall. Account data stays
until you delete your account, and is then removed immediately.

## 11. Changes

If this policy changes materially, the app will say so before the change takes
effect. The date at the top always reflects the current version.

---

## Notice

RideConnectX is an independent application. It is not produced, endorsed,
certified or supported by Suzuki Motor Corporation or Google LLC. Trademarks
belong to their respective owners.

**Safety:** what the app draws on your cluster is an aid, not a substitute for
your attention to the road or to the vehicle's own instruments. Do not interact
with your phone while riding.
