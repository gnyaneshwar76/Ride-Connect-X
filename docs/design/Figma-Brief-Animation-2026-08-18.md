# Figma Brief — Animation & Motion, 18 August 2026

**For: the Figma design agent. From: RideConnectX engineering.**
Companion to `Figma-Brief-2026-08-18.md` (glass, colour, decoration).
Ship date **30 August 2026** — this is a motion-spec pass, not a restructure.

The rider's ask, in their words: *"animations too… like Apple style or any
professional app… very premium."* This brief is the research behind that, so
you are not starting from a blank page.

**Deliver every item as explicit numbers**, the way `GDK`/`GLT` and
`SPRING_SNAPPY` were done. That format ported into Compose with zero guesswork
and is the only reason the glass and the aurora were buildable. A description
like "smooth fade" is not portable; `260ms, easeOutExpo, 12dp rise` is.

---

## 0. What already exists — build on it, do not replace it

| Token | Value | Status |
|---|---|---|
| `SPRING_SNAPPY` | stiffness 380, damping 32 | in code |
| `SPRING_GENTLE` | stiffness 220, damping 26 | in code |
| `SPRING_SHEET` | stiffness 260, damping 30 | in code |
| `EASE_OUT_EXPO` | cubic-bezier(0.16, 1, 0.3, 1) | in code |
| Press feedback | scale 0.96 | in code |
| `AnimatedSignInBg` | orbs 7/9/11s, 22 particles, 48 stars, 8s sweep | **built 18 Aug, ported exactly** |

The sign-in aurora is done and matches your spec. Everything below is what is
still missing.

---

## 1. The research — what actually makes an app feel premium

Summarised from Apple HIG, Material 3 Expressive and current motion-design
practice (sources at the end). Four findings that should shape the spec:

**1. Duration is tightly bounded, and most apps get it wrong by being slow.**
The usable range is **100–500ms**. Under ~100ms reads as an instant jump with
no cause-and-effect; over ~500ms feels sluggish and starts costing the user
time. Practical split:

| Interaction | Duration |
|---|---|
| Micro-interaction (button press, toggle, checkbox) | **100–200ms** |
| Card / sheet / in-page reveal | **200–300ms** |
| Full screen transition, many elements | **300–500ms** |

Apple's own system animations sit at **250–400ms**.

**2. Physics has replaced duration at the high end.** Material 3 Expressive
explicitly moved from duration-based curves to a spring engine — stiffness plus
damping ratio — because it survives interruption. If a user taps again
mid-animation, a spring retargets from its current velocity while a fixed
duration curve has to restart or snap. **This is the single biggest difference
between "has animations" and "feels premium."** Our three spring tokens are
already the right shape; they are just barely used.

**3. Springs should not be everywhere.** Apple's guidance is that spring, and
especially bouncy spring, belongs to **success and celebration moments**, not
routine transitions. A damping ratio below ~1 on every card makes an app feel
cheap and restless. Default to critically damped; spend the bounce deliberately.

**4. Restraint is a feature.** HIG is explicit: avoid adding motion to
interactions that happen constantly, and keep frequent animations quick and
precise. The premium feeling comes from a few well-chosen moments, not from
everything moving.

---

## 2. What to specify — the gaps, in priority order

### A. Screen transitions ⭐ highest impact

Right now every screen change is an instant cut. This is the most visible thing
separating us from a premium app, and it affects all 23 screens at once.

Please specify, with numbers:

- **Forward (push):** incoming screen offset + fade; outgoing screen behaviour
  (parallax back and dim, or hold?). Apple moves the outgoing screen ~30% of
  the width at reduced opacity, which is what creates the sense of a stack.
- **Back (pop):** must be the exact inverse, or the app feels unstable.
- **Modal / bottom sheet:** we have `SPRING_SHEET` — confirm scrim fade
  duration and whether the sheet overshoots.
- **Tab / segment change** (Statistics period tabs): direction-aware slide?

### B. Staggered list entrance

Cards animating in together read as one block; staggered reads as considered.
Research puts the stagger step at **~50ms between siblings, capped at ~100ms**,
with the whole sequence finishing inside ~400ms so a long list never feels slow.

Specify: per-item delay, travel distance (a 12–16dp rise is typical), whether it
replays on scroll-back (**it should not** — replaying on every scroll is a
classic amateur tell), and the cap after which remaining items simply appear.

Screens that need it: Dashboard cards, Statistics tiles, Service records, Safety
contacts, Notifications.

### C. Skeleton / shimmer loading states

