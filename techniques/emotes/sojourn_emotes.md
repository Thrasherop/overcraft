# Sojourn's emotes

What Sojourn's 11 emotes taught (2026-11-02). Up: [Techniques](../README.md) · the method:
[emote_reproduction](emote_reproduction.md) · her hub: [heroes/sojourn](../heroes/sojourn.md#step-9-her-emotes-from-the-data-played) ·
the hero before her: [juno_emotes](juno_emotes.md) · a gun on bone_0606 like hers: [ana_emotes](ana_emotes.md) · the
sounds: [sounds](../sound/sounds.md#sojourn-emote-sounds)

## The pipeline, run for her
- Clips: `owm_survey.py --glb --clips-only --keep=tools/clipmaps/sojourn_clips.txt --emotes=<research\sojourn\extract\Unlocks\Heroes\Sojourn\Emote>`
  on her model DCA7: 40 emote clips (15 MB), as `hero_models/sojourn_clips.glb`, merged into her model by bone name.
- Her Railgun again: `owm_survey.py --glb --keep=tools/clipmaps/sojourn_weapon.txt --clips-from=<the same Emote folder>`
  on D7B4: its 23 clips plus the 5 emote clips the data plays on it, as `hero_models/sojourn_weapon.glb`.
- Data: `tools/emote_data.py sojourn <research\sojourn\extract> <...\Models\00000000DCA7.00C\00000000DCA7.owmdl> <pack sojourn.glb>`
  -> `emotes/sojourn.json` (`WEAPON_SLOTS["sojourn"]` = 4E30, `STOWS`). No props to build: Mic Drop's mic 1A146 was already
  in the pack (Juno's, Lúcio's).
- Her 11: Triumph, Listen Up, Mic Drop, Salty, Confidence, Guffaw, Heroic, Standing By, Weapons Check, Handstand
  Push-ups, Wobble (the list shows "HANDSTAND PUSH UPS": `Loadout.emoteName` drops the hyphen of the folder's name).

## Her Railgun is an entity in slot 4E30
- Her hero entity's Railgun 2D20 hangs on hp 00C9 (bone_0606) in var 4E30 (`research\sojourn\work\assets.md`).
  `EMOTE_CONTROLS=1`: every one of her own controls names 4E30, as Ana's name 1EC3.
- **Put away** (action 1): Triumph 0-6.0 s (of 6.47), Heroic 0.03-4.07 s (of 5.0), Handstand Push-ups and Wobble whole;
  Mic Drop by its own block, Listen Up and Salty by the shared carrier 178AD (tree 058F,
  [ana_emotes](ana_emotes.md#listen-up-and-salty-put-the-weapon-away-for-the-whole-emote-tree-058f)).
- **Out, playing its own clip** (action 4): Confidence 8F4B, Standing By 7DD9 (intro) / 7DDB (with 7DD5) / 7DDA (loop),
  Weapons Check 9546. Guffaw neither: the gun stays in her hand as her clip carries bone_0606.
- Her body has no hide masks (every submesh in geoset group -1): Mic Drop's mask 7 hides nothing on her, as meant.

## The Railgun's own clips: root baked on her carrier
- Each emote clip of the gun keys its root (bone_0001, 27 bones: its 26 and a "Pelvis" with no children) in **her**
  space: frame for frame it equals her bone_0606 in the paired clip of hers, within 1 cm over Confidence (26 samples),
  Standing By's intro, short and loop (14, 2, 47) and Weapons Check (18). It is Ana's rifle's idle again (1.44 m up),
  but on emote clips: applied on the hardpoint it would put the gun 1 m over her hands.
- `HeroWeapon.pose` now plays the data's Railgun clip in an emote (`"railgun"`, as the staff and flail are) and its
  `bakedRoot` rule keeps the root on 00C9: only the gun's moving parts play. The gun's other clips (fire, reload,
  energy) keep the root at rest, so the rule changes nothing in play.
- **Weapons Check:** none of the gun's 24 inner bones goes more than 0.6 m from its root: its parts stay on it. What
  rises over her raised left hand at 3.0 s (`swc_3.0_zoom.png`) is her own cybernetic hand opening, part of her mesh.
- **Confidence (a contact):** 1.5-5.5 s bone_0606 lies at 0.08 m up, the gun flat on the ground beside her right foot
  (0.28 m from it); at 5.75 s her right foot is up at 0.70 m and the gun jumps 1 m, into her right hand by 6.0 s
  (0606 to Weapon_R 0.17 m, the 0.176 of her held pose). Seen: on the grass at 3.0 s, kicked up at 5.7 s, held at 6.2.

## Hardpoints and contacts
- 000B (Mic Drop's mic 1A146) is in her own model: Weapon_L, no offset (no borrowing, unlike Juno).
- **The mic:** its +Y 12 degrees off her left knuckle line (IndexFinger_1_L to PinkyFinger_1_L) through its window
  0-0.93 s, 20 at 1.07 s once dropped (`emote_contacts.py ... --knuckles=...`, new for this; Juno's 7-9, the tea 9). Seen in
  her left fist at 0.6 s (`sh_mic_wc.png`), on the ground at 2.0 s.

## Clip chains
- Loops (intro -> loop): Standing By 7DD3 -> 7DD4 (11.7 s), Handstand Push-ups E141 -> E142, Wobble CF17 -> CF19.
  Standing By's 0.37 s 7DD5 and Handstand's 0.37 s E143 are never played by our plan (outros or transitions: not read).
- Heroic plays BDBC; 8367, 6EE1 and 6EE3 are the gallery preview tree's (`"preview"`), left out.

## Sounds
17 02Cs (10 her own, 7 the shared achievement ones from Lúcio's folder) and 5 voice stimuli, Juno's pipeline:
[sounds](../sound/sounds.md#sojourn-emote-sounds), played in [sojourn step 9](../heroes/sojourn.md#step-9-her-emotes-from-the-data-played).

## Open
- A held movement key ends an emote and its held sounds at once; a voice line already started (Guffaw's 6.9 s laugh 06A1)
  plays on, as every hero's. Whether Overwatch cuts it isn't known.
- No separate first-person emote view exists: every emote turns the emote camera on (0 first-person frames).
- A second player watching (the "others" copies of Listen Up's music) not checked.
