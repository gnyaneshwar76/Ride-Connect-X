# RideConnectX — AI image prompts (Gemini)

Every image the app needs, one per section, ready to paste into Gemini.

**How to use each block**
1. Copy the **Prompt**. Paste into Gemini image generation.
2. Save the result under the exact **File name** given.
3. Drop it in `app/src/main/res/drawable-nodpi/`.
4. Tell me and I will wire it in — the code already has a named placeholder for
   each one (see `RcxImagePlaceholder` in `presentation/components/`).

---

## Read this before generating

**On copyright.** These prompts deliberately describe **unbranded, generic**
scooters and motorcycles. No Suzuki badge, no model name on the bodywork, no
logos anywhere. A prompt cannot make a protected vehicle design unprotected —
asking for "a Suzuki Access 125" produces an image of a design somebody owns,
whoever generated it. Asking for "a 125cc Indian commuter scooter with a flat
footboard and a round headlamp" produces something that reads as the right bike
and is yours. Every prompt below is written the second way.

**Every prompt already contains**, so you do not have to add it:
- `shot on a full-frame DSLR, 85mm lens, f/2.8` — the photographic look
- `4K, ultra sharp, high dynamic range` — the resolution
- `no text, no logos, no badges, no watermark` — keeps it clean and safe
- a lighting and colour direction that matches the app's dark navy + blue theme

**Aspect ratios matter.** Where a ratio is given, ask Gemini for it explicitly
or crop afterwards, or the image will be cut badly on the phone.

**If a result looks wrong**, the usual fix is to add one of:
`side profile, eye level, centred subject` · `no rider, no people` ·
`plain background, subject isolated`.

---

# 1. Welcome screen — hero

**File name:** `img_welcome_hero.jpg`
**Where:** Screen 02, Welcome — the large image above "RideConnectX".
**Aspect ratio:** 4:5 (portrait)

> A photorealistic wide shot of a modern 125cc commuter scooter parked on a wet
> city street at blue hour, three-quarter front view from a low angle. The
> scooter is unbranded with a smooth matte deep-blue body, a flat footboard, a
> single round LED headlamp and a small digital instrument cluster glowing
> faintly cyan. Wet asphalt reflects blue and cyan city lights. Deep navy sky,
> soft bokeh of distant streetlights and shop signs. Cinematic, moody, premium
> automotive advertising photography. Shot on a full-frame DSLR, 85mm lens,
> f/2.8, 4K, ultra sharp, high dynamic range. No text, no logos, no badges, no
> watermark, no people.

---

# 2. Onboarding 1 — Smart Navigation

**File name:** `img_onboarding_navigation.jpg`
**Where:** Screen 03 — the illustration above "Turn-by-turn on your cluster".
**Aspect ratio:** 1:1 (square)

> A photorealistic close-up of a motorcycle digital instrument cluster mounted
> on unbranded handlebars, screen glowing in the dark, displaying a large white
> left-turn arrow and a distance readout. Rider's gloved hands lightly visible
> at the edges of the frame. Background is a blurred night road with warm
> streetlight bokeh. Cool cyan and blue screen glow lighting the handlebars.
> Premium automotive product photography, shallow depth of field. Shot on a
> full-frame DSLR, 85mm lens, f/2.8, 4K, ultra sharp, high dynamic range. No
> readable text, no logos, no badges, no watermark.

---

# 3. Onboarding 2 — Bluetooth pairing

**File name:** `img_onboarding_bluetooth.jpg`
**Where:** Screen 04 — the illustration above the pairing explanation.
**Aspect ratio:** 1:1 (square)

> A photorealistic close-up of a smartphone held next to a motorcycle's digital
> instrument cluster, both screens glowing softly blue in low evening light,
> suggesting a wireless connection between them. The cluster is unbranded and
> modern with a dark glass face. Shallow depth of field, the phone slightly out
> of focus in the foreground. Cool blue and cyan rim lighting, dark navy
> background. Premium tech advertising photography. Shot on a full-frame DSLR,
> 85mm lens, f/2.8, 4K, ultra sharp, high dynamic range. No readable text, no
> logos, no badges, no watermark, no faces.

