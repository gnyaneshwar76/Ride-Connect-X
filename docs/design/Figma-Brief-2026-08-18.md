# Figma Brief — 18 August 2026

**For: the Figma design agent. From: RideConnectX engineering.**
Ship date is **30 August 2026**, so please keep this to token and spec changes
rather than restructuring screens.

Reviewed against your export **“Design system and onboarding flow updated 4.zip”**
(18 August, `src/app/App.tsx`, 2,265 lines). Two things in it are excellent and
must not be undone. Three things are the reason the app does not yet look
premium on a real phone.

---

## 0. What is already right — please do not change these

**§4 `AnimatedSignInBg` is exactly what was wanted.** Three drifting colour orbs
(7s / 9s / 11s, blur 42–52px), 22 rising particles, 48 stars with a twinkle
subset, the 8s diagonal sweep, and the top-edge specular hairline. It is fully
specified with real numbers, which is what makes it portable to Compose.
**Keep it. Do not simplify it, and do not replace it with a static image.**

> Note for the rider: this animation has **never existed in the Android app** —
> it is not something that was removed. It arrived in this export and still has
> to be built. See §4 below.

**The motion constants are good and already match what is built:**
`SPRING_SNAPPY` 380/32, `SPRING_GENTLE` 220/26, `SPRING_SHEET` 260/30.

---

## 1. ⚠ The glass tokens are the problem — they are unchanged from export 3

This is the headline. The rider asked for Apple-style glass. The glass in
export 4 is **byte-identical to export 3**:

```
GDK.blur      = 20
GDK.fill      = rgba(7,13,27,0.58)   ← 58% opaque near-black
GDK.highlight = rgba(255,255,255,0.14)
GDK.specular  = rgba(255,255,255,0.065)
```

**A 58%-opaque dark fill is not glass — it is a dark scrim with a blur behind
it.** On a photograph it reads as a dim grey box. Apple's Liquid Glass does the
opposite: a *light*, low-opacity layer over a *heavy* blur, so colour from the
backdrop bleeds through and the panel looks lit rather than shaded. That is
what makes it read as glass.

Compounding it, our photo screens also apply `SCRIM_GRAD` (up to 66% black)
*under* the panels, so the backdrop is darkened twice before the 58% fill
lands on top. Result: a nearly black panel.

### ⚠ SUPERSEDED — read the animation brief first

The rider has since supplied two reference videos. Their numbers **change the
conclusion below**, so treat §4a of `Figma-Brief-Animation-2026-08-18.md` as
authoritative over this section. In short:

- **Keep `blur` at 20.** Both references use 20–30px. The request to raise it
  to 36 is withdrawn.
- **The single `fill` token is the real problem, not its value.** The
  references use ~70% where a panel must stay legible over arbitrary content
  and ~30% where it is decorative over a controlled background. Our one 58%
  token is doing both jobs badly.
- **Please split it into `glassHeavy` (~58–70%) and `glassLight` (~30%)**, and
  fix the double-darkening (scrim under glass) rather than lightening the fill
  everywhere.

The table below is kept only as a record of what was originally asked for.

### Requested new values

**The app currently ships your exact values and will keep doing so until you
change them.** Engineering tried brightening these by hand and the rider
correctly rejected that: tokens come from Figma and are never eyeballed here.
So the numbers below are a *request*, not something already applied — please
either adopt them or give us better ones, and we will re-port whatever you
send.

| Token | Was | Now (please ratify) | Why |
|---|---|---|---|
| `blur` | 20 | **36** | Apple's is heavy; 20 barely separates panel from backdrop |
| `GDK.fill` | `rgba(7,13,27,0.58)` | **`rgba(11,20,36,0.24)`** | the single biggest cause of “dim” |
| `GDK.fillPressed` | 0.76 | **0.36** | |
| `GDK.border` | `rgba(255,255,255,0.11)` | **0.30** | the lit rim is most of the glass illusion |
| `GDK.highlight` | 0.14 | **0.55** | |
| `GDK.specular` | 0.065 | **0.18** | |
| `GLT.fill` | `rgba(255,255,255,0.22)` | **0.30** | |
| `GLT.highlight` | 0.50 | **0.70** | |
| `SCRIM_GRAD` | 0.28 → 0.66 | **0.20 → 0.54** | stop double-darkening the photo |

**The contrast question is yours to answer, and it is the real constraint.**
§2C fixes the scrim at 4.5:1 WCAG AA for white 13sp/500 text. Lightening the
fill and the scrim reduces contrast for any text sitting *on* the panel. Please
either:

- **(a)** re-check 4.5:1 with the lighter values and adjust type colour/weight
  to compensate (preferred — Apple keeps contrast by making text heavier and
  pure white, not by darkening the panel); or
- **(b)** give us a two-tier token: a light `glassDecor` for panels carrying
  large text or icons, and the current heavier `glassText` for dense body copy.

