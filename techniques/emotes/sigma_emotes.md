# Sigma's emotes

What Sigma's 14 emotes taught (2026-11-02). Up: [Techniques](../README.md) · the method:
[emote_reproduction](emote_reproduction.md) · his hub: [heroes/sigma](../heroes/sigma.md#step-11-his-emotes-from-the-data) ·
the hero before him: [sojourn_emotes](sojourn_emotes.md) (a weapon entity's clips with a baked root) · a prop's own clip
spawning more props: [lucio_emotes](lucio_emotes.md) (Dance Party's booth) · the sounds:
[sounds](../sound/sounds.md#sigma-emote-sounds)

## The pipeline, run for him
- Clips: `owm_survey.py --glb --clips-only --keep=tools/clipmaps/sigma_clips.txt --emotes=<research\sigma\extract\Unlocks\Heroes\Sigma\Emote>`
  on his model 106F7: 64 emote clips (18 MB), as `hero_models/sigma_clips.glb`, merged into his model by bone name.
- His hand spheres again: `owm_survey.py --glb --keep=tools/clipmaps/sigma_weapon.txt --clips-from=<the same Emote folder>` on
  106B4: their 41 clips plus the 6 emote clips the data plays on them, as `hero_models/sigma_weapon.glb`.
- Data: `tools/emote_data.py sigma <research\sigma\extract> <...\Models\0000000106F7.00C\0000000106F7.owmdl> <pack sigma.glb>` ->
  `emotes/sigma.json` (`WEAPON_SLOTS["sigma"]` = 54CE, `STOWS`). Props: `build_asset_pack.py props <research\sigma\extract> sigma`
  in a scratch pack: `prop_d7dd` (the spheres' base model, 3 clips), `prop_c5ca` (his rock, 11 clips), `prop_eaad` (Maestro's
  baton), `prop_3b50` (a pumpkin, static), `prop_1024e` (the carrier, AE48 added to Sombra's B9A7), copied into the dev pack.
- His 14: Listen Up, Mic Drop, Salty, Dans, Magic Pumpkins, Applause, Heroic, Inconsequential, Laugh, Maestro, Observations,
  Relaxation, Stepping Stones, Constellation.

## His spheres are an entity in slot 54CE
- His hero entity 2AB1's child 31A7 (the spheres, 106B4) hangs on hp 000A in var 54CE (`research\sigma\work\ents_sigma_out.txt`).
  `EMOTE_CONTROLS=1`: every one of his own controls on his weapon names 54CE.
- **Put away** (action 1): Dans, Magic Pumpkins, Applause, Inconsequential, Laugh, Maestro (0.04 s to the end), Stepping Stones,
  Constellation 2.0-5.9 s, and Listen Up / Mic Drop / Salty by the shared carrier 1857C (tree 058F).
- **Out, playing their own clip** (action 4): Heroic 7459, Observations 72F1, Relaxation 7365 / 7367 / 7366 (with his intro,
  step and loop), Constellation CBE3. Each is 48 bones, the spheres' skeleton.
- His body has no hide masks (every submesh in geoset group -1): no `pieceGeosets`.

## Constellation: a weapon clip keyed in his space that is *not* his hand
- Heroic's, Observations' and Relaxation's sphere clips leave the root at rest: the spheres stay on 000A and only their inner
  bones move.
- **CBE3 keys the spheres' root in his model's space, and it isn't a bake of his hand.** At 0 s it is (-0.63, 1.79, 0.54), his
  idle's Weapon_R to the cm, while his own clip CBDF has Weapon_R at (-0.25, 3.16, 0.57), 1.4 m higher, from its first frame.
  They meet only once his hand is up (3.0 s: root (-0.63, 2.82, 1.12), Weapon_R (-0.54, 2.88, 1.15)), and part again at 6.0 s
  (0.7 m). On the hardpoint the spheres would jump 1.4 m off his hand on the first frame; from his origin they leave his idle's
  place, rise with his hand and come back to it at 6.9 s. So `HeroWeapon.place` puts a sphere clip whose root starts 0.5 m or more
  from rest at his origin (less the root motion kept off him) with its root applied (`keyedInHisSpace`). Sojourn's Railgun clips
  equal her hardpoint to the cm, so both readings fit hers; she keeps the hardpoint.
- The **two copies** of the spheres (D7DD on 01B6 / 01C5 = bone_097F / bone_0984, 1.97-5.9 s, clips CBE0 / CBE2) take over while
  the spheres are put away. His clip leaves those bones at his origin; the copies' roots are keyed in his space and follow his
  Weapon_R (clip 1.0 s = emote 2.97 s: (-0.55, 2.89, 1.15) against his (-0.54, 2.88, 1.15)). As props they are drawn on the
  hardpoint with their whole clip, root included, which puts them there.
- His spheres' 3P melee clip `melee_sm1` keys the root the same way ((-0.63, 1.79, 0.54) at 0 s), which is why `bakedRoot` was
  already true for them: in play their root stays on 000A, as before.

## Props from the data
- **Dans:** a copy of the spheres (D7DD, entity 3A34) on 01B6 = bone_097F for the whole loop 83AB, playing 83AD (root at rest):
  his clip carries bone_097F round in front of him at chest height (0.6-1.0 m from either wrist) and up to 3.3 m at 8-9 s.
- **Magic Pumpkins:** the carrier 1024E (entity 424F, the 9-bone holder Sombra's Browsing uses) at his origin (0004), playing
  AE48; AE48's own effect spawns three pumpkins (3B50, entity 278D) on the carrier's bones 0986 / 0987 / 0989 (hp 01C7 / 01C8 /
  01CA, which only 1024E defines) from 0.9 / 2.87 / 1.93 s: drawn by `EmoteProps.inner`, the prop-spawned-by-a-prop path.
  **Found (fixed in the tool):** `emote_data.py` paired each pumpkin with AE48 itself (the only prop clip as long as the
  clip spawning it); a clip is never its own spawn's prop clip now.
- **Maestro:** his rock (C5CA, entity 3902) at his origin from 1.3 s playing 8087, and the baton (EAAD, entity 3A45) on 01C6 =
  bone_0985 for the whole clip playing 84DA.
- **Relaxation:** the rock at his origin under him, 7340 / 7347 / 7345 with his intro, step and loop.
- **Stepping Stones:** three rocks on 01B6 / 01C5 / 01C6 (AF5C from 0.3 s, AF62, AF66 from 1.0 s), four in the loop (the fourth
  on 000B = Weapon_L, AF60).
- **Mic Drop:** the shared mic 1A146 on 000B, Weapon_L in his own model (no borrowing).
- **A prop two heroes share keeps both heroes' clips:** `build_asset_pack.py props` merges the clips the pack's file already has
  into the new export (`tools/merge_glb_clips.py`, by node name), so building Sigma's 1024E kept Sombra's B9A7 beside his AE48.

## Sounds
30 02Cs (23 his own, 7 the shared achievement ones from Lúcio's folder) and 3 voice stimuli, Sojourn's pipeline:
[sounds](../sound/sounds.md#sigma-emote-sounds).

## Found playing
- **The emote camera framed every hero at 2.5 blocks:** his head left the top of the screen (eye 2.06, 2.5 m tall, hands to
  3.3 m in Constellation). It now orbits out in proportion to the hero's eye height over 1.62 (`EmoteCamera.reach`: 3.2 blocks for
  him; a hero no taller than Kiriko is unchanged). Ours: Overwatch's emote camera distance per hero isn't read.
- **The spheres' copies drew dark:** D7DD's cores are the same material 7C98 as 106B4's under another submesh key
  (3060878AEBDEB872); lit now as the spheres' cores are (`HeroModelRenderer.SIGMA_CORE_BASE`).
- The shared emotes (Listen Up, Mic Drop, Salty) are retargeted clips: his head 1.80 m up in Listen Up against 2.13 in his own
  emotes, so he looks smaller in them. That is the data's.

## Contacts
- **The mic** (000B = Weapon_L): its +Y 16 degrees off his left knuckle line (IndexFinger_1_L to PinkyFinger_1_L) through 0-0.93 s,
  22 at 1.07 s once let go (Sojourn's 12, Juno's 7-9). Seen in his left fist at 0.6 s.
- **Stepping Stones:** at each footstep sound (91C3 on 001C at 2.92 s, 001B at 4.6 s) the stepping foot is 0.12-0.15 m ahead of
  and 0.22-0.24 m over a rock's origin: he steps onto the rocks the data raises.

## Open
- Folder-4 layers 6599 (0.3 s) and 7004 (0.67 s, in 11 of his 14) and Relaxation's 0.47 s 7346 are never played by our plan.
- No separate agent has critiqued the frames yet; a second player (the "others" copies, the props on another client) not tried.
- Whether Overwatch's emote camera stands further back for a tank isn't read (ours scales by eye height).
