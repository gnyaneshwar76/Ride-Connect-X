# RideConnectX — brief for Figma

Paste this whole document into Figma (Make / AI) as the project brief. It is
written to be handed over cold: it says what the app is, what already exists,
what is fixed, and exactly what to produce.

---

## 1. What the app is

RideConnectX is an Android app for Suzuki scooters and motorcycles. It connects
to the bike's digital instrument cluster over Bluetooth LE and puts things on
that cluster: turn-by-turn directions from Google Maps, the rider's name, phone
battery, signal, the clock, and message and missed-call lamps. It also reads
telemetry back off the vehicle — odometer, Trip A, Trip B and a five-segment
fuel bar.

It is a personal project by a single developer, shipping by **30 August 2026**.
There is no budget. Everything must be free.

**23 screens, all built.** This brief is about raising the *quality* of what
exists, not designing new screens.

---

## 2. The design system — this is fixed, work inside it

Do not invent a new palette or a new spacing scale. These are already in the
code and every screen uses them.

### Colours

| Token | Dark | Light |
|---|---|---|
| `bg` | `#070D1B` | `#EEF2FF` |
| `card` | `#0D1526` | `#FFFFFF` |
| `card2` | `#111D35` | `#F4F7FF` |
| `text` | `#E4ECFF` | `#0A1628` |
| `muted` | `#6B7FA0` | `#5B6E88` |
| `blue` (brand) | `#2B7FFF` | `#2B7FFF` |
| `cyan` | `#00D4FF` | `#0095BB` |
| `green` | `#00D9A3` | `#009E74` |
| `amber` | `#FFB547` | `#C87D00` |
| `red` | `#FF5A6A` | `#CC2238` |
| `border` | `rgba(43,127,255,0.15)` | `rgba(43,127,255,0.16)` |

Blue is the brand and is identical in both themes. Both themes are live in the
app and switch instantly; **every frame you produce must work in both.**

### Shape and spacing

- Corner radii: 10 / 12 / 14 / 18 / 20 / 22 dp. Cards are 18–22, chips 10–12.
- Screen padding: 20 dp on the Dashboard, 24 dp on every settings-style screen.
- Section spacing: 16 dp. Rows inside a card: 12 dp.
- Cards: 1 dp border in `border`, filled `card`, on a `bg` page.

### Type — please fix this, it is the single biggest complaint

The app currently renders with the **system default font**, which the developer
describes as "boxy and odd". The design specifies three Google Fonts that were
never bundled:

| Role | Font | Weight | Size |
|---|---|---|---|
| Wordmark, headings | **Outfit** | 700 | 27 / 28 / 22 / 18 sp |
| Body, labels, buttons | **DM Sans** | 400 / 600 | 15 / 13 / 14 / 16 sp |
| Data readouts, tiny captions | **JetBrains Mono** | 500 | 11 / 9 sp (1.5 letter-spacing) |

**What to deliver:** confirm this trio still reads well at these sizes on a
phone, adjust the scale if it does not, and hand back a type sheet with final
sizes, weights and line-heights. All three are open-licence and free.

If you think a different pairing is better, say so with a reason — but keep
Outfit-or-similar geometric for headings and a monospace for data, because the
data readouts are meant to look like instrument-cluster values.

---

## 3. What we need most: motion

**This is the priority.** The developer's words: *"our app is lacking transition
and animations — I asked for an Apple-like app but this is base level. It looks
good but I need perfection."*

The app today has almost no motion. Screens cut. Cards appear. Nothing responds
to a finger. What is needed is the quiet, physical feel of a well-made iOS app —
not decorative animation.

### Specify these, as a motion sheet

For each item below give: duration in ms, easing curve (or spring damping and
stiffness), what moves, and what does not.

1. **Screen transitions.** Push and pop between screens. Currently a hard cut.
   Wanted: a shared, consistent push — probably a slide plus a slight fade, with
   the outgoing screen moving less than the incoming one.
2. **Press feedback.** Every card and button. A press should scale down slightly
   and spring back. *(Already implemented on the Dashboard quick actions at
   scale 0.96 / spring damping 0.55 — please confirm or correct those numbers
   and apply the same rule everywhere.)*
3. **List entry.** Cards in a scrolling list appearing with a small stagger the
   first time a screen opens. Say the per-item delay.
4. **Bottom sheets.** The app uses Material 3 modal bottom sheets throughout
   (add service record, add contact, confirmations). Specify the enter and exit.
5. **State changes.** A value going from "—" to a real reading when the vehicle
   connects; the connection pill going from Offline to Connected; a progress bar
   filling. These should ease, not jump.
6. **The SOS button** on the Safety screen already pulses (1.045 scale, 1200 ms,
   reverse). Confirm or correct.
7. **Theme switch.** Light↔dark currently swaps instantly. Should it cross-fade?
   If yes, how long?

Deliver this as a written motion spec plus, where it helps, a prototype. The
developer will implement it in Jetpack Compose, so **give numbers, not
adjectives**.

---

## 4. The Dashboard — the main job

This is the screen the rider sees most, and it needs the most work.

### 4a. A 3D, 360° vehicle — the centrepiece

