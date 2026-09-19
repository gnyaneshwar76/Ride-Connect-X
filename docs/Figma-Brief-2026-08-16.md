# RideConnectX — design brief for Figma

**Date:** 16 August 2026
**Deadline:** the app ships 30 August 2026.

Paste this whole document into Figma Make (or hand it to whoever is designing).
It is written to be actionable without asking me anything back.

---

## 0. Read this first — what already exists

RideConnectX is a **built, working Android app**, not a concept. 23 screens are
live in Kotlin/Compose and running on a real phone right now. This brief asks
for a **visual upgrade to specific screens**, not a redesign.

**That means the output has to be portable.** Beautiful frames I cannot
translate into Compose are worth nothing. Every deliverable below asks for
**exact numeric values published as Figma variables**, because those get typed
straight into Kotlin.

**The last export changed nothing.** The zip delivered on 16 August was byte-for-byte
identical to the 13 August one — same 70 files, same SHA-256 on every one of
them, only the archive timestamp differed. Please make actual new frames.

---

## 1. The design tokens you must match

These are live in the app. **Do not invent new ones** — match these exactly, or
the new work will not sit with the 23 screens that already exist.

### Colours — dark theme (the primary theme)

| Token | Hex | Used for |
|---|---|---|
| `bg` | `#070D1B` | screen background |
| `card` | `#0D1526` | card surfaces |
| `card2` | `#111D35` | nested surfaces, chips |
| `text` | `#E4ECFF` | primary text |
| `muted` | `#6B7FA0` | secondary text, labels |
| `blue` | `#2B7FFF` | **primary accent** |
| `cyan` | `#00D4FF` | live/data highlights |
| `green` | `#00D9A3` | connected, success |
| `amber` | `#FFB547` | warnings, fuel |
| `red` | `#FF5A6A` | danger, SOS, reserve |
| `border` | `rgba(43,127,255,0.15)` | card outlines |
| `glow` | `rgba(43,127,255,0.30)` | accent glow |
| `outerBg` | `#030810` | behind full-bleed content |

### Colours — light theme

| Token | Hex |
|---|---|
| `bg` | `#EEF2FF` |
| `card` | `#FFFFFF` |
| `card2` | `#F4F7FF` |
| `text` | `#0A1628` |
| `muted` | `#5B6E88` |
| `blue` | `#2B7FFF` |
| `cyan` | `#0095BB` |
| `green` | `#009E74` |
| `amber` | `#C87D00` |
| `red` | `#CC2238` |
| `border` | `rgba(43,127,255,0.16)` |
| `outerBg` | `#C8D8F4` |

**Every design must be delivered in both themes.** The app has a working
light/dark switch and riders use both.

### Accent colour is user-selectable

The rider can change the accent to any of these eight. Anything you design must
still work when the accent is Violet or Lime, not just blue.

`Blue #2B7FFF` · `Cyan #00C2E0` · `Mint #00D9A3` · `Violet #9B6BFF` ·
`Magenta #FF5FA8` · `Amber #FFB547` · `Coral #FF7A5A` · `Lime #9BE564`
(those are the dark-theme values; each has a darker light-theme twin.)

### Type

Three families, already specified: **Outfit** (headings/wordmark), **DM Sans**
(body), **JetBrains Mono** (numbers, labels, telemetry).

⚠️ **These were specified but never delivered as files.** The app is currently
falling back to the system default, which is exactly why the rider says the text
looks "boxy". **Please supply the actual font files or confirm the Google Fonts
weights to bundle.** This alone would visibly improve every screen.

### Motion (already ported, keep using these)

- `SPRING_SNAPPY` — stiffness 380, damping 32
- `SPRING_GENTLE` — stiffness 220, damping 26
- `SPRING_SHEET` — stiffness 260, damping 30
- `EASE_OUT_EXPO` — cubic-bezier(0.16, 1, 0.3, 1)
- Press feedback: scale to **0.96**

---

## 2. THE BIG ONE — "Liquid Glass" control style

This is the top priority and it is blocking work. The rider's words:

> "make it like integrated buttons and wait i told you do glass buttons like
> apple na where it is i cant see it do that"

> "some users not like that one glass give an option to normal and glass in
> apprence"

### What to deliver

**A. A frosted-surface token set**, as Figma variables with exact values:

- background blur radius (px)
- fill colour + opacity, dark theme and light theme
- border colour + opacity + width
- inner top highlight (the thin bright line along the top edge that sells glass)
- drop shadow: colour, opacity, blur, y-offset
- corner radius

**B. That style applied to five controls**, each in **default / pressed /
disabled**, in **both themes**:

1. Segmented tab control, 4 segments (this is the Statistics period picker:
   Today / This Week / This Month / This Year)
2. Stat tile (a label, a big number, a unit)
3. Large primary button, full width
4. Small icon tile (the Dashboard quick actions — icon above a short label)
5. Bottom sheet header

**C. The legibility proof.** Put all five controls **over a full-bleed dark
photograph** and show the minimum scrim needed to keep text readable. This is
the thing that usually goes wrong — glass over a photo with a bright patch
becomes unreadable. Give me the scrim as a value, not a vibe.

**D. The "normal" variant.** Every one of the five controls also drawn in the
current flat style, so the Appearance toggle has two real states to switch
between. The two variants must be **the same size and position** — switching
style must not reflow the layout.

### Reference

Apple's Control Center and iOS 18 widgets. Frosted, not merely transparent:
you should see the background *blurred* through it, with a bright hairline on
the top edge and a soft shadow underneath.

