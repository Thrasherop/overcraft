# Additive clips: which they are, what their numbers mean, how to add them

Overwatch's layers (breathing, aim sheets, sway, a volley's kick) are additive clips: deltas played over a pose, not
poses. Settled for #75 (2026-11-02) from the importer's source and the data, offline, Overwatch closed.
Up: [Techniques](../README.md) · the export: [animation_import](animation_import.md) · the data:
[reading_game_data](../game-data/reading_game_data.md) · the case that raised it: [juno](../heroes/juno.md)

## Which clips are additive: bit 0x4 of the folder
DataTool files every clip under `Animations/<a>/<b>/` from its header (the clip maps call them `<prio>-<group>`).
Bit 0x4 of `<a>` marks the additive clips: `4`, `6`, `20`, `36`, `132` are additive; `0`, `16`, `128` are poses.
Measured on every keyed track whose bone sits 3 cm or more from its parent (does the first key sit nearer zero or
nearer the bone's rest offset?):

| hero (build 154088) | `<a>` with bit 0x4: near zero / near rest | `<a>` without it: near zero / near rest |
|---|---|---|
| Lúcio | 4: 374 / 0; 36: 42 / 0 | 0: 285 / 1494 |
| Kiriko | 4: 982 / 36 | 0: 1782 / 11473 |
| Jetpack Cat | 4: 1677 / 17; 132: 76 / 1 | 0: 229 / 4660 |
| Mercy | 4: 768 / 5 | 0: 1321 / 11754 |

So it isn't "the 4-* folders". Lúcio's wall-ride and reload leans are `36-38`, Jetpack Cat's quick melee `132-0` and
Juno's 1P `idle_fidget_7` `20-4`: all additive. And a clip named `add_*` in a pose folder is a pose: Brigitte's `0-32`
187D (her clip map: "0x14E override"), her `0-36` run turns, Sombra's 1P `0-32` stealth-move clips. Those were named
`add_*` by mistake and are renamed (`hit`, `run_turn`, `stealth_move_start`...). None of them was played.

The `.owanimclip` header (major, minor, bone count, fps, frames) carries no additive flag: the folder is the only place
it survives. TankLib's own `teAnimation.AnimFlags` has no named members (its metadata), and `OverwatchAnimationClip`
(the writer) holds only the animation, no skeleton, so the values are the file's own, not converted against a pose.

## What the exporter does with them
- `tools/owm_survey.py` imports every clip with `BLAnimation.importAction(data, rig, False)`, the add-on's pose path
  (io_scene_owm 3.3.1, `importer/blender/BLAnimation.py`):
  - `preprocessLoc` (lines 26-36) takes a key's offset in the parent's frame (`bone.matrix.translation = parent.matrix
    @ key`);
  - `preprocessRot` (lines 39-55) takes its turn as the bone's turn from its parent.
  
  So each glb channel holds the file's value as it is, delta or pose.
- The glTF exporter writes every bone into every clip and fills a channel the clip doesn't key with the bone's rest
  pose. As a delta that adds the rest offset and turn a second time. `neutral_unkeyed` keys those channels neutral
  (offset 0, no turn) for additive clips.
- **The bug (#75):** it tested `folder.startswith("4")`, so the 36-*, 132-* and 20-* clips kept rest values on every
  bone the game doesn't key. That is the "keyed offsets equal the bone's rest exactly" of Lúcio's
  `add_wallride_left_lean` Clavicle_R: the clip keys only 77 of its channels. Now `additive_folder` tests the bit.
  `tools/neutralize_additive.py` fixes an exported glb in place without a Blender re-export. It writes neutral keys
  only on channels the `.owanimclip` doesn't key and that hold the rest pose, and fails on anything else.
  Checked against a fresh export of Jetpack Cat's 19A96 and 1913B with the fixed survey: the same 343 and 477 channels
  neutral, and the first keys equal to 2e-6.

## Composing them: the delta in the bone's own frame
`Skeleton.addAdditive` adds every additive clip as one transform after the bone's pose, `base x delta`:
- offset: `t = t_base + r_base * d` (turned by the bone's pose, before this clip's turn);
- turn: `r = r_base * d_r`.

It used to add the offset in the parent's frame (`t_base + d`), with the bone's frame as an option for Juno's torpedo
layers only. The bone's frame is now the rule for every hero. Why:
- **The importer's own additive path does exactly this.** `populateActionAdditive` (`BLAnimation.py` lines 77-109) sets
  location to `bone.matrix_basis @ key` and rotation to `basis_rot @ key_rot`. A Blender pose bone's location is in
  its own rest frame, so in the parent's frame that is `M_pose x M_delta`, the offset turned by the bone's pose.
- **Mirrored pairs are mirror images only that way.** For each pair of bones whose rest
  frames are mirror images, at every key where both offsets are 5 mm or more, read the delta both ways over the rest
  pose and see which reading's world directions mirror. Only keys where the two readings disagree count:
  - Juno's torpedo pods 3CFB / 3CFC (rest identity / 180 degrees about X): (0.279, 0.554, -0.032) and
    (-0.295, -0.554, 0.032). Mirrored in the bones' frames; in the parent's frame one went over her head and one to
    her hip.
  - Juno's 1P 2C1D / 2C1E in every reload layer: (0, -0.023, 0) / (0, 0.023, 0). Error 0.000 read in the bones'
    frames, 0.047 in the parent's.
  - Sombra's 1P knockdown clavicles: (0.037, -0.239, -0.391) / (-0.012, 0.241, 0.391). Error 0.025 in the bones'
    frames, 0.918 in the parent's.
  - Overall, 4-* clips across the 15 hero glbs: 1206 keys for the bone's frame, 196 for the parent's.
    - Lúcio's 1P, Juno's 1P, Sombra's 1P, Jetpack Cat's 1P and Mercy: all for the bone's frame.
    - The parent's-frame keys are all on Overwatch's IK helper bones (00C1 / 00C2 hands, 0180 / 0181 feet), whose
      rest turns (90, 180 degrees) say nothing: the base clips turn them by up to 180 degrees, so a test over the
      rest pose can't read them. And in non-additive or rest-filled clips, where a rest offset is trivially a
      mirror image.

What isn't settled from the data: the IK helper bones themselves, which need their base clip's pose to test. And the
order of a turn's composition (`r_base * d_r`, the bone's frame). Kept, as the add-on and the old code both do it so.

## What the change moves (look at these in game)
The change moves little where a clip's offsets are small, and a lot where a bone with a large turn carries an offset.
Measured over the rest pose on bones that carry skin weight: a measuring script, kept in the scratchpad, not the repo.
Over a real base pose the numbers differ, but not which clips move. Played clips that move 5 cm or more:
- Kiriko 3P `add_breathe`, `add_aim`: bone_2BAF / 006B under her left wrist, a prop holder parked at the model's
  origin, keyed 1.4 m out (0.43 m between the readings). Either way it sits under the floor in front of her.
- Jetpack Cat 3P `add_melee`: 7 cm on her left hand under the new rule; and its 189 rest-filled channels are neutral
  now (her eyes, face and knees no longer pushed out by a second rest offset).
- Lifeweaver 1P: `add_firing_thorn` (36 cm), `add_land_thorn` (40 cm), `add_dash` / `add_dash_thorn` (8 cm),
  `add_land` (7 cm), `add_idle` (5.5 cm), on his hand and blossom bones (3B33-3B59).
- Sombra 1P burst: `add_fire_start` / `add_fire` / `add_fire_end` (5-7 cm, fingers).
- Mercy's blaster's own `add_fire` (7.6 cm, its parts 008D / 009F).
- Juno 3P torpedoes: unchanged (they had the bone's frame already).
Lúcio's layers, everyone's breathing but Kiriko's, Ana's and Brigitte's played layers move under 5 cm.

## Clips named for what they are
- `add_*` only for clips from an additive folder (bit 0x4); a pose clip never gets the prefix, even when it plays as
  a layer.
- Some additive clips are still named without it (1P knockdowns of Brigitte, Mercy and Juno; Juno's 1P `land` and
  idle fidgets; Mercy's blaster `reload`). None is played as a pose today. Played as one, it collapses the arms (every
  unkeyed bone at its parent): rename it `add_*` first.
