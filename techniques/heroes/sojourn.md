# Sojourn (hub)

Everything learned about Sojourn, by subject. The tenth hero (issue #2) and the first Damage hero: Damage, Sharpshooter.
Up: [Techniques](../README.md) · the method: [reading_game_data](../game-data/reading_game_data.md) · built the way Juno
was: [juno](juno.md) (her movement hooks) and Ana (a gun on bone_0606: [ana](ana.md))

## Her notes and sources
- **Numbers:** `tools/reference/sojourn.md` (Blizzard's patch notes to 2026-11 and the wiki's revision 408636; nothing has
  changed her kit since the data build: the last kit change was BZ 2026-06-16).
- **The data map:** `tools/reference/sojourn-map.md`; the detail in `E:\Overwatch Archive\research\sojourn\`
  (`work_kit\kit_sojourn.md`, `work\assets.md`, `work\clipmap.md`, `extract\`).
- **The plan:** `tools/reference/sojourn-plan.md` (where the sources disagree, what maps onto Overcraft, the order).
- **Clip maps:** `tools/clipmaps/sojourn.txt` (3P, 235 clips), `sojourn_1p.txt` (79), `sojourn_weapon.txt` (the Railgun, 23).
- **Code:** `dev.overcraft.sojourn` (`Sojourn`, `SojournStats`: every number with D / BZ / W and its confidence),
  `client/ClientSojourn`, `OwMovement.slideTravel`.

## The models (step 1)
Exported from the research extraction (build 154088) with `tools/owm_survey.py --glb --keep=<clip map>` (Blender 5.2,
headless, Overwatch checked closed first), copied into the shared dev pack (`overcraft_ana/local-assets/pack`) as
`hero_models/sojourn.glb`, `sojourn_1p.glb`, `sojourn_weapon.glb`:
- 3P: `Models/DCA7.00C/DCA7.owmdl`, no `--entity` (9 meshes, 288 bones, 235 of 255 clips kept; 20 minutes). One material
  without a colour (8210, a Normal input only): its one texture 21C98 is a soft white blob, a glow card, not a body part.
- 1P arms: `DCA8.owmdl` with `--entity=Entities/Gameplay1P/Gameplay1P.owentity` (2 meshes, 138 bones, 79 clips; the Railgun
  child entity's rig dropped by owm_survey as meant).
- The Railgun: `D7B4.owmdl` (7 meshes, 26 bones, 23 clips). Two materials with no colour and no inputs (9829 / 982A): not seen
  missing on the gun from any side.
- **The gun is a held weapon on her carrier bone** (`HeroWeapon` SPECS: `bone_0606` in both views, hp 00C9, no offset), as
  Ana's rifle: in the select screen, 1P and 3P it sits in her hands with no correction (its idle doesn't bake the root).

## Steps 1-2 (the shell, Power Slide), played 2026-11-02
Dev client (`OC_ASSET_PACK` = the Ana checkout's pack), fresh flat survival worlds `soj1`, `soj2`, `soj3`, noon, mobs off and
killed; every speed from the harness's `trace` (one line a tick: speed from the position change, vertical speed, height),
screenshots in `run/screenshots/` (not committed), each looked at.
- **Select screen** (`s1_select_sojourn.png`, `s1b_select.png`): her model holding the Railgun, "DAMAGE SHARPSHOOTER, 225
  HEALTH", five kit icons (LMB Railgun, RMB Charged Shot, LSHIFT Power Slide, E Disruptor Shot, Q Overclock). **Found:** the
  roster's DAMAGE header ran into SUPPORT's (`s1_select.png`): a group of one portrait is narrower than its header. A
  group is now as wide as its header (`HeroSelectScreen.groupWidth`); fixed in `s1b_select.png`. The portraits moved: Sojourn
  at GUI (127, 216), Kiriko's row starts at 164.
- **In play** (READY): 18 Minecraft health (225), her HUD (`s1_1p.png`, `s1b_1p_hud.png`): Power Slide (LShift) and Disruptor
  Shot (E) boxes, the Charged Shot panel with the energy number, the Railgun's 45 | 45. **Found:** the energy number in her
  red vanished over her red gun in first person; now white with the brush behind it (`s1b_1p_hud.png`). The energy only
  ever reads 0 until the Railgun (step 3): its band filling is **not yet seen**.
- **Looked at:** first person (`s1_1p.png`: the Railgun big at the lower right, her right hand on its grip), third person back
  and front (`s1_back.png`, `s1_front_zoom.png`: the gun in both hands, pointed ahead), the pause menu's hero card
  (`s1_pause.png`). Save and Leave, rejoin: the select comes up with Sojourn chosen, "ESC KEEP HERO" (`s1_rejoin.png`).
- **Power Slide on flat ground** (no keys: forward): 9.02, 10.34, 11.66, 12.98, then 14.30 m/s from 0.2 s; from 0.3 s before
  the end 13.27 ... 9.17; 20 ticks; 13.2 m (the plan: 9.0 / 14.3 / 8.1 m/s, W about 13 m). The cooldown 7 s from its end.
- **The slide jump** (jump 0.6 s in): 20.80 m/s along (14.3 + 6.5: W's 20.5 "+273 %") and up at 17 m/s; **apex 7.24 m**,
  36 ticks (1.8 s) in the air, **17.9 m** of reach in the air (`s2_jump_front*.png`). The plan's 6.9 m was worked with 17.5 m/s²
  rising; our mover rises at 16.7 m/s² (OwMovement.GRAVITY_UP, fitted to the 0.98 m jump), which with the data's 1.4 x for
  0.25 s gives 7.2 m: the same arithmetic. W's "about 11.7 m" is far under our 17.9 m of reach: the 20.8 m/s decays at the
  default mover's air decay (2 per second above 5.5) and nothing else slows it. **Open:** the data's xF808ACE8 1.15 during
  the jump's 0.25 s may be that decay's field (Mercy's launch sets it to 0); a Workshop recording settles the reach.
- **No fall damage** after the slide or its jump: the jump's landing 18 -> 18; a slide off a 10 m ledge 18 -> 18, 25 m out;
  the control, walking off the same ledge: 18 -> 11 (and a /tp 10 m up: 17 -> 11).
- **Off a ledge:** at the edge 20.9 m/s (+280 % in the air) with her fall held at exactly -2.00 m/s, then the ramp down.
- **Into a wall** (stone 3 m ahead): stopped at its face, the slide ended 0.3 s later (mode off, cooldown 7 from there),
  before its 1 s.
- **Steps:** a 6-step stone stair (0.5 m steps) climbed in one slide, 10 m/s on it (one step a tick: Minecraft's step-up);
  a 1-block step stops her like a wall (Minecraft steps 0.6 m; ours). Down the same stair: off its top edge she flies out
  over it, sinking at 2 m/s (the data's fall cap), 18 -> 18 on landing.
- **Started in the air** (jump, then Shift at 0.25 s): 15.6 ramping to 20.9 m/s, her fall at -2.00, then on landing 14.3.
- **Jump in the air:** 3 ticks after leaving a ledge (the 0.15 s grace) it jumps (27.4 m/s along: 20.9 + 6.5); 0.5 s after
  leaving it, 10 m up, nothing (no ground within 2 m).
- **Under a 1.5 m gap** (top slabs): she slides under it in Minecraft's crouching pose (her camera low,
  `s2_slide_1p_low.png`) and stays crouched where it stops her. The low pose (`PlayerMixin.getDesiredPose`) came after the first
  plays: without it the camera stayed at standing height (`s2_slide_1p.png`).
- **Water** (found playing): pressed in 2 m deep water it did nothing but spend its cooldown; slid into a pool it skimmed on
  over the water at 20.9 m/s with a hop (Minecraft's `jumpOutOfFluid` on the one tick swimming ran). Now: refused in water
  (cooldown 0 after the press), and it ends the tick it touches water (`OwMovement.slideTravel`): 20.9 -> 16.7 -> 13.4 ...
  Minecraft's water slowing, no hop. Ours: Overwatch has no water.
- **G mid-slide:** the slide ends at once (Minecraft's friction stops her), its cooldown runs on in Minecraft mode (5.55 ->
  4.95), G back: no slide.
- **The look:** 3P front, the slide's start clip low with a leg out, the slide jump's tucked knee, from behind
  (`s2_sheet.png`); her Power Slide box marked green while she slides, coral as she jumps, then 7.
- **Seen once, not reproduced:** the very first slide off the ledge stopped at its edge (`/fill` and `/tp` in the same batch
  just before); 5 more tries the same way, and after waiting, all went over. Not explained.
- Not yet: the 1P slide additives (`add_slide_start` / `add_slide_end`) are not played (only the 1P `slide_jump` action);
  the slide's eight body directions (0x72D: which category is which is L) play `slide_dir_214` only; others' clients seeing
  her slide (the synced `SOJOURN_SLIDE` animates her, not checked from a second client); lava, ladders, boats; a night.
