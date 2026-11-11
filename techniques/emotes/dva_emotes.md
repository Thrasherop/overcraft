# D.Va's emotes

What D.Va's 15 emotes taught (2026-11-11). Up: [Techniques](../README.md) · the method:
[emote_reproduction](emote_reproduction.md) · her hub: [heroes/dva](../heroes/dva.md#step-12-her-emotes-from-the-data-2026-11-11) ·
the hero before her: [sigma_emotes](sigma_emotes.md) · holograms: [sombra_emotes](sombra_emotes.md)

## The pipeline, run for her
- Her emotes were already extracted (2026-11-02, `research\dva\logs\extract-dva-emotes.log`): 15 folders under
  `research\dva\extract\Unlocks\Heroes\D.Va\Emote`, the data's current set (the plan's 15).
- Clips: `owm_survey.py --glb --clips-only --keep=tools/clipmaps/dva_clips.txt --emotes=<that Emote folder>` on the MEKA's model
  FBB0: 57 clips (15 MB, 1.5 min), as `hero_models/dva_clips.glb`, merged into `dva.glb` by bone name.
- Data: `tools/emote_data.py dva <research\dva\extract> <...\Models\00000000FBB0.00C\00000000FBB0.owmdl> <pack dva.glb>` ->
  `emotes/dva.json`. **Fixed in the tool:** her unlock-list name "D.Va" never matched the id `dva` (names now compared without
  underscores; `HERO_FOLDERS` has her folder). No weapon slot: her cannons are the MEKA's mesh, and her only stows are the shared
  carrier's (Listen Up, Mic Drop, Salty), on slots she doesn't have.
- Props: `build_asset_pack.py props <research\dva\extract> dva` in a scratch pack: `prop_38ef`, `prop_38f0`, `prop_38f4` (Game On's
  sprites, 3 clips each), `prop_399f` (Game On's screen, a hologram), `prop_2cde` (Heartbreaker's pixel heart, a hologram, 1.76 m),
  `prop_162ca` (Make it Rain's bill, 16 x 6 cm); the mic 1A146 was already in the pack. Copied into the dev pack.
- Her 15: Heart Power, Listen Up, Mic Drop, Salty, Dance, Bow, ^o^ (id `o`, shown "^O^"), Bunny Hop, Game On, Heartbreaker, Heroic,
  Party Time, Make it Rain, Crazy Choreography, Choreography.

## Which body plays them: the MEKA, every one
Each emote clip's bones against both skeletons (TankLib, `teAnimation.BoneList` against each model's skeleton ids; MEKA FBB0 250
bones, pilot F2FF 206, 163 shared): every body clip of the 15 drives FBB0's 243-250 bones, **87 of them the mech's alone**; 13 bones
are the pilot's alone (her legs, which FBB0 doesn't hold), none of the clips is a pilot clip. FBB0 holds her seated in the cockpit
(her head, hair and body are its submeshes 10-17). So the MEKA plays them all, and the pilot has none: `Comms.emote` refuses an emote
unless she's in her MEKA in hero mode (not the pilot, not the wreck, not Call Mech). Overwatch's own rule for the pilot isn't read; this
is the data having no pilot clip.