---

# 4. Onboarding 3 — Ride intelligence

**File name:** `img_onboarding_intelligence.jpg`
**Where:** Screen 05 — the illustration above the alerts/telemetry explanation.
**Aspect ratio:** 1:1 (square)

> A photorealistic over-the-shoulder view from behind a rider on a scooter at
> dusk, looking down at a glowing digital instrument cluster showing speed and
> fuel gauges. The rider wears a plain dark helmet and jacket with no branding.
> The road ahead fades into soft focus with warm and blue city light bokeh.
> Cinematic, calm, premium. Shot on a full-frame DSLR, 85mm lens, f/2.8, 4K,
> ultra sharp, high dynamic range. No readable text, no logos, no badges, no
> watermark, no visible face.

---

# 5. Permissions screen — hero

**File name:** `img_permissions_hero.jpg`
**Where:** Screen 10, Permissions — above "Before we begin".
**Aspect ratio:** 16:9 (landscape)

> A photorealistic minimal still-life of a smartphone lying face-up on a dark
> brushed-metal surface next to a motorcycle key and a pair of black riding
> gloves. The phone screen is off. Soft directional light from the upper left,
> deep navy and charcoal tones with a subtle cyan rim light along the phone
> edge. Clean, calm, premium product photography with generous negative space
> on the right. Shot on a full-frame DSLR, 85mm lens, f/2.8, 4K, ultra sharp,
> high dynamic range. No text, no logos, no badges, no watermark, no people.

---

# 6. Pairing tutorial — the cluster's PAIR button

**File name:** `img_pairing_cluster_select.jpg`
**Where:** Screen 14, BLE Pairing — the "how to put your cluster in pair mode"
help card. This is the one the original app explains with an arrow.
**Aspect ratio:** 4:3

> A photorealistic close-up of a motorcycle's digital instrument cluster and
> the small round mode button on the left switchgear cluster, shot from the
> rider's seat looking down. The instrument screen is dark with a faint blue
> glow. The button is clearly visible and in sharp focus. Unbranded switchgear,
> textured black plastic, soft daylight from above. Technical product
> photography, clean and instructional. Shot on a full-frame DSLR, 85mm lens,
> f/4, 4K, ultra sharp, high dynamic range. No text, no logos, no badges, no
> watermark.

---

# 7. Navigation screen — background

**File name:** `img_navigation_background.jpg`
**Where:** Screen 15, Navigation — the full-bleed background behind the
maneuver card.
**Aspect ratio:** 9:19.5 (tall portrait, full phone screen)

> A photorealistic aerial night view of a city road network from directly
> above, softly out of focus, with warm amber streetlights and faint blue-white
> car light trails forming curves across dark asphalt. Very dark overall,
> heavily blurred, almost abstract, so that white text placed on top stays
> readable. Deep navy and near-black tones with subtle cyan highlights.
> Cinematic long-exposure photography. Shot on a full-frame DSLR, 4K, high
> dynamic range. No text, no logos, no signage, no watermark.

*Note: keep this one genuinely dark and blurred. If the first result is too
busy, add `extremely blurred, low contrast, 80% dark` to the prompt.*

---

# 8. Vehicle gallery — the eight models

All eight use the **same framing and lighting** so the gallery looks like one
set. Only the vehicle description changes.

**Shared framing, already inside every prompt below:** exact side profile, eye
level, vehicle centred, plain seamless dark charcoal studio background, soft
large overhead softbox with a cyan rim light from behind left, no shadows on
the backdrop.
**Aspect ratio for all eight:** 16:9

### 8a. Access 125

**File name:** `img_vehicle_access_125.jpg`

