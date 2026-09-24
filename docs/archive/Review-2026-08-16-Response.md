# Response to the rider's review — 16 August 2026

The rider walked the whole app on their phone and reported twelve areas. This
records what each one turned out to be, what was changed, and what is still
open. Screens were re-walked on a running emulator after the changes; 70/70
unit tests pass.

---

## The two privacy reports — what was actually happening

The rider reported, twice and separately, that the app opened their contacts and
their gallery while the permission screen showed both as *not granted*. They
were right to flag it, and the explanation is worse than a bug in one screen.

**What the code did:** the contact picker uses `ACTION_PICK` and the photo
picker uses Android's Photo Picker. Both are system UIs. The rider taps one
item, and the app is handed only that item. Neither needs a permission, and
that is the privacy-preserving way to do it — the app never sees the rest.

**What made it look like a violation:** `READ_CONTACTS`, `READ_MEDIA_IMAGES` and
`READ_EXTERNAL_STORAGE` were **declared in the manifest and never used by any
code**. So the permission screen listed Contacts and Photos as things the app
wanted, showed them ungranted, and then a button visibly opened the contact
list. From outside there is no way to tell that apart from a permission being
bypassed.

**Fixed three ways:**

1. **All three permissions removed from the manifest.** Verified on the device —
   `dumpsys package` no longer lists them. The app now *cannot* read the contact
   book or the gallery, even if a future change tried to. Same reasoning already
   applied to `READ_SMS` and `READ_CALL_LOG`.
2. **A consent sheet before either picker opens** (`PickerConsentSheet`). It
   names what is about to open, states that only the item tapped comes back, and
   carries a line saying the app holds no permission to read the rest. Android
   does not require this. It is there because a system picker appearing
   unannounced is indistinguishable from an app helping itself.
3. **The permission screen now explains it** — a "Handled by Android's pickers"
   section with both capabilities and a `NO PERMISSION HELD` badge, so their
   absence from the permission list is answered rather than left to be noticed.

---

## Bugs found and fixed

### Reported by the rider

| # | Report | Cause | Fix |
|---|---|---|---|
| 1 | Back twice on Welcome → blank page, only Recents recovers | Navigation Compose finishes the Activity only when the *start* destination is popped. The start destination is the splash, which every route pops off itself — so backing out of the last screen emptied the back stack and left the NavHost with nothing to draw. | A `BackHandler` at the host: pop if there is something behind, otherwise finish the Activity. Fixes it everywhere, not screen by screen. |
| 9 | Dashboard greets "Eshwar P" after typing "P Gnyaneshwar" | `DashboardViewModel` read `session.name` — the Google account name — instead of the rider name. Every other screen already read the right one. | Greeting reads the typed name first, account name only as fallback. |
| 9 | "Change vehicle" reopens Create Profile and re-asks for Terms | It navigated to the Create Profile route. | New `ProfileFormMode.CHANGE_VEHICLE`: same form with name, city, photo and consent left out, a "Change Vehicle" header and a "Save vehicle" button. |
| 4/9/11 | Profile picture missing on Account Found, Dashboard and Settings; lost after clearing data | The decoder was a **private helper inside the Profile screen**, so only that screen could draw it. And the file was local-only — never part of the account. | One shared `RiderAvatar`, used by all four. The picture is now written into the Firestore user document as a 256px Base64 JPEG and restored with the rest of the profile. |
| 12 | "Run in background" drops you on App info, and never ticks | The row was hardcoded `granted = false` and opened App info. | Reads the real battery-optimisation exemption, and fires Android's own one-tap dialog. Ticks on return. |
| 3 | Bluetooth and location still had to be switched on by hand | Only a link to the Settings app. | Play Services' location-settings resolution now shows Google's own "Turn on location" dialog *over* the app, chained after the permission dialogs. Verified with location switched off: dialog appeared in-app, tap turned it on, screen went to "Continue". Added `play-services-location`. |
| 10 | "Battery Check" on a petrol scooter | Seeded task list. | Replaced with the petrol-scooter schedule: Engine Oil, Air Filter, Brake Inspection, Tyre Pressure & Tread, Spark Plug, Drive Belt. |

### Found while testing, not in the review

