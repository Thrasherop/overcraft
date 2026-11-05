# Aim sheets: how the body follows the aim

Third-person spine, arms and gun following the aim's pitch (#95, 2026-11-04). Read from the pulled data, offline, Overwatch closed.
Up: [Techniques](../README.md) · beside: [additive_clips](additive_clips.md), [animation_import](animation_import.md),
[weapon_parts](weapon_parts.md) · code: `AimSheet`, `HeroAnimator.aimSheets`

## What the data holds
An `AnimationPose2d` node inside a `LayerAdditive` (research\<hero>\resolved\*.021.json): the clip, an X driver in mode 6 on df 0xB6 and a
Y driver in mode 5 on df 0x14. The frames of the clip are a **table of poses**, not a motion. Nothing in the node says how the table is laid
out (`m_668D2844` is 3 on all 218 nodes read), and the layouts differ per sheet, so the pitch line is **measured from the clip**. This is the
fallback the owner's rule allows: the data was searched first and doesn't hold it.

| Layout (frames) | Hero, sheet | Pitch line (down end ... [level] ... up end) |
|---|---|---|
| 3 x 21 | Kiriko add_aim 63 | 21 ... [31] ... 41 |
| 21 + 3 x 21 | Ana add_aim 84 | 42 ... [52] ... 62 |
| 21 + 3 x 7 | Sombra add_aim 42, Ana add_aim_crouch 42 | 28 ... [31] ... 34 |
| 3 x 11 + 3 x 11 | Sojourn add_stand / add_air 66 | 11 ... [16] ... 21 |
| 5 + 3 x 5 ... | Lifeweaver add_idle 42 | 0 ... [2] ... 4 |
| 13 x 3 + 5 x 7 | Lúcio add_aim 74 | 53 ... [56] ... 59 |
| one line | every run / crouch-walk / jump sheet (7, 11, 21) | the whole clip |
| rows of 7 yaws | Brigitte add_aim 21, Sombra add_aim_stealth, Sigma 72AC | the level frame's column: 3, [10], 17 (the rows' order differs per sheet) |

`AimSheet.layout`: take the frames where the spine-to-head chain's total turn is least (25 degrees at most: Juno's `add_crouch` has a level frame with a constant offset); from each, walk both ways while the chain's pitch
falls monotonically, doesn't jump and the yaw stays within 55 degrees; the longest such line wins if it spans 30 degrees or more and **turns at least 10 on each side of level**; else the level frame's
column across rows of seven, leaving out rows within 8 degrees of level. A clip that passes neither is no sheet: Sojourn's `add_crouch` is a breath, so her crouch falls through to her stand's.
- **The side rule and the near-level rule came from the critique** ([critique_parts](../testing/critique_parts.md) A1): Sojourn's `add_crouch_walk` passed a span test alone as frames 17, 3, 31 at +39, 0, -3, and aiming up left her gun level. Now 17, [3], 10 (+39, -39).
- **Brigitte's `add_aim_shield`** (rows +27, 0, -37, -74: 10, [3], 24, 17) was dropped by a yaw test of 15 (frame 10's yaw is -16): the test is 20 now.
DevServer `aimsheets [hero]` lists what each hero's clips measure as.

The sign: forward (+X on the chain, spine bending over) is looking down. Checked against Kiriko's (the old code's sign, tested in game), then by looking
at every hero's gun at both ends.

## Which sheet
`HeroAnimator.aimSheetNow` tries names per state, and the clip must measure as a sheet. Crouch-walk (with direction), crouch, Juno's glide, air, run (8-way
where the hero has them), then the stand. Variants `_scoped`, `_stealth`, `_shield`, `_blaster`. Names differ by hero because the clip maps were
named from the tree's place: Sojourn's `add_stand`, `add_air`, `add_run*`; Juno's `add_jump`; Lifeweaver's `add_idle`, `add_idle_thorn`, `add_crouch_idle`;
Sigma's `add_layer_663` (set 0A7E alias 663, data mode 6), `add_jump_takeoff`, `add_stance_sm1_sm1_stand_1ed`. Ana's back-slot run uses her stand's (her clip
map: 3B7D on BR, B, BL). Sheets cross-fade over 0.15 s (ours). New exports: Mercy's 09EF (`add_aim`) and 0AC3 (`add_aim_blaster`) in `mercy_clips`, Jetpack
Cat's 1899F (`add_aim`) in `jetpack_cat_clips`.

## What it moves
`UpperBody.aimMask`: the named upper-body bones, and **bone_0605 / bone_0606**, because Ana's rifle and Sojourn's Railgun hang on hp 00C9 = bone_0606 under the
pelvis, not in the arm chain. Without them the arms lift and the gun stays level. The head's own look turns the rest: `headPitch - AimSheet.pitchAt`, the chain's
measured pitch at that place on the line, not a share. Kiriko's old 0.3 share gave her head 63 degrees on top of the sheet's 44; a measured
share gives 90 in all, and her head no longer folds back.

## Is the gun on the aim? Measured (DevServer `aimcheck`, the barrel from its origin to hp 0006, 2026-11-05)
| Aim | -85 | -45 | 0 | 45 | 85 |
|---|---|---|---|---|---|
| Ana | -87 | -47 | -3 | 41 | 81 |
| Sojourn | -86 | -47 | -2 | 43 | 83 |
| Mercy | -84 | -45 | 0 | 27 | 65 |
| Lúcio | -85 | -49 | -5 | 40 | 78 |
| Sombra (less her muzzle's 22-degree offset from the gun's origin) | -47 | -50 | 0 | 56 | 86 |

So the +-90 mapping lands the guns on the aim to a few degrees (Ana, Sojourn, Lúcio). Sombra's sheet stops at about 50 up and Mercy's at about 65 down: the sheets' own ends. **Sojourn's gun in her thigh fully down is the data's:** the barrel is at 83 of 85, her stance low; we'd have to leave the sheet to move it.
**Sigma's hands barely follow because his stand's sheet doesn't move them:** `add_layer_663` turns Neck and Head 8-31 degrees and Arm_R 2. His fire layer does (`add_fire_layer_sm1`: Arm_R 21, Weapon_R 40), so it plays for 1.2 s after a volley (the time ours). **Lifeweaver's neck fully down** is `add_idle`'s 64 degrees (0 to 4), most of it in his neck: the data's.

## What a critic should check
- Side view, aim up and down at a level camera (`aimpose <pitch> <turn>`: the third-person camera otherwise swings with the aim): the gun's barrel along the aim at both
  ends, its grip in the hand, on every hero, standing, moving, crouched, in the air, and Ana scoped, Sombra stealthed, Brigitte with her shield up.
- A sheet's change of state doesn't pop at a steep pitch.
- **Not done:** the aim's yaw columns (the body always faces the aim, so never needed); Sombra's `add_aim_hack*` (no client state says she's hacking);
  Sojourn's `add_slide_dir_*` and Juno's `add_double_jump*` (their sheets exist, the stand's or air's plays); D.Va's (not on `next`).
- **Ours:** the driver's range. Pitch +-90 to the line's ends, the old code's choice, which lands the guns along the aim in the shots; df 0xB6's own scale isn't read.
