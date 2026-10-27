# Kiriko's coat

Kiriko's cloth in numbers, the worked example of [cloth](cloth.md). Up: [Techniques](../README.md) · hero hub:
[kiriko](../heroes/kiriko.md) · per emote: [kiriko_emotes](../emotes/kiriko_emotes.md#her-coat-in-the-emotes)

Data: `assets/overcraft/cloth/kiriko.json` (tools/cloth_data.py; model 3A.118, full id 0xe8800000000003a).

## The Domino cloth
- **6 pieces of 45, 45, 98, 98, 28 and 28 points** (mirrored pairs): 342 points. 278 follow her `cloth_` bones; 64
  are top-row anchors with no bone.
- Each point has a vertical parent (the grid's columns), a follower bone, and up to four drivers with weights.
- **Drivers:**
  - her body: `bone_0000`, `Spine_3`, `Hips`, `Thigh_L/R`, `Knee_L/R`, `Foot_L/R`, `Arm_L/R`, `Elbow_L/R`,
    `KneeTwist_3_L/R`, `bone_0054` (the waist);
  - 38 coat bones: `bone_14CF`-`bone_14EC` and `bone_2FD8`-`bone_2FE0`.
- **The coat bones are keyed by hand** in about 90 of her clips, run and strafe among them, and 48-51 of her emote clips
  (per emote: [kiriko_emotes](../emotes/kiriko_emotes.md#her-coat-in-the-emotes)).
- Her chains hang from `bone_0054` (24) and `KneeTwist_3_L/R` (4 each: the trouser cuffs).
- Only `cloth_0180` and `cloth_0181` are keyed in clips.

## What it looks like driven
- **The owner, on Kneeling and Fox Cuddles:** "spot on ... perfect matches" (run/share/cloth_*_before_after.png).
- **In normal play:** the coat hangs standing, streams back running, swings wide strafing, and hangs in front of her
  mid-jump.

## Her Domino settings (decoded, [model_data](../game-data/model_data.md#cloth-domino))
| Pieces | What | Settings asset | Damping | Gravity | Colliders |
|---|---|---|---|---|---|
| 0, 1 | back and front waist panels (not mirrors) | 0x3F5 | 1.0 | (0, -8, 0) | Spine_3, Thighs, Hips |
| 2, 3 | the long side tails (mirrored) | 0x37D | 2.0 | (0, -5, 0) | adds Arms, Elbows, Knees, Feet |
| 4, 5 | the trouser cuffs (mirrored to 5 mm) | 0x409 | 0.1 | 0 | Feet |

Likely iterations: 2 everywhere.

## Open
- Domino's physics on top: rigid to the drivers for now. The data to run it is decoded (above); the solver isn't
  written yet.
- Overwatch drives cloth points and turns the bones to follow them; we blend whole bone transforms. It looks the same
  so far.
- The 64 anchor points aren't used.