> A photorealistic studio photograph of an unbranded 125cc step-through
> commuter scooter in exact side profile at eye level, centred in frame. Slim
> rounded body panels, flat footboard, 12-inch alloy wheels, single round LED
> headlamp, upright handlebars, long single-piece seat. Glossy metallic
> silver-blue paint. Plain seamless dark charcoal studio background, soft large
> overhead softbox with a cyan rim light from behind left, no shadows on the
> backdrop. Premium automotive studio photography. Shot on a full-frame DSLR,
> 85mm lens, f/8, 4K, ultra sharp, high dynamic range. No text, no logos, no
> badges, no numberplate, no watermark.

### 8b. Burgman Street 125EX

**File name:** `img_vehicle_burgman_street.jpg`

> A photorealistic studio photograph of an unbranded 125cc maxi-scooter in
> exact side profile at eye level, centred in frame. Large bodywork with a tall
> windscreen, a wide stepped seat, a broad front apron, LED daytime running
> strips, 12-inch alloy wheels. Glossy metallic dark blue paint with a matte
> black lower fairing. Plain seamless dark charcoal studio background, soft
> large overhead softbox with a cyan rim light from behind left, no shadows on
> the backdrop. Premium automotive studio photography. Shot on a full-frame
> DSLR, 85mm lens, f/8, 4K, ultra sharp, high dynamic range. No text, no logos,
> no badges, no numberplate, no watermark.

### 8c. Avenis 125

**File name:** `img_vehicle_avenis_125.jpg`

> A photorealistic studio photograph of an unbranded sporty 125cc scooter in
> exact side profile at eye level, centred in frame. Sharp angular body panels,
> a split seat with a raised pillion section, a sporty front apron with an
> aggressive LED headlamp, black alloy wheels, a short upswept exhaust. Glossy
> metallic blue paint with matte black accents. Plain seamless dark charcoal
> studio background, soft large overhead softbox with a cyan rim light from
> behind left, no shadows on the backdrop. Premium automotive studio
> photography. Shot on a full-frame DSLR, 85mm lens, f/8, 4K, ultra sharp, high
> dynamic range. No text, no logos, no badges, no numberplate, no watermark.

### 8d. Gixxer SF 250

**File name:** `img_vehicle_gixxer_sf_250.jpg`

> A photorealistic studio photograph of an unbranded 250cc fully-faired sport
> motorcycle in exact side profile at eye level, centred in frame. Full sports
> fairing, a low clip-on handlebar stance, a single-cylinder engine, an upswept
> exhaust, a split seat with a raised tail, 17-inch black alloy wheels, a
> vertically stacked LED headlamp behind a clear screen. Glossy metallic blue
> paint with white and black graphics. Plain seamless dark charcoal studio
> background, soft large overhead softbox with a cyan rim light from behind
> left, no shadows on the backdrop. Premium automotive studio photography. Shot
> on a full-frame DSLR, 85mm lens, f/8, 4K, ultra sharp, high dynamic range. No
> text, no logos, no badges, no numberplate, no watermark.

### 8e. Gixxer 250

**File name:** `img_vehicle_gixxer_250.jpg`

> A photorealistic studio photograph of an unbranded 250cc naked street
> motorcycle in exact side profile at eye level, centred in frame. Exposed
> single-cylinder engine, a muscular sculpted fuel tank with tank shrouds, a
> flat wide handlebar, a round LED headlamp, a short upswept exhaust, a split
> seat, 17-inch black alloy wheels. Matte black paint with metallic blue
> accents on the tank. Plain seamless dark charcoal studio background, soft
> large overhead softbox with a cyan rim light from behind left, no shadows on
> the backdrop. Premium automotive studio photography. Shot on a full-frame
> DSLR, 85mm lens, f/8, 4K, ultra sharp, high dynamic range. No text, no logos,
> no badges, no numberplate, no watermark.

### 8f. Gixxer SF (155)

**File name:** `img_vehicle_gixxer_sf_155.jpg`