⚠️ **Android note:** real-time background blur only exists on Android 12+. On
older phones it falls back to a flat translucent fill. **Please give me both**:
the true blurred version and the fallback fill colour that looks closest to it.

---

## 3. Statistics screen (screen 16)

The rider's words:

> "now statics what is that only showing pic upside i should cover whole screen
> like not if we do that the buttons will go confusion like something will off
> so sync or do glass buttons"

### What it looks like now

A 132dp photograph band at the top, then flat cards below it. It reads as a
picture stuck on top of a list.

### What is wanted

- The photograph **full-bleed, behind the whole screen** — not a band.
- The controls **floating on top of it as glass** (section 2), so the image is
  the screen rather than a header.
- It must stay readable. That is what the scrim in 2C is for.

### Content that must survive the redesign

Do not drop any of this — it is real data the app computes:

- Period tabs: Today / This Week / This Month / This Year
- Summary tiles: **Distance** (km), **Time**, **Trips**
- Second row: **Avg speed** (km/h), **Longest** ride (km)
- "Ride History" list, and its empty state ("No rides yet")
- Back header with the screen title

The hero image already exists: a dark misty mountain road at dawn, 16:9.
Design against that.

---

## 4. Sign In screen (screen 06)

> "the sign in page is looking boring no animation in background anything our
> app should not look boring"

Currently: a logo, two lines of text, three buttons, all on a flat dark
gradient. Functional and lifeless.

**Wanted:** a living background. Suggestions, pick one and specify it fully:

- slow-drifting conic/mesh gradient in the brand blues and cyan
- faint animated route line tracing across the screen and fading
- soft particle drift, like city lights out of focus

**Constraints:**
- It must loop seamlessly and **never distract from the three buttons.**
- Specify it as **animatable values** — gradient stops, positions, durations,
  easing — not as a video file. It has to run in Compose at 60fps on a
  mid-range phone.
- Budget: this runs on the sign-in screen only, so a little cost is acceptable,
  but no more than ~2ms per frame.

**Do not change** the button order (Google, Email, Guest) or the legal footer.

---

## 5. Permissions screen (screen 10)

> "in permission page same looks like boring do some magic like make it looks
> like not boring when i or any one see it wow"

Currently: a photo band with the title on it, then three permission cards, then
a Continue button.

**Wanted:** make granting permissions feel like progress rather than a form.
Ideas to design out:

- A progress indicator that fills as each permission is granted (3 of 3)
- Each card animating to a "granted" state — a satisfying check, colour shift,
  the card settling
- The Continue button changing character once everything is on

**Constraint that cannot be designed around:** the actual permission dialogs
are **Android system UI**. We cannot restyle them. Design only what surrounds
them.

---

## 6. Welcome and onboarding (screens 02–05)

> "make the pic big like fit to the screens and make the buttons glass ... i see
> that the pic is boxy and buttons and text it is looking like old boxy app it
> should look like apple app like modern sliky"

**Already done in code** — do not redo, just design *on top of* this:
- The four pages are now one swipeable pager
- Skip sits beside the primary button and disappears on the last page
- The artwork is edge-to-edge

**Still wanted from you:**
- The buttons in the glass style from section 2
- Page-transition motion: how the image, title and dots move as the rider
  swipes (parallax? cross-fade? specify values)
- The dot indicator restyled to match

---

## 7. Dashboard (screen 13)

**Already fixed in code, do not redesign:**
- Vehicle is a full-width hero standing in a drawn pool of light in the rider's
  chosen paint colour
- Fuel is a five-segment E–F bar (**five is fixed — the scooter's hardware
  transmits exactly one digit 0–5, so it cannot be any other number**)
- Settings moved out of the header into Quick Actions
- The greeting shows a short nickname beside the rider's photo

**What would help:**
- Quick action tiles in the glass style
- A treatment for the telemetry tiles (ODO / Trip A / Trip B / Fuel) so they
  read as one instrument panel rather than four separate cards
- Connected vs Offline: right now it is a small pill. Make the connected state
  feel alive.

---

## 8. What to deliver

1. **Figma frames** for every screen above, **dark and light**, at 1080×2400.
2. **Published Figma variables** for every new token — colours with opacity,
   radii, blur radii, shadows, durations, easings. This is the part that gets
   typed into Kotlin, so it matters more than the frames.
3. **A component sheet**: every state of every control from section 2B.
4. **A motion spec**: for each animation, the property, from/to values,
   duration in ms, and easing curve.
5. **The font files** (or exact Google Fonts weights) for Outfit, DM Sans and
   JetBrains Mono.

**Not needed:** icons. The app uses Material Symbols, which is already bundled,
vector, and Apache 2.0 licensed. Do not draw icons.

---

## 9. Things that cannot change, and why

Please do not design around these — they will be rejected:

| Thing | Why |
|---|---|
| Fuel bar has exactly **5 segments** | The cluster transmits one ASCII digit `0`–`5` at byte 24. More segments would be inventing precision the vehicle does not send. |
| **No speed** anywhere | The scooter does not transmit speed. A speed readout would be permanently blank. |
| System permission dialogs | Android owns them. Cannot be restyled. |
| Vehicle photographs | Real photographs, already generated and processed. A photo cannot be recoloured convincingly, so the *stage light* takes the paint colour instead. |
| Material Symbols icons | Already bundled and licensed. |

---

## 10. Priority order

If time is short, do them in this order:

1. **The glass style (section 2)** — everything else depends on it
2. **The fonts** — cheapest, most visible improvement across all 23 screens
3. **Statistics (section 3)**
4. **Sign In background (section 4)**
5. **Permissions (section 5)**
6. Onboarding and Dashboard polish (sections 6–7)