| Report | Detail |
|---|---|
| **Back from the Dashboard went to the Permissions setup screen** | Create Profile → Dashboard popped only itself, leaving the finished setup screens underneath. Now the whole setup run is cleared. Verified: back from Dashboard exits to the launcher. |
| **Back from Permissions walked into the onboarding tour** | Sign-in popped only the sign-in screen, leaving Welcome and the three onboarding pages beneath it. Now the whole intro is cleared on authentication. |
| **A guest typed their name, then Create Profile asked again with an empty field** | The guest name went into the session but never into the rider name the rest of the app reads. Now stored once, and Create Profile arrives pre-filled. |
| **The Service screen had a "Service reminders" switch that Settings also had** | Two controls, one setting. Removed from Service; Settings keeps it, with the other notification switches. |

---

## Changed as asked

- **Swipe.** Welcome and the three onboarding pages are now one `HorizontalPager`
  (`IntroScreen`). Four destinations became one, which is also where the
  blank-screen bug was living.
- **Skip** moved beside the primary button and disappears on the last page.
- **The intro artwork is edge to edge** and takes all the height left over,
  instead of sitting inside side padding on a fixed-height box — that inset box
  is what made the pages read as boxy.
- **The vehicle is the hero** on Dashboard and Profile: full card width, ~190dp
  tall, name underneath. `Connected Vehicle` is gone — it took the larger half
  of the row to say nothing the rider did not know.
- **The rider's picture sits beside the greeting** on the Dashboard.
- **Fuel is a five-segment E–F bar** instead of "— / 5", with the last segment
  red and labelled RESERVE. **Five is not a design choice**: the cluster
  transmits a single ASCII digit `'0'`–`'5'` at byte 24 of the telemetry frame,
  so five bars is the resolution the vehicle actually reports. Drawing more
  would be inventing precision.
- **Colour now visibly changes the vehicle.** The stage light behind it takes
  the chosen paint. Per-colour photographs are supported too — see
  `Vehicle-Colourway-Prompts.md`; drop in a correctly named file and it is used.
- **Service order** is Upcoming Tasks → Next Service → Maintenance History, and
  the empty history card opens the add sheet when tapped.

---

## Still open, and why

**Language (item 11) — now working, and partly translated.** See the
"Localization" section below.

**The "make it less boring" items (2, 3, 5, 7).** Animated sign-in background,
glass buttons with a normal/glass switch in Appearance, and the Statistics
full-bleed treatment are design work, not defects. They need a decision on the
glass style before it is built in six places, and that is worth doing once
rather than guessing. Not started.

**Service-centre directory (item 10).** The rider wants a picker of real service
centres for a chosen area. **This is a data problem, not a UI one** — accurate
Suzuki service-centre names and addresses per city are not something that can be
written from memory without inventing them, and a wrong address in a service
record is worse than a blank field. Two honest routes: a Places search intent
that hands the rider Google's own results to pick from, or a bundled list the
rider supplies. Needs the rider's call.

**Explaining permissions during onboarding (item 12).** The permission screen
explains each one; the onboarding pages do not yet mention them. This is copy on
existing screens, not new machinery.

**The call button on Safety** was not tested — the rider said they would try it
in the morning.

---

## Second review, same day — the four follow-ups

### 1. Permission dialogs, reverted to Android's own

The rider's decision, overriding the earlier consent-sheet approach:

> "what i told when user hit that profile pic selection and contacts selection
> the default android selection same like Bluetooth permission then it should
> open photos and contracts like allow all or limited"

`READ_CONTACTS`, `READ_MEDIA_IMAGES` and — the part that matters —
**`READ_MEDIA_VISUAL_USER_SELECTED`** are declared again. That third one is what
turns the Android 14+ photo prompt into the three-way **Allow all / Select
photos / Don't allow**; asking for `READ_MEDIA_IMAGES` alone gets an
all-or-nothing prompt, which is the one thing the rider did not want.

The in-app consent sheet is deleted. Tapping the avatar or "Choose from
contacts" now fires the system dialog, then opens the picker **whatever the
answer was** — the picker needs no permission, so refusing narrows what the app
can see rather than dead-ending the rider.

### 2. The spotlight was measured, not guessed

> "the bike is out of frame like it should beam on to the bike in middle"

Correct, and it could never have lined up. Measured: the bright pool in
`img_dashboard_hero_plate` sits at **64% of the image height**; a centred
vehicle sits at **50%**. Cropping the photo would only move the problem to the
next card size.

The light is **drawn now** (`VehicleStage`). One constant, `GROUND = 0.80f`,
places both the tyre contact patch and the centre of the ellipse, so they cannot
drift apart on any screen, at any card width, for any vehicle.