> A photorealistic studio photograph of an unbranded 155cc fully-faired sport
> motorcycle in exact side profile at eye level, centred in frame. Compact
> sports fairing, a slightly raised handlebar, a small single-cylinder engine,
> a split seat, 17-inch alloy wheels, a horizontal LED headlamp behind a small
> screen. Glossy metallic silver paint with blue and black graphics. Plain
> seamless dark charcoal studio background, soft large overhead softbox with a
> cyan rim light from behind left, no shadows on the backdrop. Premium
> automotive studio photography. Shot on a full-frame DSLR, 85mm lens, f/8, 4K,
> ultra sharp, high dynamic range. No text, no logos, no badges, no
> numberplate, no watermark.

### 8g. Gixxer (155)

**File name:** `img_vehicle_gixxer_155.jpg`

> A photorealistic studio photograph of an unbranded 155cc naked street
> motorcycle in exact side profile at eye level, centred in frame. Exposed
> single-cylinder engine, a compact muscular fuel tank, a flat handlebar, a
> vertically stacked LED headlamp, a short exhaust, a split seat, 17-inch alloy
> wheels. Glossy metallic red paint with black accents. Plain seamless dark
> charcoal studio background, soft large overhead softbox with a cyan rim light
> from behind left, no shadows on the backdrop. Premium automotive studio
> photography. Shot on a full-frame DSLR, 85mm lens, f/8, 4K, ultra sharp, high
> dynamic range. No text, no logos, no badges, no numberplate, no watermark.

### 8h. V-Strom SX

**File name:** `img_vehicle_vstrom_sx.jpg`

> A photorealistic studio photograph of an unbranded 250cc adventure-touring
> motorcycle in exact side profile at eye level, centred in frame. A tall
> upright stance, a beak-style front fender, a small windscreen, a wide
> handlebar with handguards, a large fuel tank, long-travel suspension, a
> single-cylinder engine, a wide comfortable seat, 19-inch front and 17-inch
> rear spoked wheels with dual-purpose tyres. Matte yellow and black paint.
> Plain seamless dark charcoal studio background, soft large overhead softbox
> with a cyan rim light from behind left, no shadows on the backdrop. Premium
> automotive studio photography. Shot on a full-frame DSLR, 85mm lens, f/8, 4K,
> ultra sharp, high dynamic range. No text, no logos, no badges, no
> numberplate, no watermark.

---

# 9. Dashboard — vehicle hero plate

**File name:** `img_dashboard_hero_plate.jpg`
**Where:** Screen 13, Dashboard — behind the vehicle card, under the 3D model.
**Aspect ratio:** 16:9

> A photorealistic empty dark studio floor with a soft circular pool of cyan
> light in the centre, as if a vehicle is about to be placed on it. Glossy dark
> charcoal floor with a subtle reflection, deep navy background fading to
> black, faint volumetric haze catching the light. No subject in frame, just
> the lit floor and atmosphere. Premium automotive studio photography. Shot on
> a full-frame DSLR, 35mm lens, f/8, 4K, ultra sharp, high dynamic range. No
> text, no logos, no watermark, no vehicle, no people.

*This is the stage the 3D model stands on — see the Figma brief for the 360°
viewer.*

---

# 10. Service screen — hero

**File name:** `img_service_hero.jpg`
**Where:** Screen 18, Service — above the status card, and the empty state.
**Aspect ratio:** 16:9

> A photorealistic close-up of clean mechanic's tools laid out neatly on a dark
> workshop bench — a torque wrench, a socket set and a clean oil filter — lit
> by a single soft overhead light. Deep charcoal and navy tones with a faint
> cyan reflection on the chrome. Shallow depth of field, generous negative
> space on the left. Premium editorial product photography, clean and orderly,
> not greasy. Shot on a full-frame DSLR, 85mm lens, f/2.8, 4K, ultra sharp,
> high dynamic range. No text, no logos, no brand names, no watermark, no
> people.

