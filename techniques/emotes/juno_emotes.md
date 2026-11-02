# Juno's emotes

What Juno's 22 emotes taught (2026-11-01). Up: [Techniques](../README.md) · the method:
[emote_reproduction](emote_reproduction.md) · her hub: [heroes/juno](../heroes/juno.md) · heroes before her:
[sombra_emotes](sombra_emotes.md), Brigitte's (her hub, [brigitte](../heroes/brigitte.md#what-step-9-emotes-taught)) ·
geosets: [model_data](../game-data/model_data.md#geosets)

## The pipeline, run for her
- Clips: `owm_survey.py --glb --clips-only --keep=tools/clipmaps/juno_clips.txt --emotes=<research\juno\extract\Unlocks\Heroes\Juno\Emote>`
  on her model 0159.118: 75 emote clips (28 MB), as `hero_models/juno_clips.glb`, merged into her model by bone name.
- Data: `tools/emote_data.py juno <research\juno\extract> <...\Models\000000000159.118\000000000159.118.owmdl> <pack juno.glb>`
  (`EMOTE_CONTROLS=1` lists every EntityControl; `EMOTE_UNLOCK=<dir>` dumps each emote's blend tree set) ->
  `emotes/juno.json`. Props: `build_asset_pack.py props <research\juno\extract> juno` (with `OC_ASSET_PACK` set).
- Her 22: Heart Wave, Bunny Hop, Orion Dance, Listen Up, Mic Drop, Salty, Disapproving, Giddy, Laser Training, Heroic,
  Laugh Attack, Winding Down, Game Start, Release!, Galaxy Gazing, Crazy Choreography, Chun-Li Wins, Spinning Bird Kick,
  Cheer Routine, Blooming, Shuffle, Sledding.

## Her weapons are geosets of her body, and the mask bits aren't the geoset bits
- Her model's geosets (submesh `m_40`, first LOD): **1 her body** (25159 vertices), **2 the torpedo pods** (3572),
  **4 the Mediblaster** (6429), 8 (6901: her first-person arms, no material in the third-person look), 16 a shell.
  The .glb's `Submesh_<i>` is the i-th first-LOD submesh: Submesh_4 the pods, Submesh_5/6/7 the gun.
- **Her emotes' GeoSetFlags only ever read 0, 1, 3, 7**, and 1 is what nearly every emote sets 0.1 s in. Read as
  Kiriko's are (mask bit = geoset bit), that hides her body. It isn't: **mask 1 is the Mediblaster**, shown twice by
  the data:
  - **the gun cell's slot:** her Mediblaster's cell is an entity in slot E709 (5ADB on 01B5). Its put-away windows
    (`EMOTE_CONTROLS=1`, 35 of them) are the mask-1 windows, clip for clip: Heart Wave 0.07-2.70 s against the mask's
    0.10-2.70, Game Start 0.97-3.33 against 1.0-3.3, Cheer Routine from 0.1 s against 0.1 s;
  - **a contact:** Cheer Routine sets mask 1 at 0.1 s and from 0.37 s she holds a pom-pom on 000A, the right hand the
    gun's bone (37DC) stays at, 0.13 m from Wrist_R, in every emote clip but Spinning Bird Kick's.
- The masks nest (1, 3, 7), as Kiriko's 1 -> 3 -> 7 do. **2 is taken as the pods, 4 as geoset 8** (not drawn in third
  person): Sledding sets 3 on purpose (not the shared 7), and only the pods would make that bit mean anything in third
  person. M: the data doesn't say which of 2 and 4 is the pods. Where it shows: the pods are hidden in Sledding (3) and
  in Mic Drop and Spinning Bird Kick (7). If it were the other way round, only Sledding would change (pods docked on
  her back while she sleds).
- `emote_data.py` writes `pieceGeosets` with the **mask bit** each piece answers to (`BODY_WEAPON_GEOSETS`: geoset 4 ->
  mask 1, geoset 2 -> mask 2), only her weapon pieces, so no mask can hide her body.
- **Listen Up and Salty have no mask** (Mic Drop has 7). The shared carrier 18537 puts E709 away over the whole emote
  (as it does every hero's weapon slot: [ana_emotes](ana_emotes.md#listen-up-and-salty-put-the-weapon-away-for-the-whole-emote-tree-058f)),
  so the gun follows those windows where a clip has no mask: `WEAPON_SLOTS["juno"]` = E709, and `"stowPieces": [1]`
  in her data (`EmoteData.stowsPiece`): only the gun follows them, not the pods (no slot of theirs is put away). Found
  playing: in Listen Up she held the gun to the side of her head with her other hand cupped at her ear.
- DA1A (her child 3024, effect 2B1E) is put away 30 times, but never in Heart Wave, Mic Drop or the shared emotes: not
  the pods (Mic Drop's 7 hides them). Not read further.

## Hardpoints
- 01B6 is her own (bone_097F): the sled, Galaxy Gazing's mobile, Game Start's hologram.
- **000A / 000B aren't in any of her models** (Cheer Routine's pom-poms 1920B on both, Mic Drop's mic 1A146 on 000B),
  but she has the bones. Borrowed from Brigitte's model 10C10, which puts them on Weapon_R / Weapon_L with no offset
  (`emote_data.py` BORROWED), and checked:
  - **the mic** in her left fist: its shaft (+Y) 7-9 degrees off the knuckle line through the clip (Brigitte's 4-6, the
    verified tea 9); held out level, then dropped: on the ground (0.05 m) at 2.2 s;
  - **the pom-poms** (a 0.34 m ball each, centred on the hand bone) in both fists, arms up (props at 2.5 m as she jumps).
- **The sled** (1AB3F, 1.52 m long, 0.35 m high, on 01B6): her feet 0.37-0.39 m above its base through Sledding (2-4 cm
  over its top), kneeling in it; the clip rides it 6 m forward in the intro, which we take off (her spot kept).

## Clip chains
- **Shuffle played only its 0.4 s intro** (found playing: `emoteClip` null at 0.4 s). Its intro 18182 starts 0.011 from
  her idle's first frame (her idle hovers and sways) against `EmotePlan.SAME` 0.01, so no clip started the emote and
  the first clip played once. It joins its loop 18183 at 0.0002. `EmotePlan` now takes the nearest clip under 0.013
  (below every non-join measured) when none is within 0.01. Scanned every other hero's emotes: none is in that gap
  (theirs with no start are 0.03 and up: Listen Up, Mic Drop, Salty, Hustle, Regifted). After the fix: intro, then the loop.
- Loops (intro -> loop): Bunny Hop 1A273 -> 1A272, Orion Dance, Galaxy Gazing, Crazy Choreography (11FD5 -> 11FD4),
  Chun-Li Wins, Cheer Routine, Shuffle, Winding Down (its outro F38B isn't played: ours ends a loop where the player
  moves), Sledding.
- **Sledding has three loops** (17E9B, 17E9C, 17E9D; each joins the others at 0.008-0.014): ours plays the first only.
  The game's choice among them is the blend tree's, not read.
- Heroic plays F4F0 (back to her idle); F151 (89 bones) and her idle E94E are in its folder too.

## Props
- **Galaxy Gazing:** 17A7C on 01B6 with its own clips 111C8 (intro) and 111C9: a crescent moon and stars 2.3-2.8 m
  over her as she sits cross-legged looking up. Out of the emote camera's view at its usual pitch: look up to see it.
- **Game Start:** 11DA3, a 1 x 1 m hologram quad that the clip scales 6.85 x on bone_097F, 1.4 m in front of her: a
  wall of light (a circle and beams) between her and the camera from 0.7 s to 2.7 s. It is drawn with Sombra's hologram
  tint (`HeroModelRenderer.HOLOGRAM`, violet 0x8344EF: shader 0x32's tint isn't read per material). M: its own colour.
- Mic Drop's mic falls (a projectile entity in the data); Cheer Routine's pom-poms; the sled.

## The additive question (#75)
Her emote folders hold additive layers (EBA8, her `add_aim`, in most; F504 / F55C / 119FF in Heroic). Nothing looked
displaced: in Heroic at 2.0 s the pods' bones in game (08B2 at 0.21, 1.56, -0.12) are the base clip's to the
centimetre. Not a case for #75.

## Played (2026-11-01)
Dev client, fresh creative flat world `junoemo`, Juno picked from the select screen:
- All 22 by the harness's `emote`, four frames each, 0.8 / 2.0 / 3.6 / 5.6 s (`run/screenshots/je_<id>_<0-3>.png`,
  sheets `jsheet0-7.png`); after the fixes Listen Up, Salty and Shuffle again (`jf_*`, `jfsheet1.png`).
- Held frames (`emoteat`): Game Start 0.4 / 1.2 / 2.2 / 3.0 s (`jg_gs_*`, `jgsheet.png`), Galaxy Gazing 1.5 / 4.0 s and
  looking up (`jg_gg_40e.png`), Sledding from the side and behind (`jg_sled_sheet.png`), Cheer Routine 2.5 s, Mic Drop
  0.6 s and 2.2 s (`jg_props_sheet.png`), Heroic 2.0 s.
- **The player's way:** Esc -> Voice Lines -> Emotes (all 22 listed), Sledding / Galaxy Gazing / Shuffle put on the
  right / down / left slots (`jw_loadout_set.png`); then T held, the right button to the emote page, the pointer right:
  Sledding (`jw_wheel.png`, `jw_wheel_sled.png`); left: Shuffle; down: Galaxy Gazing. A jump ends one; G (Minecraft
  mode) mid-emote ends it.
- Not yet (sounds since done: "Sounds" below): their sounds (no Juno section in `tools/soundmaps/emotes.txt` then: the emote-sounds pass, as Sombra's and
  Brigitte's); first person; a second player watching.

## Sounds
Her emotes' sounds and lines (48 02Cs, 21 voice stimuli) from the clips' effects, played at their data times:
[sounds](../sound/sounds.md#juno-emote-sounds), the record in [juno step 8b](../heroes/juno.md#step-8b-emote-sounds-voice-lines-played).
Two never sound: Winding Down's BAA1 (on its unplayed outro) and Sledding's slide loop F232 (on the 0.033 s clip 1AB5A).