We currently show either a spinner or nothing. Shimmer skeletons are the
established premium pattern (LinkedIn, Facebook feeds) because they preserve
layout and remove the perceived wait.

Specify: base and highlight colour for both themes, the sweep angle, the sweep
duration (**~1000–1500ms** is the norm), and the shape/size of the skeleton for
each card type we have.

Where: Dashboard telemetry cards before the first BLE packet, Statistics before
Room returns, Service history.

### D. Value transitions on live data

The dashboard's ODO, trip and mileage numbers **snap** from one value to
another. Rolling/counting digits is a small change with a large payoff, and it
suits a vehicle cluster particularly well.

Specify: whether digits roll or cross-fade, duration (**300–600ms** for a
counter), easing, and the threshold below which a change should not animate at
all (a 0.1 km tick should not spin the whole number).

Also: the five-segment fuel bar currently jumps between levels — specify the
fill transition.

### E. State-change moments — where the bounce is allowed

Per finding 3, these are the places a spring with real bounce is *earned*:

- BLE connect succeeding (the connection pill going Offline → Connected)
- A service record saved
- An emergency contact added
- Arrival / route complete

Specify one **`SPRING_CELEBRATE`** token (bouncier than the existing three) plus
what actually moves — scale pop, colour flush, icon transform.

### F. Continuous / ambient motion

The aurora proved this works. Two more places worth it:

- **Connection pill** — a slow breathing glow while connected
- **SOS button** — already pulses; confirm the rate is right (it must read as
  urgent without being distracting on a long ride)

---

## 3. Hard constraints — please respect these

**Transform and opacity only.** Anything animating layout (width, height,
padding, position-in-flow) causes a re-layout every frame and will drop frames
on the rider's phone. Your aurora spec already follows this rule; please keep
it for everything.

**Budget: 60fps on a mid-range Android.** Test device is a OnePlus Nord CE 3 5G.
Not a flagship — an effect that needs a flagship is the wrong effect.

**Reduce Motion must be honoured.** Both Apple and Android require that when
the system accessibility setting is on, animation is minimised or removed. Tell
us, per animation, what it degrades to — usually a plain cross-fade, or nothing.
This is an App Store review criterion, not a nicety.

**Interruption behaviour.** For every transition, say what happens if the user
taps again mid-flight. Springs retarget; duration curves need an explicit answer.

**Do not animate on every recomposition.** Ambient loops must be driven by one
infinite transition, as the aurora is — not restarted per frame.

---

## 4. The rider's own reference video — analysed

The rider sent a YouTube reference. Engineering could not open it (YouTube
blocks automated access), so **the rider had Gemini analyse it and returned the
spec below.** This is their taste stated directly and **outranks anything else
in this brief.**

**The video is iOS Dynamic Island / Live Activities.**

| Element | Property | Duration | Curve | Trigger |
|---|---|---|---|---|
| Island expansion | scale (w/h) | 300ms | **spring, overshoot** | tap / data update |
| Content fade-in | opacity 0→100% | 200ms | ease-in-out | interaction |
| Icon move | position Y + scale | 250ms | **spring, bounce** | event |
| Live-activity list | position Y + opacity | 350ms | spring, damped | screen entry |
| Live waveform | height + opacity | 500ms **loop** | ease-in-out | data stream |

- **Transitions are shared-element morphs**, not pushes. A small container
  scales and transforms into the destination screen.
- **Stagger: 60ms between items, 20px Y offset.**
- **No skeletons** — a stable container with a 1500ms circular spinner instead.
- **Glass: blur 20px with a ~70% dark overlay.**

Their top three premium moments: the elastic overshoot on expansion; the
shared-element morph from small container to full screen; and live data
animating continuously rather than snapping.

### ⚠ What this changes — please read before acting on the glass brief

**The reference glass is blur 20px at ~70% dark. Your `GDK` token is blur 20px
at 58% dark — very close, and arguably already correct.** That means the
"too dim" complaint is probably **not** the token at all, but the fact that our
photo screens apply `SCRIM_GRAD` (up to 66% black) *underneath* the panels, so
the backdrop is darkened twice.

So please treat §1 of the companion brief as **"fix the double-darkening"**
rather than "lighten the glass". Reducing the scrim under glass panels, and
leaving `GDK.fill` near its current value, is now the more likely correct fix.
Engineering has no opinion to press here — this is yours to decide — but the
evidence changed and you should have it.

### How this maps onto our screens

