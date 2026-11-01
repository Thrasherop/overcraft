# Sombra's emotes

What Sombra's 20 emotes taught (2026-11-01). Up: [Techniques](../README.md) · the method:
[emote_reproduction](emote_reproduction.md) · her hub: [heroes/sombra](../heroes/sombra.md) · the hero before her:
[ana_emotes](ana_emotes.md)

## The pipeline, run for her
- Clips: `owm_survey.py --glb --clips-only --keep=tools/clipmaps/sombra_clips.txt --emotes=<research\sombra\extract\Unlocks\Heroes\Sombra\Emote>`
  on her skin model F376: 68 emote clips, 25 MB, as `hero_models/sombra_clips.glb` (merged into her model by bone name).
- Data: `tools/emote_data.py sombra ...` -> `emotes/sombra.json`; props: `build_asset_pack.py props <extract> sombra`.
- **One pack for every worktree:** `build_asset_pack.py` takes `OC_ASSET_PACK` (as `gradlew runClient` does), so a feature
  worktree builds into the pack the dev client loads; junctions don't work on E:.
- **Her pistol is in slot 209F** (assets.md "slot var 209F"; `EMOTE_CONTROLS=1`: 28 put-away controls on 209F, no clip on
  it): `WEAPON_SLOTS["sombra"]`.

## A prop's clip can start in her stance (Regifted)
- Regifted's first clip by id, 7CD4, is her hologram tablet's own clip, and it starts in her idle pose, so `EmotePlan`
  took it for the whole emote: she stood still for 4.9 s, never reaching her 8.5 s clip 7C82 (found playing: "emoteClip"
  stuck on emote_regifted_1). The data already marks it `"skeleton": "prop"`; `EmoteData` now leaves such clips out of the
  plan with the gallery previews. After the fix: 7C82 plays, with the presents and the toy.

## Hologram materials
- **Shader 0x32** (Amused's glyph dome 3CDA, Masterpiece's crowd screen 3CF7, Sit's equaliser 49BB) is a light mask the
  shader tints: its colour slot holds the engine's grey swatch 10A3, so exported plain it was a solid grey half-dome at
  her feet. `owm_static.py` (HOLOGRAM_SHADERS) now gives such a material its flipbook mask (the texture with the most
  frames) and names the mesh `holo_*`; `HeroModelRenderer` draws `holo_*` as added light in the strip A06C's violet
  0x8344EF ([sombra-effects](../../tools/reference/sombra-effects.md), "Her tablet ... emote holograms").
- **Her tablet 389D** (shader 0x26, material 38EC) came out as the glow ADC4 for colour with the opacity ADC3 as alpha, a
  few dark specks as a cut-out. Its colour 9FE9 drawn as light all but vanished; its glow layer ADC4 (0403F89A) is the lit
  lilac screen: `owm_survey.py` SCREENS names it, the mesh `holoc_*`, drawn as added light untinted. The tablet is small
  (0.2 x 0.17 m at rest, bones growing it x1.5) and in Browsing its panels open only from 3.8 s to 6.5 s: screenshots at
  2 s showed nothing, and that was the data, not a bug.
- The other 33 materials in 389D's folder are effect materials DataTool collected with it, not on the mesh: read the
  model look (4890 -> 38EC) before trusting a Materials folder.

Played (dev client, creative, fresh world `emo1`, Sombra from the select screen, the harness's `emote`, third person,
screenshots `em_<id>_a/b/c.png`): all 20 (Rainy Stroll's umbrella in her hand; Listen Up; Mic Drop's mic; Salty; Dance;
Groove; Amused's glyphs; Boop; Heroic; Hold On; Marioneta's puppet; Masterpiece's screen; Sit's equaliser; Dance-Off;
Shuffle; Browsing's tablet at her hand; Pet Slicer's robot pet; Choreography; Ed's Saunter; Regifted's presents and toy);
jumping ends each. Not looked at yet: their sounds (with step 8's sound pass) and first person.