Option (b) is probably the honest answer, and it also solves §2.

---

## 2. Glass on *some* controls, not all — please mark which

The rider's words: *“every button glass like in apple… but I see in some places
only, not all screens.”* Correct instinct — glass everywhere is noise, and it
destroys contrast on dense screens.

Please annotate the component sheet with a **`glass: yes/no`** flag per control.
Our proposed split, for you to correct:

**Glass (sits over a photograph or a coloured field):**
Dashboard quick-action tiles · Statistics summary tiles and period tabs ·
Navigation overlay cards · bottom-sheet headers · the Pair “nothing found” card

**Flat (dense text, forms, lists — glass hurts legibility):**
Settings rows · Service records · Safety contact rows and the SOS sheet body ·
all text fields · Legal and About

**Never glass:** the SOS button itself. It has to be unmistakable.

---

## 3. “Make it look colourful and professional” — the actual ask

The rider's words: *“we should decorate our app like a professional app, like
colourful.”* Today the app is navy + one blue accent, and it reads flat and
utilitarian next to the photography.

This is a **decoration pass, not a restructure.** Please do not move or rename
anything — screens are built and the deadline is 30 August.

What we would like:

1. **A real accent palette with roles, not one blue.** We already ship 8 accent
   choices, but everything else is the same navy. Give each *data type* a
   consistent hue used across every screen — distance, time, trips, speed, fuel,
   service, safety. They are currently picked per screen by hand and disagree
   with each other.
2. **Gradient treatments for the “hero” numbers.** The big values (ODO, mileage,
   statistics totals) are flat white. A subtle two-stop gradient per data type
   would carry most of the “premium” feeling at zero layout cost.
3. **Coloured icon tiles with matched glow.** The tiles already tint at ~9%
   alpha. Specify a per-type tint + a soft outer glow so they read as lit
   objects rather than flat squares.
4. **Depth ladder.** Give us 3 elevation levels with exact shadow values
   (background → card → raised/sheet). Everything currently sits on one plane,
   which is a large part of why it looks amateur.
5. **Empty states.** Several screens are honest but bleak (“No rides yet”, “No
   service records yet”). A small piece of per-screen colour art would lift
   them enormously — these are the screens a new rider sees *first*.

Please deliver as **explicit token values**, the way `GDK`/`GLT` were done.
That format ported directly into Compose with no guesswork and is the reason
the glass system was buildable at all.

---

## 4. What engineering will build from your export

Not a request — recording it so we do not duplicate work:

- **`AnimatedSignInBg` → Compose.** Orbs as blurred radial `Brush` on a
  `Canvas` driven by `rememberInfiniteTransition`; particles and stars likewise.
  All transform/opacity only, as you specified.
- The ratified glass tokens, once you answer §1.

---

## 5. Please ignore

`guidelines/Guidelines.md` in the export is still the untouched Figma
boilerplate (2.5 KB, generic). It has been in every export so far. Either fill
it with the real design rules or delete it — right now it misleads anyone who
opens the zip first.

The `src/app/components/ui/*` tree is stock shadcn boilerplate and is not read
by us; only `App.tsx` matters. No need to keep shipping it if it is easy to
exclude.

---

## 6. NEW REQUEST (18 Aug, after export 5) — more photography, and colour on Statistics

The rider, in their words: *"we should add more pics I think for our app —
where is Statistics colour with pics."*

Two things are being asked for:

**(a) More photography across the app.** We currently have 23 generated images
and 14 in-app slots wired. The rider wants more. Please say **which specific
slots** should gain a photograph — a list of screen + placement, as you did for
the original set. Engineering will generate them to your spec (the pipeline
exists: `tools/images/prepare_scenes.py`, 111 MB of PNG → 707 KB of WebP, and
the Gemini watermark is a fixed 96px crop off two edges, so new images are
cheap to process).

Constraints worth repeating: vehicles must be **unbranded and generic**, and
the whole `drawable-nodpi` folder is currently 1.16 MB, so there is real room.

**(b) Statistics needs colour.** This is the sharper half of the request. The
screen is a dark mountain photograph with a heavy scrim and dark glass tiles
over it — it reads as monochrome grey even though the values themselves are
tinted. Now that §3 defines `DC.distance / time / trips / speed`, please
specify how those hues actually reach the eye on this screen:

- Does each summary tile get a coloured rim, an inner glow, or a tinted
  `fillAccent` keyed to its data type?
- Should the distance chart bars use the per-type hue, and with a gradient?
- Does the selected period tab carry the accent, and how strongly?
- Should the photograph behind it be a warmer or more colourful one? A dusk
  mountain is beautiful but almost greyscale — a more saturated image would
  do more for "colourful" than any token change.

That last point may be the real answer, and it is cheap: it is one image swap,
not a redesign.