| Their pattern | Our equivalent |
|---|---|
| Island → full screen morph | Dashboard vehicle card → detail; quick-action tile → its screen |
| Live-activity stagger (60ms / 20px) | Dashboard cards, Statistics tiles, Service list |
| Elastic overshoot on expand | bottom sheets, the connection pill going Connected |
| Live waveform | live telemetry — ODO/trip/fuel updating from BLE |
| Stable container + spinner | our BLE connecting state |

**Please use 60ms / 20px as the stagger spec** unless you have a reason to
differ — it is what the rider pointed at, and it sits inside the researched
safe range in §2B.

Compose supports shared-element morphs natively via `sharedElement()` /
`sharedBounds()`, so if you specify these we can build them.

---

## 4a. Second reference — Apple-style UI animation tutorial (After Effects)

The rider sent a second video, also analysed via Gemini. It is a **design
tutorial on recreating Apple's UI motion and aesthetic**, so it is closer to
"how to author this" than to a shipping product — but the numbers are useful
and it independently confirms the stagger range.

| Element | Property | Duration | Motion | Trigger |
|---|---|---|---|---|
| Heading / body text reveal | position Y | ~400ms | eased, no bounce | screen entry |
| **Word-by-word reveal** | opacity + Y offset **100px** | ~600ms total | eased ramp | screen entry |
| Glass panel | blur + opacity | static | — | screen entry |

- **Stagger: 50–100ms per item, travelling 50–100px.** Independently matches
  both our research range and the 60ms/20px from the Dynamic Island video.
- **Transitions: fades and shape-layer morphs** — again shared-element, not cuts.
- **Whitespace is called out explicitly** as what makes it read premium:
  minimal, uncrammed layouts. Worth remembering during the colour pass in the
  companion brief — decoration must not mean density.

Their standout: the **word-by-word text reveal**, described as feeling like
Apple's editorial marketing because the staggering is rhythmic and deliberate.
A candidate for our Welcome and Onboarding headings — please say whether you
want it, and where. It should be used **once or twice in the whole app**; on
every screen it would become a tic.

### ⚠ The two videos disagree about glass opacity — and that is the answer

| Source | Blur | Overlay opacity |
|---|---|---|
| Video 1 — Dynamic Island (dark pill, over anything) | 20px | **~70%** |
| Video 2 — glass panel (over a designed background) | 20–30px | **~30%** |
| **Our current `GDK`** | **20px** | **58%** |

They are not contradicting each other — **they are two different jobs.** A
container that must stay legible over arbitrary content is heavy (~70%); a
decorative panel over a controlled background is light (~30%). Our single 58%
token is being asked to do both, which is why it looks too heavy on the
photo screens and unremarkable elsewhere.

**This is strong evidence for the two-tier token** proposed as option (b) in
§1 of the companion brief:

- **`glassHeavy` ≈ 58–70%** — panels carrying dense text over a photograph or
  live camera-like content (Statistics cards, sheet bodies).
- **`glassLight` ≈ 30%** — decorative panels, quick-action tiles, chips, and
  anything over a controlled background, paired with a lighter scrim beneath.

Our blur radius of 20px is confirmed correct by **both** videos, so the earlier
request to raise it to 36 should be **withdrawn** — please keep blur at 20.

---

## 4b. Other reference points

- **iOS Control Centre** — the glass reference the rider sent earlier.
- **Apple Music Now Playing** — mini-player → full-player expansion.
- **Apple Wallet card expansion** — directly applicable to our vehicle hero card.

---
## 5. What engineering will do with this

Port it exactly, as with the aurora. We will not invent motion values or
"improve" them — the rider has been explicit that design decisions are yours.
If something is unspecified, we will ask rather than guess.

---

## Sources

- [Motion — Apple Human Interface Guidelines](https://developers.apple.com/design/human-interface-guidelines/foundations/motion)
- [Reduced Motion evaluation criteria — Apple Developer](https://developer.apple.com/help/app-store-connect/manage-app-accessibility/reduced-motion-evaluation-criteria/)
- [Motion — Material Design 3](https://m3.material.io/styles/motion/)
- [Adding Motion Physics with Jetpack Compose — Material 3 blog](https://m3.material.io/blog/m3-expressive-motion-theming)
- [How Long Should App Animations Be? The 200ms Rule](https://www.appypie.com/blog/mobile-app-animation-guide)
- [Skeleton Shimmer example — Motion.dev](https://motion.dev/examples/react-skeleton-shimmer)
- [Jetpack Compose Shared Element Transitions](https://medium.com/@me.zahidul/jetpack-compose-shared-element-transitions-create-smooth-android-animations-0088ca05c987)
- [Material 3 Expressive: New Components, Motion, Shapes](https://supercharge.design/blog/material-3-expressive)