---

# 11. Safety screen — hero

**File name:** `img_safety_hero.jpg`
**Where:** Screen 19, Safety — behind the emergency card header.
**Aspect ratio:** 16:9

> A photorealistic close-up of a plain matte black full-face motorcycle helmet
> resting on a dark surface, lit from the left by a soft light with a subtle
> red rim light along the right edge of the visor. Deep charcoal background,
> dramatic and serious in mood but calm, not alarming. Shallow depth of field.
> Premium automotive editorial photography. Shot on a full-frame DSLR, 85mm
> lens, f/2.8, 4K, ultra sharp, high dynamic range. No text, no logos, no
> badges, no watermark, no people.

---

# 12. Statistics screen — hero

**File name:** `img_statistics_hero.jpg`
**Where:** Screen 16, Statistics — header band, and the empty state.
**Aspect ratio:** 16:9

> A photorealistic long-exposure photograph of an empty winding mountain road
> at dawn seen from above, with soft mist in the valley and cool blue light.
> Very dark and calm, plenty of negative space in the upper half. Deep navy and
> slate tones with a faint warm glow at the horizon. Cinematic landscape
> photography. Shot on a full-frame DSLR, 35mm lens, f/8, 4K, ultra sharp, high
> dynamic range. No text, no logos, no signage, no watermark, no vehicles, no
> people.

---

# 13. Notifications — empty state

**File name:** `img_empty_notifications.jpg`
**Where:** Screen 17, Notifications — "You're all caught up".
**Aspect ratio:** 1:1

> A photorealistic minimal still-life of a single small brass bell lying on its
> side on a dark charcoal surface, softly lit from above with a faint cyan rim
> light. Very simple, calm, lots of empty space around the subject. Shallow
> depth of field. Premium minimal product photography. Shot on a full-frame
> DSLR, 85mm lens, f/2.8, 4K, ultra sharp, high dynamic range. No text, no
> logos, no watermark, no people.

---

# 14. Profile — default cover

**File name:** `img_profile_cover.jpg`
**Where:** Screen 21, Profile — behind the avatar in the profile card.
**Aspect ratio:** 3:1 (wide banner)

> A photorealistic abstract close-up of dark brushed metal with a smooth
> diagonal gradient of blue and cyan light sweeping across it, like a reflection
> on a motorcycle fuel tank. Very dark, smooth, no hard edges, nothing
> recognisable. Deep navy to near-black. Premium abstract product photography.
> Shot on a full-frame DSLR, 85mm lens, f/2.8, 4K, ultra sharp, high dynamic
> range. No text, no logos, no watermark, no people, no objects.

---

# 15. About screen — header

**File name:** `img_about_header.jpg`
**Where:** Screen 23, About — behind the app information card.
**Aspect ratio:** 16:9

> A photorealistic wide shot of an empty road stretching to the horizon at
> twilight, shot from very low near the road surface, with the asphalt texture
> sharp in the foreground and the road blurring into deep blue distance. Cool
> navy and cyan tones, a faint warm band at the horizon. Calm, aspirational,
> cinematic. Shot on a full-frame DSLR, 24mm lens, f/8, 4K, ultra sharp, high
> dynamic range. No text, no logos, no signage, no road markings text, no
> watermark, no vehicles, no people.

---

# 16. App store / promotional key art (optional)

**File name:** `img_promo_keyart.jpg`
**Where:** Not in the app — for a Play Store listing or a presentation.
**Aspect ratio:** 16:9

> A photorealistic hero shot of an unbranded modern scooter parked on a rooftop
> car park at night with a city skyline behind, three-quarter front view, lit
> by a strong cyan rim light from behind and a soft warm fill from the front.
> Wet ground reflecting the lights. Dramatic, premium, cinematic automotive
> advertising photography with space in the upper third for a headline. Shot on
> a full-frame DSLR, 35mm lens, f/2.8, 4K, ultra sharp, high dynamic range. No
> text, no logos, no badges, no numberplate, no watermark, no people.

