# Kiriko (hub)

Everything learned about Kiriko, by subject. She was the flagship: where the methods were learned. Up:
[Techniques](../README.md)

## Her notes
- **Emotes:** [kiriko_emotes](../emotes/kiriko_emotes.md): the spawn table, hardpoints, weapons' geosets, what was
  wrong at first and why.
- **Coat:** [kiriko_cloth](../animation/kiriko_cloth.md): Domino cloth driven by 38 hand-keyed coat bones.
- **Reference research:** [kiriko-reference](../../tools/reference/kiriko-reference.md) (how she looks and plays),
  [kiriko-sounds](../../tools/reference/kiriko-sounds.md), [kiriko-voice](../../tools/reference/kiriko-voice.md),
  [kiriko-effects](../../tools/reference/kiriko-effects.md), [sounds-round8](../../tools/reference/sounds-round8.md),
  `tools/clipmaps/kiriko-research.md` (every clip mapped).
- **Numbers:** `KirikoStats` (each with its source), HANDOFF.md's owner's decisions.

## Facts that come up often
- **Model:** `Heroes/Kiriko/Skin/Overwatch/Overwatch/Models/00000000003A.118` (full id 0xe8800000000003a): 566 bones,
  278 of them `cloth_`.
- **Bones:** Weapon_L = `bone_002C` (her kunai hand), Weapon_R = `bone_004A` (her ofuda hand); the mover (root motion)
  is `bone_0001`.
- **Prop bones:** `bone_2BAF` and `bone_2BBE` (under Wrist_L), `bone_2BAE` (under Wrist_R).
- **Clips:** her full-body clips have 295-296 bones; `4/*` clips are additive.
- **Weapons as model pieces:** ofuda (geoset 1), Suzu (2), the three kunai (4), parked at her feet until a clip takes
  them. The Suzu is hidden except while she throws it.
- **The kunai's ammo comes back** at 65% of the reload (statescript 0x2171).
- **Her states, settled by Lúcio's data:** category 005 is her run (A8C7...), alias 0x006's 4-slot set (AFCB-AFD7)
  is a directional jump (unused so far: her jump is AA37), aliases 0x64-0x66 a knock-down (AF3C plays as her death).
  See [lucio](lucio.md).
