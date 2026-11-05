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

`AimSheet.layout`: take the frames where the spine-to-head chain's total turn is near zero; from each, walk both ways while the chain's pitch
falls monotonically, doesn't jump and the yaw stays near; the longest such line wins if it spans 30 degrees or more; else the level frame's
column across rows of seven. A clip that passes neither is no sheet: Sojourn's `add_crouch` is a breath, so her crouch falls through to her stand's.
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

## What a critic should check
- Side view, aim up and down at a level camera (`aimpose <pitch> <turn>`: the third-person camera otherwise swings with the aim): the gun's barrel along the aim at both
  ends, its grip in the hand, on every hero, standing, moving, crouched, in the air, and Ana scoped, Sombra stealthed, Brigitte with her shield up.
- A sheet's change of state doesn't pop at a steep pitch.
- **Not done:** the aim's yaw columns (the body always faces the aim, so never needed); Sombra's `add_aim_hack*` (no client state says she's hacking);
  Sojourn's `add_slide_dir_*` and Juno's `add_double_jump*` (their sheets exist, the stand's or air's plays); D.Va's (not on `next`).
- **Ours:** the driver's range. Pitch +-90 to the line's ends, the old code's choice, which lands the guns along the aim in the shots; df 0xB6's own scale isn't read.