### 3. Header and nickname

- **Settings gear removed from the header.** Two 34dp icon buttons plus the
  connection pill left it squeezed against the screen edge. It is a fifth Quick
  Action tile now, with a real touch target.
- **Nickname**, stored separately from the full name. Required, capped at 14
  characters, and **never auto-filled** — the account name is precisely the
  thing that does not fit the header, so deriving a short form from it lands
  back where it started.
- **The Samsung-style glow.** The Continue button stays live once the rest of
  the form is valid; tapping it with an empty nickname flashes the field blue
  and settles over 900ms rather than doing nothing. A dead button gives the
  rider nothing to follow.

### 4. Account migration

> "edit the email like Gmail like transfer the account to new mail ... guest to
> new mail or google account"

**The bug underneath it:** `clearSession()` already keeps the rider name,
nickname, city, vehicle, paint and photo — only the account identity is
cleared. But `persist()` only ever *restored from* the cloud, and did nothing
when the account was new. So a guest who signed up, or anyone moving to a
different email, kept their profile locally while the new account stayed empty —
and lost the lot on the next reinstall.

`carryLocalProfileToCloud()` now runs on the new-account branch, taking the
whole local profile up to the new uid. It writes nothing when there is nothing
to carry, so a genuinely new rider does not get a document full of blank fields.

Profile gains **"Move to another account"** (or "Save to an account" for a
guest), with a sheet stating plainly what travels and what stays on the phone.

The nickname now travels with the profile in both directions.

### Also fixed here

- **The profile picture is editable from the Profile screen.** It could only be
  set during Create Profile, so the one screen actually called "Profile" could
  show the picture and not change it — and anyone who skipped it at setup had no
  route back to it at all. Tapping the avatar or its camera badge opens
  Change/Remove, with the same cropper, syncing to the account on change.

### Verified vs built

**Verified on the emulator:** the drawn spotlight, the header change, the
nickname field, the mandatory glow, and the nickname showing on the dashboard.

**Built and compiling but not yet seen on a device:** the standard permission
dialogs at the point of use, profile photo editing, and account migration. The
emulator became unstable and hung on `screencap` twice; these want a pass on the
rider's phone.

---

## Localization — the mechanism is done, the translation is half done

**The switch works.** Choosing తెలుగు or हिन्दी now changes the app, verified on
the emulator in both languages: the intro, the permission flow and the whole
dashboard come up in the chosen script, with no missing glyphs and no layout
breaking on the longer words.

**How it is wired:**

- `androidx.appcompat` added purely for `AppCompatDelegate.setApplicationLocales`,
  which is the only API that covers both sides of API 33 — the platform's own
  `LocaleManager` is 33+, and this app is `minSdk 28`.
- `res/xml/locales_config.xml` declares en-IN, te and hi, which is also what
  puts RideConnectX in Android's own per-app language picker in system Settings.
- `AppLocale.apply()` is called from `MainActivity` beside the theme, because a
  language change recreates the Activity — driving it from the screen that set
  it would kill the thing doing the work. "Phone language" applies an empty
  locale list, which hands the decision back to Android rather than pinning
  English.
- **A missing translation falls back to English, not to a crash.** That is what
  makes a screen-at-a-time translation safe.

**Translated so far** — Welcome and onboarding, Sign In, Guest account,
Permissions (including the permission catalogue itself), Dashboard, Settings,
Appearance, Notifications, About. Roughly 150 strings across three languages.

**Not yet translated** — Create Profile, Profile, Service, Safety, Statistics,
BLE Pairing, Navigation, Permission details, Account Found, Vehicle Gallery,
Legal. These still show English inside a Telugu or Hindi app, which is why the
work is worth finishing rather than leaving here.

**One change worth noting:** `AppPermission` used to hold its label, description
and explanation as plain `String`s. That object is built at class-load time,
long before there is a Context to resolve a locale against, so the permission
cards would have stayed English no matter what. They are `@StringRes Int`s now.

**Deliberately left in English**, because it is what riders say and what is
printed on the vehicle: Bluetooth, GPS, BLE, SOS, ODO, Trip A / Trip B, and
every model name (Access 125, Burgman Street, Gixxer, V-Strom).

**The translations need the rider's eye.** They are mine, not a native
speaker's. Telugu especially — the rider reads it, and a wrong word in a safety
screen matters more than a clumsy one on a settings row.
