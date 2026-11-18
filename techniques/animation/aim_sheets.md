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

## The newer heroes (#95, 2026-11-13): their sheets hide under other names
D.Va (MEKA and pilot), Pharah, Tracer, Cassidy and Junkrat came after the sheets and had none in use: their clip maps name a clip from
the first alias that holds it, so the stand's sheet is called `add_stand_se0`, or `add_jump_takeoff` when the jump holds it too.
**Find a hero's sheets from the data, not the names:** in `research\<hero>\resolved\<set>.021.json`, each item (alias) whose tree has a
`STU_B391A063` (the Pose2d subclass: the old scan looked for `AnimationPose2d` only and missed the stands) or `AnimationPose2d` with X
mode 6 / Y mode 5; its `m_animation.bound` is the clip. Follow the state's alias down its category switches to the sub-alias the clip we
play sits in (Cassidy's stand: alias 1 > dfE5 default > 803EC0B3, his `idle` 12F4's; the sheet beside it is 12F3).
`HeroAnimator.SHEET_NAMES` maps the generic names to them:

| Hero (set) | Stand | Run | Air / crouch |
|---|---|---|---|
| Tracer (0002) | AB `add_stand_se0` 0 1 [2] 3 4, +31 to -53 | E7 ... `add_crouch_walk*` (alias 5 shares the crouch-walk tree's) | crouch 5F `add_crouch_tree_sm0`; jump 166 `add_jump_takeoff` |
| Pharah (001B) | 44B `add_stand_se0`, rows of 7: 3 [10] 17, +18 to -46 | 3D2 ... `add_move*` | 3F8 `add_jump_takeoff` (hover 02F: the same and `add_hover_*`) |
| Cassidy (00FB) | 12F3 `add_jump_still_takeoff`, rows of 7: 10 [3] 17, +57 to -69 | 1425 `add_move_3`, 1424 ... `add_move_<dir>` (803EC161) | jump 622B `add_jump_takeoff_2` (803EC15D) |
| Junkrat (0372) | 2C5B `add_jump_still_takeoff`, rows of 7: 17 [3] 10, +62 to -48 (also his crouch's and jump's) | 3133 ... `add_move*` | 2C5B |
| D.Va MEKA (0282) | 2375 `add_jump_takeoff`, 53 ... [56] ... 59 (Lúcio's layout), +37 to -65 | 3171 `add_move_2` on all eight (80A08053, the run 30F8's) | 2375 |
| D.Va pilot (048A) | 322E `add_stand_se0`, 28 ... [31] ... 34, +22 to -70 | 321F ... `add_move*` | 3218 `add_jump_takeoff` |

- **Lines stored up end first** (`AimSheet.layout`, 2026-11-13): most of their 7-frame run, crouch-walk and jump sheets run -36 ... 0 ... +66, the
  reverse of every older sheet, and failed the walk. The layout now also reads the clip backwards. It also picks up Ana's
  `add_aim_crouch_walk` (+_scoped) and Brigitte's `add_aim_run_back_left`, which had fallen back to the crouch's and the run's.
- **Cassidy's model was baked at 24 fps** (pack `cassidy.glb` of 2026-11-12 03:22, keys every 1.25 frames, a Blender round trip at its
  default rate). A pose table resampled is garbage: his 21-frame sheets came out as 17 keys between the rows. The pack now holds the
  30 fps export (`research\cassidy\export\3p\00000000EFA3.glb`, same meshes, clips and channels; the 24 fps file is in
  `local-assets\backup_glb\aim95\`). Check a model's key spacing before reading its sheets.
- **The MEKA's cannons hang on bone_0099 / bone_009A** (under her wrists) and bone_08FD (under Spine_1): 2375 turns them 110-121 degrees
  end to end, and the mask's no-`bone_*` rule left the cannons level. `UpperBody.aimMask` takes those three names under those parents
  only (Sojourn's and Jetpack Cat's bones of the same names hang elsewhere).
- **A hero switch clears the picks** (`HeroAnimator.sheetNames`): the first frame of a new model posed under the last hero's names, and
  Cassidy after Pharah cached "no sheet" (found playing).
- **Measured in game** (`aimcheck` barrel pitch at aim -70 / 0 / +70, standing; run record
  [aim_95](../testing/runs/aim_95/run_2026-11-13.md)): Tracer -70 / -13 / +40, Pharah -79 / -9 / +61, Cassidy -81 / -14 / +59, Junkrat
  -71 / -1 / +68, pilot -83 / -27 / +43; the MEKA has no barrel to read: its cannons by eye. The down ends short of +70 are the sheets'
  own (Pharah's stand bends 18 down, the pilot's 22). **Cassidy's run holds his gun down** (barrel +85 at any aim): his run sheet
  1425 turns Arm_R 13 and Weapon_R 24 degrees, mostly his head; that's the data's for that run set.
- **Not done:** D.Va's MEKA while firing (dfE5 cat1A5: 32D7 `add_move` on the stand and run) has no client state; the pilot's crouch
  sheet (`add_base_se1_se1_se1_crouch_move`, 6 x 7, no level frame within 25 degrees) falls back to her stand's; Cassidy's dfE5 catB6
  stand (130E, +26 to -55) is unused, as is the catB6 state.

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
  Sojourn's `add_slide_dir_*` and Juno's `add_double_jump*` (their sheets exist, the stand's or air's plays); the newer heroes' gaps
  (above).
- **Ours:** the driver's range. Pitch +-90 to the line's ends, the old code's choice, which lands the guns along the aim in the shots; df 0xB6's own scale isn't read.

## Lúcio's wall-ride lean (#105, 2026-11-05)
Alias 0x357's other `AnimationPose2d`: 87 frames, **three rows of 29**: level, down (+89), up (-89), Y driver df 14, X driver df 5E3 (3P) / df 299 (1P). The X drivers' writer isn't in his graphs (searched `research\lucio\graphs`): the layout is measured, off bone_0001: column 14 is the aim along the ride, 12.86 degrees a column, the root's yaw undoing the aim (+90 at column 7), so the feet stay on the wall while the body faces the aim, as `HeroAnimator.facing` has it. In the 1P sheets the Camera bone rolls +20 at column 14 and -20 at the ends (the right sheet the other way), 0 square to the wall, linear between, 0 in the rows at +-89.
- `HeroAnimator.wallLean`: the level row only, additive, both views; the aim's pitch stays the aim sheet's and the head's. `CameraMixin.overcraft$wallRoll` turns the camera, `ClientHero.wallRoll` eases it 0.25 s (the node's blend 4.0: M).
- **The sign (M):** the sheet's axis alone doesn't say which way in Minecraft's view: taken as top of the view away from the wall, as his feet on the wall put him.
- **What a critic should check:** Workshop or a clip of the real game for the roll's sign and whether it follows the aim this way; only the local player rides (see [minecraft_26](../minecraft/minecraft_26.md)).
- **The critique's answers (2026-11-05, raw [r1](../testing/runs/polish2/r1_before_exits_flick.txt) before, [g1](../testing/runs/polish2/g1_exits_flick.txt) after):** the roll stayed on (20 degrees) through death's screen, levitation and a hero switch, as `riding` was only cleared by `ride()`, which those skip. `OwMovement.riding` now ends a ride whenever he isn't truly on it. Read 0.0 after each exit, final branch: jump off, water, ladder, levitation (and no ride resumed after it), elytra, G, hero switch, death, respawn. **Elytra's case didn't glide** in the harness (anim stayed `jump_forward`): the exit was played, the elytra branch itself wasn't reached; the ladder and water are walls of them ahead of the ride.
- **The flick (g1, every frame):** roll is rate-limited at 80 deg/s. A +60 aim swing moves the target from 20 to 6.67, reached in 0.17 s, in steps of about 0.7 degrees a frame at 110 fps: no jump. The widest swing (along to back, 40 degrees) takes 0.5 s. Harness aim snaps, so that's the worst case. Judged: no problem; no option added. A player's own feel of it is not judged here.
