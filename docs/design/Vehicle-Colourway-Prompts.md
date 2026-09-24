# Vehicle colourway photographs — how to add them

The app already supports a different photograph per paint colour. Nothing needs
coding to add one: the lookup is by file name, and a missing variant falls back
to the base photograph, so the set can be filled in one vehicle at a time.

## The file name is the whole mechanism

```
frontend/app/src/main/res/drawable-nodpi/img_vehicle_<vehicleId>_<colorId>.webp
```

`VehicleArtwork` resolves that name at runtime (`colourwayPhoto` in
`presentation/components/VehicleArt.kt`). If the file exists it is used; if not,
`img_vehicle_<vehicleId>.webp` is used. There is no list to update.

**Vehicle ids** — `access_125`, `burgman_street_125ex`, `avenis_125`,
`gixxer_250`, `gixxer_155`, `gixxer_sf_250`, `gixxer_sf_155`, `vstrom_sx_250`.

**Colour ids** come from `VehicleCatalog` — the same id the colour picker
stores. Check `domain/model/VehicleCatalog.kt` for the exact spelling before
naming a file; a mismatch is silent, the app just keeps showing the base photo.

## Before generating 50 images — read this

The Access alone has seven colours. All eight vehicles fully covered is roughly
**50 generations**, and each one has to come back at the same angle, the same
lighting and the same framing as the others or the gallery stops looking like
one set. That is a real afternoon of work.

**Cheaper option that is already built:** the stage light behind the vehicle on
the Dashboard and Profile takes the chosen paint colour, so picking a different
colour visibly changes the screen today, with no images at all. Consider doing
colourways only for the Access — the vehicle this rider actually owns and the
one on screen every day — and leaving the rest on the base photograph.

## The prompt

Take the vehicle's prompt from `Image-Prompts-Gemini.md` and change **only the
paint sentence**. Everything else must stay word for word, or the new image will
not match the set.

For the Access 125, the paint sentence is:

> Glossy metallic silver-blue paint.

Replace it with the colourway you want, keeping the same sentence shape:

| Colour | Paint sentence |
|---|---|
| Pearl Mat Aqua Silver | `Matte pearl aqua-silver paint.` |
| Solid Ice Green | `Solid pale ice-green paint.` |
| Pearl Shiny Beige | `Glossy pearl beige paint.` |
| Pearl Grace White | `Glossy pearl white paint.` |
| Metallic Mat Black No. 2 | `Matte metallic black paint.` |
| Metallic Mat Stellar Blue | `Matte metallic deep-blue paint.` |
| Metallic Mat Fibroin Grey | `Matte metallic warm-grey paint.` |

If a result comes back at a different angle, add
`exact side profile, eye level, centred subject` and generate again — matching
the set matters more than the individual image.

## Processing

Same pipeline as the base photographs, and it needs no changes:

```bash
python tools/images/prepare_vehicles.py --src "<folder of new PNGs>" --out frontend/app/src/main/res/drawable-nodpi
```

Background removal takes the Gemini watermark away with the backdrop, so there
is nothing extra to crop. Rename the output to
`img_vehicle_<vehicleId>_<colorId>.webp` and it is live on the next build.