Today the vehicle is a flat vector drawing. The developer wants a **3D model of
the selected bike that the rider can spin 360°**, as the hero of the Dashboard.

**Constraint: it must be free.** No paid assets, no paid tools, no subscription.

The practical, free way to do this — and what we would like you to design
around — is a **pre-rendered turntable**:

1. Model or source the vehicle in **Blender** (free, and the developer has it
   available).
2. Render **36 frames**, one every 10° of rotation, on a transparent
   background, at the Dashboard's display size.
3. Ship those 36 images. The app swaps frames as the rider drags horizontally.

This looks and feels like real 3D, costs nothing, needs no 3D engine on the
phone, and runs on any device. A true real-time 3D model (Filament, SceneView)
is possible but would need a `.glb` per vehicle, more work, and more battery for
very little visible gain at this size.

**What we need from you:** the *design* of that hero — the framing, the lighting,
the reflection or shadow the vehicle sits on, the drag affordance (how does the
rider know it spins?), the idle state, and how it behaves while it is being
dragged. Eight vehicles need it: Access 125, Burgman Street 125EX, Avenis 125,
Gixxer SF 250, Gixxer 250, Gixxer SF, Gixxer, V-Strom SX.

**Important:** the vehicles must be **generic and unbranded** — no Suzuki
badging, no model names on the bodywork. Copyright.

### 4b. Polish the telemetry tiles

Four tiles: **ODO METER**, **FUEL**, **TRIP A**, **TRIP B**, plus a new
**MILEAGE** card showing derived km/L and estimated range.

They are functional but plain. Wanted: make them feel like instruments. Ideas to
explore — a subtle gauge arc, the fuel tile drawing its five segments as the
cluster does, values counting up when they change, a faint inner glow in the
tile's accent colour. Keep them readable at a glance; this is a screen looked at
while wearing gloves.

Each tile has a fixed accent: ODO blue, FUEL amber, TRIP A cyan, TRIP B green,
MILEAGE green.

**Note:** speed is deliberately absent. The scooter does not transmit it. Do not
design a speed tile.

### 4c. Quick actions — already restructured, please refine

The developer's complaint was that four equal rows *"cover the dashboard and
take the attention in a bad way"*. This has been changed to:

- **Two large tiles side by side: Navigate and Pair.**
- **Four small square icon tiles below: Stats, Safety, Service, Profile.**

Please refine that layout — proportions, the icon treatment in the small tiles,
and the press animation. Then a **Service reminder card** sits below, showing
the next service and an OVERDUE / DUE SOON badge.

---

## 5. The Navigation screen

The background image needs replacing. The developer: *"the background pic should
change — instead something good looking or synced, or an animation."*

The screen shows a large maneuver arrow, the instruction, distance and ETA, over
a full-bleed background.

Options to explore, in order of preference:
1. An **animated gradient or subtle motion** in the brand colours, reacting to
   navigation state (calm while going straight, a hint of movement as a turn
   approaches).
2. A **very dark, heavily blurred aerial night road** photograph. A prompt for
   this already exists in `docs/design/Image-Prompts-Gemini.md` (#7).
3. A generative pattern — light trails, a subtle grid.

Whatever you choose, the constraint is absolute: **white text and a large arrow
must stay perfectly readable on it, at a glance, in sunlight.** This screen is
read while riding. Legibility beats beauty here, every time.

---

## 6. Everything else, in one line each

- **Onboarding (screens 2–5).** Four screens introducing navigation, Bluetooth
  pairing and ride intelligence. Photographic heroes are being generated; design
  how they sit with the text and the progress dots.
- **Permissions (screen 10).** The system dialogs now fire automatically on
  arrival. Design the screen *behind* them — it should explain each permission
  while the dialogs appear over it.
- **Pairing (screen 14).** Radar pulse while scanning; a "nothing found" state
  with a checklist and a Scan again button. Both exist — please polish.
- **Service, Safety, Settings, Profile, Appearance, About (18–23).** Built and
  working, in the settings-card style. A consistency pass would help.

---

## 7. What to deliver

1. **A type sheet** — final fonts, sizes, weights, line-heights.
2. **A motion spec** — every item in §3, with numbers.
3. **The Dashboard**, redesigned: 3D hero framing, telemetry tiles, quick
   actions, service card. Both themes.
4. **The Navigation background**, resolved.
5. **A component sheet** — buttons, cards, rows, sheets, chips, in both themes,
   so the rest of the app can be brought in line.

Everything must survive a **light and a dark** theme, and must be implementable
in **Jetpack Compose** — so please avoid effects that need a blur behind moving
content, or per-frame image filtering, and say which of your values are exact
and which are approximate.

---

## 8. Things you should know before you start

- **The app is real and working.** It talks to an actual scooter over BLE. Do
  not design anything that implies data the vehicle does not send — no speed, no
  fuel percentage, no engine temperature. The fuel bar has exactly five
  segments because that is what the cluster draws.
- **Honesty is a design rule here.** Where the app cannot know something it says
  so rather than showing a confident zero. Please keep that. The mileage card,
  for example, states how many readings its estimate is based on.
- **The design already exists** at <https://hazel-mummy-35461748.figma.site/>
  and must stay recognisable. This is a polish pass, not a redesign.