---

## Icons — do NOT generate these with AI

The app's icons all come from **Material Symbols**, which is Apache 2.0
licensed, already bundled, and vector — so they stay sharp at any size and cost
nothing. AI-generated icons would be raster, inconsistent between screens, and
would not follow the theme colour. Nothing to do here.

The one exception is the **RideConnectX wordmark**, which is already traced as
a vector (`ic_rcx_wordmark.xml`) from your approved logo. Leave it alone.

---

## Checklist

**All 23 generated, processed and wired in — 15 August 2026.** The originals
live at `C:\Users\eshwa\Downloads\App images\Images` and are never edited.
Everything in the app is produced from them by two scripts:

```bash
python tools/images/prepare_vehicles.py --src "<originals>" --out app/src/main/res/drawable-nodpi
python tools/images/prepare_scenes.py   --src "<originals>" --out app/src/main/res/drawable-nodpi --store-out docs/store
```

The vehicles are cut-outs (background removed, contact shadow added); the other
fifteen are scenes and are only cropped, resized and re-encoded. Re-running
either script is safe and reproduces the same output.

`img_promo_keyart` lands in `docs/store/`, not in `res/` — it is store artwork
and has no slot in the app.

| # | File name | Screen | Ratio | Done |
|---|---|---|---|---|
| 1 | `img_welcome_hero.jpg` | 02 Welcome | 4:5 | ✅ |
| 2 | `img_onboarding_navigation.jpg` | 03 Onboarding 1 | 1:1 | ✅ |
| 3 | `img_onboarding_bluetooth.jpg` | 04 Onboarding 2 | 1:1 | ✅ |
| 4 | `img_onboarding_intelligence.jpg` | 05 Onboarding 3 | 1:1 | ✅ |
| 5 | `img_permissions_hero.jpg` | 10 Permissions | 16:9 | ✅ |
| 6 | `img_pairing_cluster_select.jpg` | 14 BLE Pairing | 4:3 | ✅ |
| 7 | `img_navigation_background.jpg` | 15 Navigation | 9:19.5 | ✅ |
| 8a | `img_vehicle_access_125.jpg` | 11 Vehicle | 16:9 | ✅ |
| 8b | `img_vehicle_burgman_street.jpg` | 11 Vehicle | 16:9 | ✅ |
| 8c | `img_vehicle_avenis_125.jpg` | 11 Vehicle | 16:9 | ✅ |
| 8d | `img_vehicle_gixxer_sf_250.jpg` | 11 Vehicle | 16:9 | ✅ |
| 8e | `img_vehicle_gixxer_250.jpg` | 11 Vehicle | 16:9 | ✅ |
| 8f | `img_vehicle_gixxer_sf_155.jpg` | 11 Vehicle | 16:9 | ✅ |
| 8g | `img_vehicle_gixxer_155.jpg` | 11 Vehicle | 16:9 | ✅ |
| 8h | `img_vehicle_vstrom_sx.jpg` | 11 Vehicle | 16:9 | ✅ |
| 9 | `img_dashboard_hero_plate.jpg` | 13 Dashboard | 16:9 | ✅ |
| 10 | `img_service_hero.jpg` | 18 Service | 16:9 | ✅ |
| 11 | `img_safety_hero.jpg` | 19 Safety | 16:9 | ✅ |
| 12 | `img_statistics_hero.jpg` | 16 Statistics | 16:9 | ✅ |
| 13 | `img_empty_notifications.jpg` | 17 Notifications | 1:1 | ✅ |
| 14 | `img_profile_cover.jpg` | 21 Profile | 3:1 | ✅ |
| 15 | `img_about_header.jpg` | 23 About | 16:9 | ✅ |
| 16 | `img_promo_keyart.jpg` | — | 16:9 | ✅ |