## Props from the data
- **Game On:** three sprite entities (16E2 / 16E3 / 16E4: models 38F0 / 38F4 / 38EF) at her origin (0004) and the screen 399F on 007C
  (Spine_1, 0.5 up and 1.0 ahead) over the 9.5 s loop. **Found (fixed in the tool):** by length all three sprites took 3E6E (all
  three have a 1.8 s clip, and the 2-bone ones counted as "hers"). Each model's own clips are in its own folder of the emote
  (`Models/<id>/Animations`, the entity's 020 set names them): 38EF 3E69 / 3E6B / 3E6A, 38F0 3E6E / 3E6C / 3E6D, 38F4 3E72 / 3E70 /
  3E71. `emote_data.py` looks there first now.
- **Heartbreaker:** the heart 2CDE on 007F (bone_0001, 1.21 m up, 1.15 ahead) 0.97-3.2 s. Seen in front of the mech from the side.
- **Make it Rain:** the bill entity 5E1F (162CA) spawned on 01B6 / 01C5 (bone_097F / bone_0984) for 0.067 s at 0.8 s of the intro and
  0.067 s of the loop. 5E1F has a ragdoll (STURagdollConfig) and a contact set: it falls by physics once spawned. The tool now marks
  a ragdoll entity `falls` as it does a projectile (only D.Va's data was regenerated with that rule).
- **Mic Drop:** the shared mic 1A146 on 000B = her bone_08BC, on the MEKA's left arm, no borrowing.

## Contacts (`tools/emote_bones.py`, `emote_contacts.py` on `dva_clips.glb`)
- **Mic Drop:** bone_08BC stays 0.638 m from Wrist_L through the whole clip (a rigid child of the MEKA's left arm) and holds the mic up
  at 2.53-2.56 m through its window 0-0.93 s, then it falls (seen on the ground at 1.6 s, held at 0.6 s on the raised arm from above).
  The pilot's knuckle line doesn't apply: her fingers stay on the controls, 1.0 m away.
- **Make it Rain:** bone_097F is 0.520-0.526 m from Wrist_R from 0 to 0.8 s (on the right arm's end) and leaves it after (1.15 m at
  0.9 s): the bills spawn as the bone is flung off the arm (0.8 s), as the data's window says. Seen in flight at 1.5 s.
- **Heartbreaker:** the heart's centre 0.4 m ahead of her hands (1.45 m up, 0.74 ahead at 1.0 s): a hologram in front of the cockpit.
- **Game On:** the MEKA sits (Spine_1 0.49 m up); her hands at 0.57-0.65 m up, 0.5 ahead; the screen on 007C over them.
- **Every emote:** chained by pose (`EmotePlan`): one-shots Bow, Bunny Hop, Heart Power, Heartbreaker, Heroic (3A6D; the 0/12, 0/36 and
  4/* gallery-preview clips left out), Listen Up, Mic Drop, ^o^, Party Time, Salty end back in her stance; Choreography E0BF -> E0C0,
  Crazy Choreography 12675 -> 12676, Dance 42A1 -> 42A2, Make it Rain F383 -> F384, Game On 3E29 -> loop 3E2A (3E2B, 0.43 s, never
  played: the outro, not read).

## Found playing
- **The emote camera cut her top off** (antennae, raised arms): it framed by eye height, and the MEKA's eye (1.74, the cockpit) sits low
  in a 2.3 m body. `EmoteCamera.reach` now also scales by the box height over 1.97 m (Sigma's 2.5 m over his eye ratio, so Sigma and
  Kiriko frame exactly as before): 2.92 blocks for her. Bow fits now; Choreography's jumps still leave the frame's top at its highest.

## Open
- **Her emote sounds and voice lines are not built**: 02Cs in every emote's effect and voice stimuli 6A1 (^o^), 15E2 / 15E3
  (Choreography); the shared Listen Up / Mic Drop / Salty sounds are Lúcio's and may already play. The emote-sound pipeline
  ([sounds](../sound/sounds.md#sigma-emote-sounds)) wasn't run for her.
- **The canopy is drawn opaque:** her head and body are in FBB0 under it, so ^o^'s and Heart Power's faces can't be seen (as in play
  since step 1). Overwatch's canopy is see-through.
- **Hologram colour:** the heart and the game screen take Sombra's hologram tint (`HeroModelRenderer.HOLOGRAM`, purple, additive): the
  heart reads white-pink, Overwatch's is pink. Each hologram material's own colour isn't read.
- Folder-4 layers 2375 (2.43 s, in 13 of 15) and 2379 (0.63 s) are never played by our plan.
- No separate critique agent; a second player not tried; the real mouse and keys still owed (harness keys only).
