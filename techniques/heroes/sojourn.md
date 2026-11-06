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
- **Code:** `dev.overcraft.sojourn` (`Sojourn`, `SojournStats`: every number with D / BZ / W and its confidence, `Railgun`:
  the rounds, energy, Charged Shot; `Disruptor`: the Disruptor Shot's fields), `entity/RailgunShotEntity`,
  `entity/DisruptorShotEntity`, `client/ClientSojourn`, `client/effect/SojournFx` (her data's
  effects since step 8: [sojourn-effects](../../tools/reference/sojourn-effects.md)), `client/SojournSounds` and
  `sojourn/SojournSounds` (her sounds: [sojourn-sounds](../../tools/reference/sojourn-sounds.md)), `OwMovement.slideTravel`.

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

## Steps 3-4 (the Railgun, its energy, the Charged Shot), played 2026-11-02
Built: `Railgun` (server: an accumulator at 16/s, a spread ellipse, 45 rounds and the 1.2 s reload, energy and its decay, the
Charged Shot's piercing ray), `RailgunShotEntity` (Mercy's blaster shot's shape: an `OwProjectile`, swept by Minecraft's own
hit test with `ProjectileUtilMixin`'s radius, allies passed through), the fire clips (`HeroAnimator.firstShotFire`: a burst's
first shot `add_fire_first` 6F09 / 1P 6C0B, then `add_fire` 6F08 / the 1P's four at random; the gun's own `add_fire_first_1p`
/ `add_fire_1p` / `add_fire_3p` and `add_charged_shot_*` in `HeroWeapon`), the 1P energy additive 7D6C weighted by energy /
100, and stand-in effects (`SojournFx`: a cyan glow and trail per round, a cyan band for the Charged Shot).

**Data vs plan:** the plan's numbers all stand. Added from the data: the Charged Shot waits **0.16 s after the last round**
(D #513 / #230, M) and its press is **buffered 0.15 s** (D SECONDARY's buffer, H). Ours: the rounds leave from her eyes (D:
the gun's muzzle hp 0006), so the crosshair is where they go, as Kiriko's kunai; the Charged Shot reaches 100 m (no number
anywhere); a press while the left button is held stops the rounds so the shot goes (found playing, below); it pierces every
creature on the line (D's pierce block, count 3, is L meaning: not applied).

Dev client, world `soj4` (flat survival, midnight frozen, monsters off), targets NoAI zombies with 200 / 500 health (`Tags`,
read with `data get entity @e[tag=..] Health`), skeletons at 20; each number below read in game, screenshots in
`run/screenshots/` (not committed), each looked at. The harness's `trace` now logs the synced ammo, reload share and meter.
A zombie has 2 armor: Minecraft takes 6.56 % off a 9 (0.72 health) and 1.6 % off anything over 40.
- **Damage, crits:** a body round 0.6728 health off a zombie at 10 m (9 less its armor), a head round 1.3663 (18); a
  skeleton (no armor) at 10 m and 25 m: 0.72 body, 1.44 head (`s3_skeletons.png`). No falloff at 25 m.
- **Rate:** the left button held 2 s: 32 rounds in 40 ticks, four every five ticks; 0.3 s: 5; 0.5 s: 8.
- **Reload:** empty, it starts by itself; 45 back at 19 ticks (0.95 s), firing again at 24 (1.2 s); R the same. G off 0.25 s
  into it: the reload finishes in Minecraft mode (as Juno's), the left click there fires nothing; G back, she fires.
- **Spread** (a zombie 20 m off): one round 0.2 m over its head misses (0 at the start), 2 s held there hits 16 of 32 (the
  ramp reaching down); 0.75 m to its side, 32 held: 0 hits (0.64 sideways: a 1.6 circle would have hit); 1 m over: 0. Single
  rounds 0.6 s after a burst over its head: all miss (back to 0). On its head top, 2 s: 27 of 32 (80 % expected).
- **Energy:** +5 a body round, +10 a head round (2 rounds: 15; 2 heads: 20), +1 an armor stand's body, +2 its head, +1 a
  boat, a minecart, an end crystal (which blew up 10 m off: 18 -> 14.3 for her). **Decay** (`trace`): the last gain at tick
  14, falling from tick 194 (180 ticks = 9 s) at 0.75 a tick, 45.5 -> 0 in 60 ticks (15/s).
- **The energy band** (never seen before): `s3_energy_hud.png` / `s3_energy_zoom.png`, 20 energy: an orange band a fifth of
  the Charged Shot panel under the number; full (100) it turns white (`s3_sheet.png`). Over her red gun in first person the
  orange band is hard to see; the number reads.
- **Allies:** a villager 7 m and a tamed wolf 5 m on the line to a zombie 15 m off: 0.5 s of fire, 9 rounds hit the zombie,
  villager 20 -> 20, wolf 8 -> 8, +45 energy (`s3_allies.png`). The Charged Shot through the villager: zombie -5.12 (65),
  villager 20 -> 20.
- **Water:** a zombie standing in a 2-deep pool 10 m off: 9 of 9 rounds; from under water at a land zombie 20 m off: 8 hits
  (rounds in water slow by Minecraft's 0.8 a tick, as Kiriko's kunai do: they still reach).
- **Charged Shot damage:** energy 30 -> 3.936 health (50, less armor), 100 body -> 9.446 (120), 100 head -> 14.17 (180), 25 ->
  3.542 (45). **Pierce:** three zombies at 8, 12, 16 m: each 3.542. **A block:** a stone wall between the first and second:
  the first 3.542, the two behind untouched (`s4_wall.png`). **Falloff:** 25 energy at 50 m -> 2.659 (x0.755), at 65 m ->
  1.721 (x0.5).
- **Lockout:** after it, rounds again 13-14 ticks later (the left button held through it); with 0 energy the right button
  does nothing and there is no lockout (rounds at once).
- **Found playing (fixed):** the right button pressed while the left is held did nothing (`trace`: the meter never dropped,
  the rounds went on): each round restarted the 0.16 s gate and the press was gone. Now the press is kept 0.15 s (her data's
  buffer) and holds the rounds back: rounds stop at tick 12-13, the shot at 15, then the lockout. Reproduced on the old
  build first, the same steps after the fix.
- **Found playing (fixed):** in her own first-person view each round's glow sat on the crosshair as it left her eyes, a cyan
  blob over the target at 16 a second (`s3_rounds_1p.png`, `/tick rate 4`). Now drawn from 3 m on (`s3_rounds_1p_fixed.png`).
- **The look:** 3P (`s3_rounds_back.png`, `x45.png`): her stance while firing, the Charged Shot's band from the gun. The 1P
  energy additive at 100 against 0 (`s3_sheet.png`): no visible change to the gun's pose (its look is M). The fire clips play;
  not compared frame by frame with the data's timing.
- **Night fight** (3 zombies, a husk, a skeleton 35 m off, scripted aim through `aim.py`): without help she died twice
  (a zombie hits 3 every second or so; her 225 is 18 health; the harness's aim lags moving mobs, the trap in harness.md), the
  first time to old mobs left by a `kill` sent while dead. With Resistance III all five died to her rounds and Charged Shots
  (energy 100 reached three times, `s3_night_fight.png`). After a death: Respawn, still Sojourn, 45 rounds.
- **Every way in:** picked on the select screen in a fresh world and after a rejoin (client restarted): Sojourn, READY.
- **Seen once, not reproduced:** a screenshot after a Charged Shot showed 45 rounds and 100 energy (`s4_3p_charged_front.png`);
  the same steps with a `trace` showed the right numbers. A first Charged Shot through the villager found 0 energy (the 0.5 s
  of rounds before it gained none); the same steps again worked.
- **Not tried:** barriers (+1 energy: needs a second player as an enemy Brigitte; the code reads `Barrier.took`), the ender
  dragon, a second player seeing her rounds and tracer, lava, boats, slopes, firing during Power Slide (allowed: nothing gates
  it), the Charged Shot past 100 m.

## Steps 5-6 (Disruptor Shot, Overclock, Sharpshooter), played 2026-11-02
Built: `Disruptor` (server: the cast, the launch, the fields as a list ticked by the server as Juno's Hyper Ring, their damage
and Deceleration Field's slow), `entity/DisruptorShotEntity` (an `OwProjectile`, 40 m/s for 15 ticks, allies passed through),
Overclock and Sharpshooter in `Sojourn` / `Railgun` (`Sojourn.overclocked`, `Railgun.cap`, `Sojourn.sharpshooter`), stand-in
looks in `SojournFx` (a cyan glow and trail in flight; the field a faint ball, a 4 m dome and a bright core), the Disruptor's
and the Overclock cast's clips in `HeroAnimator` (`disruptor` / `overclock`, their `_slide` copies while she slides; Overclock
has no 1P clip, H). The harness's `trace` now also logs both cooldowns, the second ammo and the ult flag (`c1 c2 s u`).

**Data vs plan:** all the plan's numbers stand. From the data: the field hurts in steps of 17.28 every 0.192 s (W's cadence on
the data's 90/s: an accumulator of 3.84 ticks, 18 steps = 311 in its 70 ticks, not 315); her Disruptor cooldown is written
0.5 s after the launch, so the box shows "just used" (15, coral) from the press until it's written. **Ours:** the shot leaves
her eyes (D: her left wrist hp 0002), as the rounds do; the field reaches a creature when the nearest point of its box is within
4 m; no line of sight (W: the field goes through barriers; nothing in the data asks for one), so it hurts through a wall; G or
death during the 0.17 s cast drops the shot with nothing spent; Overclock runs on through Minecraft mode (G) as other heroes'
ults do; the 9 s before the decay count from Overclock's end. Minecraft's magic damage is cut 85 % on witches (their own rule:
not changed).

World `soj5` (flat survival, midnight, monsters off), NoAI zombies / skeletons with 500 health (`Tags`, read with `data get`),
health in Minecraft points (x 12.5 = Overwatch); scripts over one harness connection, screenshots in `run/screenshots/`:
- **Damage through armor:** a plain zombie (2 armor) and one in full diamond (20 armor, 8 toughness), the field on the ground
  between them: both 500 -> 475.117 (311.0, 18 steps of 17.28), 7.37 health a second while it lasts (92/s; `s5_field_1p.png`:
  the field between them). The first try was at noon: the plain zombie burned in the sun (-1 a second) and read wrong.
- **Cooldown:** ready again 16.1 s after the press (polled at 0.5 s: 0.17 + 0.5 + 15 = 15.67 expected); E shows 15 from the press.
- **Max range in the air** (level aim, nothing hit): a zombie 3 m beside the line at 30 m took 311; 5 m beside it, 5 m past
  it and 4 m short of it, 0: the field opened 30 m out (40 m/s x 0.75 s) and reaches 4 m.
- **A wall** (stone, hit in its middle): a zombie 2.4 m in front and one 2.8 m behind it both took 311 (`s5_wall.png`: the
  field's glow on the wall). With a zombie in the line the shot opened on it instead.
- **The ground at her own feet** (looking down): her 18 -> 18, a villager 20 -> 20, an (untamed, NoAI) wolf beside her 8 -> 8
  (a wolf counts as an ally of hers only when tamed by her; Allies), a zombie 3 m off took 311.
- **Stacked:** five zombies in one block, the field at their feet: each 311.
- **Water:** the shot into a 3-deep pool beside a zombie standing in it: 311 (`s5_water.png`); from under water at a zombie on
  the bank 6 m off: 311.
- **Overclock** (`trace`, one line a client tick): from the Q press her energy +5 a tick, 100 at tick 20; the left button held
  from the press, the first round at tick 16 (the 0.75 s cast: 15 ticks with no fire); the ult flag on 182-184 ticks (184:
  15 + 160 + 9). Three Charged Shots to a skeleton's head during it: 180 each (full energy), the meter back to 100 within 20
  ticks each time (`s6_ult_hud.png`: the band white at 100, the ult ring empty). No ult charge while it runs.
- **Ult charge from damage:** 1 s of rounds into a skeleton's body (135 dealt) took the ult from 3.2 to 150.3 points in 2 s
  (135 + 5 a second); a fight's Disruptor and rounds filled it from 11 % to 100 %.
- **Sharpshooter** (the slide's cooldown in `trace`): a full Charged Shot to a skeleton's head (180) took 1.30-1.55 s off it in
  one step (1.35 expected; the client's samples straddle a tick); four single head rounds (18 each) 0.18-0.19 s each (0.135 +
  the tick's 0.05); four body rounds, nothing.
- **G mid-Overclock:** 1.2 s in, G: Minecraft mode, the hotbar, the left button fires nothing (45 rounds kept), energy 100;
  G back 2.5 s later: rounds again, the ult flag ending at 184 ticks from the press as without G (`s6_mcmode.png`).
- **Not tried:** a second player as her enemy (the Deceleration slow on a hero's own mover, the field on a player), lava, the
  Disruptor against a barrier (the field opens on it, the data says it goes through), the Overclock cast while sliding (its
  `overclock_slide` clip), the 3P stance additive `add_overclock` during it (not played: step 8's look).

## Step 7 (the perks), played 2026-11-02
Each picked through the perk menu (`overcraft perks @s 3`, Left Alt held, the left button for the first of a tier, the right
for the second: `s7_perk_menu.png`, `s7_menu_major.png`), each against its control (`overcraft perks @s 1`: none), same
world, `trace` per client tick:
- **Overcharged:** in Overclock the meter reached 150 (the HUD reads 150, `s7_overcharged_hud.png`) and a body Charged Shot
  on a skeleton did 170 (control: 100 and 120); after the ult 150 -> 145 -> 140 -> 125 -> 110 at 5, 10, 25, 40 ticks: 20 a
  second down to 100 (control: stays 100).
- **Deceleration Field:** a NoAI zombie inside the field: movement speed 0.1725 (`attribute ... get`: 0.23 x 0.75); one 6 m
  off, and the same zombie after the field, 0.23. A zombie walking at her through the field: 2.2-2.6 m/s before it, 1.24-1.56
  inside (about 0.6, not 0.75: the reads are 0.37 s apart with the log's jitter, and Minecraft's walk isn't measured
  finer here), back to 2.2-2.6 when the field ended; the control zombie kept 2.2-2.5 through it (the damage itself doesn't slow).
- **Dual Thrusters:** two slides 1.3 s apart, both 15 ticks (0.75 s) at 14.3 m/s; the control refuses the second press. Its
  charges (`s`): 2 -> 1 at the first press, 0 at the second, back to 1 at 7.0 s after the first slide's end and 2 another 6.8
  s on, one at a time. The slide jump 0.4 s in: 14.3 -> 25.3 m/s along (14.3 + 11; W's 25), rising 12.6 m/s on the first tick
  (13.75 under the 1.4 gravity), apex 4.57 m (control: 20.8, 15.8, 7.24 m). The HUD: the box ready with a yellow 2 in its corner
  while both are there (`n_end.png`), the recharge's seconds once none is left (`s7_dual_hud.png`).
- **Friction Generators:** a full slide from 0 energy: 75 (3.75 a tick; control 0); jumped out 0.4 s in: 48.75 (13 ticks).
  **Found playing (fixed):** the first full slide gave 71.25: the gain ran after the slide's tick, so its last tick never
  counted. Now before it (`Sojourn.tick`): 75.00 on the same steps after a restart.
- **Seen once, not reproduced:** after an Overclock, a perk swap to Dual Thrusters and a slide, a screenshot 1.5 s after the
  press still showed her sliding with 1 charge and no recharge (`s7_dual_one.png`): as if the slide began 0.75 s late. The
  same steps twice more with `trace`: the slide at once, the recharge from its end (7.0).

## A night with her whole kit so far (2026-11-02)
`soj5` at 13000 with time running and monsters spawning, survival, her perks Overcharged and Dual Thrusters, 60 % ult; three
zombies, a husk, a skeleton and a spider summoned 15-30 m round her, more spawning; 150 s of a scripted fight (`night.py`
in the session's scratchpad: aim at the nearest, rounds, a Charged Shot at 70 % energy or more, Disruptor under 28 m, the
slide jump away when hurt; real keys): Disruptor four times, 23 Charged Shots, Overclock at 17 s with a spider and a zombie
2 m off (`n_ult.png`), G 2.5 s into it (Minecraft mode, energy 150, health 14.5 / 18: `n_g_midult.png`), G back 1 s later
(hero mode, 150), Charged Shots at 150 after it; the ult full again by 105 s from her damage. She lived (18 at the end: the
regeneration); the far skeletons at 30-45 m were missed for a minute (the scripted aim lags moving mobs: harness.md). The
Disruptor's dome over the zombies (`n_end.png`). Not tried: dying in this night (the respawn was played in step 3), a raid.

## Step 8 (her effects, sounds and voice from the data), played 2026-11-02
Built: her effects from her data ([sojourn-effects](../../tools/reference/sojourn-effects.md); research `fx\Sojourn`, 45 effects,
12 of Overclock's Stadium ones not in the build): `SojournFx` (rounds' comet ribbon 280D, muzzle 2837, hits 31E5, the gun's
energy glow 31E6 by her energy's steps, the reload 30A6, the Charged Shot's muzzle 2834 / beam 2835 / tracer 2BF0 / hits 22BF,
the slide 2905 and its jump 330E, the Disruptor's launch 2C92 / flight 2C8D / field 2228 with Deceleration 6C02, Overclock 2880 and
a glow for 333B), 66 `SOJ_*` palettes (their stops in `SojournPalettes`: the enum's initializer outgrew 64 KB), 12 new masks.
Her stance additive `add_overclock` plays through Overclock. A new synced `HeroManager.SOJOURN_GUN` (energy + Overclock) lets
every client draw her gun and hear Overclock. Her sounds: the map from a parallel pass
([sojourn-sounds](../../tools/reference/sojourn-sounds.md), `tools/soundmaps/sojourn.txt`, 119 events + 29 voice-line unlocks in
the dev pack), wired by `sojourn.SojournSounds` (server one-shots) and `client.SojournSounds` (the slide's loop / scrape / end,
Overclock's loop / ending / end, the Disruptor's flight loop, the field's loop and end, the burn for a player in an enemy field),
`OcSounds.SOJOURN_SOUNDS` / `SOJOURN_VOICE`, her stimuli in `Voice`, her kill lines (`Sojourn.killed`).

Dev client (`OC_ASSET_PACK` = the Ana checkout's pack, `JAVA_TOOL_OPTIONS=-Xmx2G`: memory was short, another session's client
running), fresh flat world `soj8` at night (time stopped; `gamerule spawn_mobs` / `spawn_monsters false`: the old names failed),
a NoAI zombie with 1000 health 12 m ahead; sounds from `heard` / `playing`, screenshots `run/screenshots/s8_*.png` (not
committed), each looked at:
- **Railgun:** per burst `fire_first` then `fire` per round (16/s), `projectile` at her, `hit` + `hit_enemy` per hit,
  `energy.gain` per gain, `fire_end` at the release (heard). The round's ribbon: a cyan comet streak several metres long with a
  bright head (`s8_r8.png`, seen by firing at `/tick rate 2` then turning 75 degrees: along the view it foreshortens to a dot);
  muzzle flares and sparks at the gun (`fxnear`: at hp 0006, 1.06 m up), hit flashes on the zombie (`s8_r1-3.png`).
- **Energy glow:** at 100 a cyan glow on her gun (`s8_rounds_slow.png`); **found:** too strong (a 0.76 m ball at 0.3): alpha
  halved. **Not seen in her own view** (`s8_g100.png`): the view model covers it (open).
- **Charged Shot:** `fire` / `fire_mid` / `fire_low` by her energy (95 -> fire, 40 -> mid, low seen), `shot`, `hit` +
  `hit_enemy` per creature pierced, `hit_block` where it stops (31 m on, heard). The beam and tracer (`s8_c3.png`, `s8_c6.png`,
  `fxslow 0.1` then a turn): **found:** glows every 0.6 m read as beads and the 1F35 sparks as white rings; now every 0.35 m,
  fainter, sparks at 0.6 (`s8_c6.png`: a continuous dotted cyan line). No energy: the "not available" click (heard).
  `charged_kill` ("Vaporized") on a 2-health zombie pierced behind the target (heard).
- **Power Slide:** `slide.start`, her line, `slide.loop`, `slide.scrape.loop`, `slide.end` (heard); thruster glows at her
  hips (`s8_s1.png`, `s8_s3.png`), dust behind; G at 0.3 s: the loops stop, the end rings out (`playing`). The slide jump:
  `slide.jump` (heard), its plume, glows and sparks spawned at hp 002D / 002F (`fxnear`); in screenshots they were mostly behind
  her. Her `slide_jump` exert wasn't said 0.45 s after the slide's (one line at a time: the data's rules).
- **Disruptor:** `cast` + "Disrupting", the flight loop following it, `hit` at the zombie, `field.open` / `field.loop` /
  `field.end` at it (heard). The field (`s8_d2.png`): the swirl decal on the ground, line glows, motes, the core; **found:**
  its sphere at 0.18 alpha was a milky solid dome hiding everything: 0.06 (`s8_d4.png`).
- **Overclock:** `ult.cast`, `ult.friendly`, `energy.fill`, `ult.loop` through it, `ult.ending` 1.3 s before its 8 s end,
  `ult.end`, then "Cooling down" (heard). The look (`s8_o1-3.png`): glows on her gun, a faint cyan glow round her.
  `overclock_kill` on a kill in it (heard). Hero switched mid-Overclock: its loop stopped (`playing`).
  **Found playing (fixed):** "Railgun charged!" was said as Overclock ended (energy still 100); the data's line fires when it
  reaches 100 outside Overclock: a fill in Overclock now counts as said. After the fix: `overclock_end` and no charged line.
- **Reload and melee:** `railgun.reload` (heard); `melee` + her `melee` line (heard).
- **Not tried / open:** a second client (`runClient2` was another session's: the `_3p` copies, the enemy rows, the burn sounds
  on a player in an enemy field were not heard or seen); the in-own-view gun glows (hidden by the view model); death mid-
  Overclock; `slide.touch` / `lift` / `idle.loop` / `energy.glow` / `full` / `spent` (EXTRA, L meanings: not wired); the field's
  flame ring and Overclock's vents (no mesh); material effects; listened to for balance by a person.

## Step 9 (her emotes from the data), played
All 11 from the data ([sojourn_emotes](../emotes/sojourn_emotes.md)): 40 clips in `hero_models/sojourn_clips.glb`, the Railgun
re-exported with its 5 emote clips (`sojourn_weapon.txt`, `--clips-from`), her data `emotes/sojourn.json` (`tools/emote_data.py
sojourn`, slot 4E30), her emote sounds (`tools/soundmaps/emotes.txt`, Sojourn's section: 17 02Cs, 5 voice stimuli; 10 own banks
extracted into `ExtraSounds\Sojourn\Emotes`; [sounds](../sound/sounds.md#sojourn-emote-sounds)), built in a scratch root and
merged into the dev pack (only `sojourn.emote.*`, their subtitles; `sounds.json` and `en_us.json` backed up first). What it took:
- **The Railgun's emote clips key its root at her bone_0606 in her space** (equal within 1 cm in all five): `HeroWeapon.pose`
  plays them (`"railgun"`) with the root kept on the hardpoint (`bakedRoot`), so only its parts move. Confidence lays it on
  the ground and kicks it back up into her hand; Standing By and Weapons Check keep it in her hands.
- `tools/emote_contacts.py --knuckles=` (the handle-across-the-fist check the method describes, now printed): her mic 12 degrees.

Played 2026-11-02, dev client (`OC_ASSET_PACK` = the Ana checkout's pack, `-Xmx2G`), a fresh creative flat world `sojemo`, noon
frozen, mobs off (55 slimes killed), Sojourn picked on the select screen (her portrait, READY); screenshots in `run/screenshots/`
(not committed), each looked at:
- **All 11 by the dev `emote`** (from first person: the emote camera takes over, 0 first-person frames), frames at 0.8 / 2.0 /
  3.6 / 5.6 s (`se_<emote>_<t>.png`, sheets `sesheet0-3.png`), each ended by a held W (0.3 s) or by running out; clips and
  sounds polled every 0.1 s. Chains: Standing By 7DD3 -> loop 7DD4, Handstand E141 -> loop E142, Wobble CF17 -> loop CF19.
- **Held frames** (`emoteat`): Weapons Check 1.0-7.5 s (`swc_sheet.png`), Confidence 3.0 / 5.7, Standing By 1.4, Guffaw 3.0,
  Heroic 4.3, Triumph 6.2 (`sh_sheet.png`), Mic Drop 0.6 and Weapons Check 7.5 close (`sh_mic_wc.png`); Standing By's loop from
  the back camera (`se3p_sheet.png`): the gun away where the data puts it away, back in her hands at Heroic 4.07 / Triumph 6.0,
  on the grass and kicked up in Confidence, the mic in her left fist then on the ground. A separate agent critiqued the sheets:
  its worst points (no mic at 0.8 s, the gun through her torso at Weapons Check 7.5 s) were small-tile misreadings, checked
  close up; the gun's tip touching the grass in Standing By's kneel is the clip's.
- **Sounds** (`heard`, times from the command, about 0.1 s late): Triumph 650F 0.18, "(cheers)" 1211 0.48, "Yeah!" 12DE 3.13,
  6BDB 5.82; Listen Up E548 + E549 0.18; Mic Drop DA44 1.08 (DA4F, 40 %, not in that play); Salty EEA6 0.18; Confidence 6516;
  Guffaw 6511 + laugh 06A1; Heroic 927E 0.18, 6BDB 4.25; Standing By 6510, 650E 0.98, 650D 1.40; Weapons Check 6517;
  Handstand grunts 16B6 3.02, 16B7 / 16B6 in the loop at its data times; Wobble 6BBD 1.77. No missing-sound warning.
- **Ending** (`playing`): Weapons Check's 8.7 s 6517 stops with a held W at 4 s; Listen Up's music fades out over its 1.5 / 2 s
  fades after the emote (ended early or not), gone 3 s later; Guffaw's bed stops, its laugh line plays on (voice lines aren't
  cut: every hero's). G mid-emote: Minecraft mode, the emote and its sound end; G back: Sojourn.
- **The player's way:** Esc -> Voice Lines -> Emotes listed all 11 (`sw_em.png`); Weapons Check / Standing By / Confidence put on
  right / down / left (`sw_em_set_crop.png`); T held, the right button to the emote page (`sw_wheel_right.png`), the pointer
  right: Weapons Check (its sound heard); down: Standing By's loop; left: Confidence; each ended by a held movement key.
- **Seen once, not reproduced:** once, 3 s after Weapons Check was started from the wheel, `playing` listed nothing; three more
  starts (dev and wheel) all listed 6517 through the clip.
- Not yet / doesn't match: Standing By's 7DD5 and Handstand's E143 (0.37 s each) are never played (not read what they are); the
  emote names lose their punctuation ("HANDSTAND PUSH UPS"); a second player hearing the "others" copies; Salty has no salt
  shaker (the data spawns none, as for every hero).

## Step 10 (the release play), played 2026-11-02
`next` (0.3.29) merged first (cb632f3: the additive convention change of #75 took her `localOffsets` overload away; her clips
now go through the one `Skeleton.addAdditive`). Dev client (`OC_ASSET_PACK` = the Ana checkout's pack, `-Xmx2G`), Overwatch
and Battle.net checked not running. Worlds: `soj10` (flat survival: bed and spawn point at -4 -60 -6; a NoAI zombie in an iron
helmet with fire resistance at 0.5 -60 10.5, a NoAI villager at 4 health at 4.5 -60 10.5, a tamed NoAI wolf; a 2-deep pool
at x 10-30 z 20-30 with a lava strip at x 32-36; a ladder at 50 -60 -1; a slab ramp at x -21..-31 z 20-24 up to a stone block
at -32..-40, top -54; later a 9 m stone platform at x 100-124 z -33..-26, top -51) and `soj10n` (normal terrain, survival,
a forest; the night on the plains at -169 66 5 by a village). Scripts over one harness connection (scratchpad `soj10\rp.py`,
`rp2.py` for two clients); health with `data get`, speeds with `trace`, sounds with `heard`. Screenshots in
`run/screenshots/` (`r_*`, `k_*`, `e*`, `ps_*`, `dis_*`, `oc_*`, `pk_*`, `emw_*`, `it{on,off}_*`, `mc*_*`, `n_*`, `nn_*`, `env_*`,
`ramp_*`, `boat_*`, `g_*`, and `sheet_*` of them) and `run2/screenshots/` (`v_*`, the second player's), each looked at.

**Every way in.** On the new world's select screen: her portrait (GUI 113, 192), "DAMAGE SHARPSHOOTER, 225 HEALTH", READY:
18 health, 45 | 45 (`r_select_soj`, `r_join_hud`). H at the bed after Kiriko: Sojourn, 18, 45 (`r_h_soj`). `/kill`: the death
screen, Respawn: Sojourn in hero mode, 18, 45, at the bed (`sheet_r_ways`). Save and Leave, `openworld`: the select screen
with Sojourn chosen and "ESC KEEP HERO" (`r_rejoin_select`), READY: Sojourn. On `soj10n` from its select screen. Back to
`soj10` later: Esc kept Sojourn and the Minecraft mode she was left in. After the client was killed (Save and Leave first)
the select screen still offered her.

**Her kit after the merge** (numbers in Minecraft health; the zombie has 4 armor with its helmet):
- Railgun (the left button, `secondary_fire`): a body round 0.6152, a head round 1.2511 (9 / 18 less the armor), energy +5
  / +10; held 1 s: 17 rounds, 10.46, energy capped at 100; R reloads to 45. Its sounds: `fire_first`, `fire`, `projectile`,
  `hit` + `hit_enemy`, `energy.gain`, `fire_end`.
- Charged Shot at 100: 9.293 (120), on through the zombie to a block 26 m on (`hit_block`); in Overclock to the head 13.94
  (180). With no energy: the "not available" click and no shot.
- **The additive change, looked at:** the 1P gun at energy 0 and 100, three frames each (`sheet_e_energy`): no visible
  change, as before the merge (step 3). Overclock's stance `add_overclock` in 3P front against before it (`sheet_oc_zoom`):
  the same stance, shoulders slightly turned; nothing jumps or floats.
- Power Slide: 9.0, 10.3, 11.7, 13.0, 14.3 m/s, 20 ticks, then down to 0 (`ps_front`); the slide jump 20.8 m/s, apex 7.24 m,
  18 -> 18 (`ps_jump_front`). Disruptor: the zombie 311 (24.88 health), the villager 20 -> 20 beside it, the dome
  (`dis_1p`, `dis_3p`); its sounds and "Disrupting". Overclock: energy 100 at once, `ult.cast`, `ult.friendly`, `ult.loop`,
  `ult.ending`, `ult.end`, "Cooling down"; her cyan glow (`sheet_dis_oc`).
- Perks (`pk_menu`): Overcharged (150 in Overclock, then 139, 109 after: back down to 100), Dual Thrusters (two 0.75 s slides
  1.3 s apart, charges 2 -> 1 -> 0, `pk_dual_hud`), Friction Generators (0 -> 75 over a full slide), Deceleration Field (the
  zombie's speed 0.1725 in the field, 0.23 after).
- Emotes from the wheel (T held, the right button, the pointer right / down / left): Weapons Check, Standing By, Confidence,
  their sounds, the emote camera, ended by W (`sheet_emw`). A voice line from the wheel (up): "Not again" heard.
- **Improved Transparency on** (F3+X; options.txt `improvedTransparency:true`), then the same round off (`sheet_it_on`,
  `sheet_it_off`): rounds 1P and 3P, the Charged Shot, the field 1P and 3P, Overclock's glow, the slide's thrusters, the field
  in the pool seen through the water. No crash, no dark boxes, the same look both ways. options.txt ends
  `improvedTransparency:false`.

**Minecraft mode (G) as Sojourn.** Held items (`sheet_mc2`, `sheet_mc_held`): the iron sword in her own gloved right hand at
the lower right, the swing; bread in hand and eaten (food 8 -> 13); the bow drawn and fired (arrows 16 -> 15); the shield in
the off hand drawn Minecraft's way and raised (with the sword: with cobblestone in hand the use placed a block instead,
vanilla's order); a cobblestone placed and mined back with an iron pickaxe. Survival in `soj10n`, from an empty inventory:
punched oak trunks (4 logs, then 2 more, about 3 s a log), 12 planks and a crafting table in the inventory grid, the table
placed, at it 16 planks, sticks, a wooden pickaxe and a chest (`n_table_pick`, `n_table_chest`); a staircase dug 9 blocks
down with the pickaxe (10 cobblestone, andesite); three wall blocks taken out and cobblestone put back from the hand (3 of 3,
10 -> 7); the chest placed, 10 dirt and 2 sticks stored, its contents read back, reopened (`sheet_n_survival`). A first
chopping script held the attack 7.8 s per log and dug her 7 blocks down beside the tree (the script, not a bug).

**A night on normal terrain** (`soj10n`, time running from 13000, monsters on, five summoned with their AI round her plus the
wild ones, 150 s of a scripted fight, real keys): in the forest the target was a mob in a cave below and she fired into the
ground for two minutes (the script); on the plains by a village (`sheet_nn2`): Disruptors, Charged Shots at 0.7-1.0, Overclock
at 14 s with zombies 2 m off, G 2.5 s into it (Minecraft mode, energy 100) and back, killed by a skeleton at 22 s, back at the
spawn as Sojourn in hero mode, kills in the feed, alive at the end with 18. A run where the script's target choice was broken
let zombies kill her 12 times at the spawn: every respawn Sojourn in hero mode (the one death with G sent by the harness on
the death screen came back in Minecraft mode, the mode she died in).

**Water, lava, slopes, ladders, a boat** (`soj10`):
- Water: walking in 5.5 -> 2.0 m/s swimming; Shift refused in the water (cooldown 0); rounds from the water at the zombie hit
  (4.71 in 0.6 s); slid into the pool: 20.9 m/s off the edge for 3 ticks, then the water's slowing, no hop (`env_water_*`).
- Lava (fire resistance): walking in 5.5 -> 0.78 m/s, Shift refused, rounds from the lava hit (6.06), the Disruptor cast
  from it; 18 -> 18 (`env_lava_*`).
- Slopes: the slab ramp walked at 5.5 m/s to the top (Minecraft mode 4.32), slid up at 14.3 m/s, slid down off the block at
  20.9 m/s with no damage (`ramp_hero`, `ramp_mc`). (A first ramp had a full block at its foot, my build: both modes stopped.)
- Ladder: W climbs 8 m in about 3.5 s (`env_ladder_*`); let go, she slides down, 18 -> 18.
- **Found playing (fixed, 3d6f3a0): a slide jump up the ramp took 4 health.** It grazed the block's top corner past the ramp
  and dropped 6 m. Reproduced on the 9 m platform (slide, 0.4 s, jump, from x 100.5 + an offset): 2 of 7 hurt (landed on the
  edge at 125.13 and slid off, 18 -> 12; grazed it and landed at 128.9, 18 -> 11). Her soft landing ended on the first tick
  on the ground; it now ends after 5 ticks on the ground in a row. After, on a fresh client: 10 of 10 unhurt (one on the edge
  then off to -60, three past it to x 129); walking off the same edge still 18 -> 12, and a slide jump that settled on the
  platform then walked off 18 -> 12.
- **Found playing (fixed, ae763bd): Power Slide in a boat spent its cooldown.** Seated, Shift played its start and her line,
  ended the next tick, 6.7 s left, nothing moved. Refused while seated now: boat and minecart, cooldown 0, nothing heard;
  rowing 6.3 m in 1 s, crouch (LCtrl) gets her out of both, the slide on land after as before. G while seated switched modes.

**The general play: every other hero from H at the bed** (one fight each against the zombie and the villager, `sheet_g_*`),
with Sojourn picked between Lucio and Jetpack Cat, Lifeweaver and Ana, and after Juno (each time 4.92 off the zombie in
0.5 s): Kiriko (kunai -4.49 / -9.29, ofuda 4 -> 13.6, Swift Step 8.32 m, Suzu); Lucio (-6.16, Soundwave -3.28, aura, Amp It Up,
crossfade, Sound Barrier); Jetpack Cat (Pawjectiles -6.52 and 4 -> 9.76, Lifeline on and off, Purr 4 -> 11.2, Catnapper);
Mercy (heal beam 4 -> 11.26, blaster -12.56, Guardian Angel 9.4 m, Resurrect, Valkyrie); Lifeweaver (Blossom 4 -> 11.2,
Thorns, platform, dash 9.58 m, Life Grip 10.94 m, Tree of Life); Ana (4 -> 10, -5.11, scoped, Sleep Dart, grenade 4 -> 11.2,
Nano Boost); Sombra (pistol -9.29, Cyberspace, Translocator 12.6 m, EMP; Hotfix tapped does nothing, as designed); Brigitte
(flail, barrier, whip, pack 4 -> 13.6, Rally); Juno (volley, torpedoes, Hyper Ring, Glide Boost 13.15 -> 7.70 m/s by `trace`,
Orbital Ray 4 -> 19.98).

**A second player** (`runClient2` from this worktree, "Viewer", port 25600, run dir `run2`; `/publish false 25565`, `connect
127.0.0.1:25565`, Kiriko from its select screen; subtitles on):
- As allies (no team): the Viewer sees her rounds' tracers, the Charged Shot's cyan dotted beam, her slide and slide jump,
  the Disruptor's cyan dome and Overclock's glow (`sheet_v_ally`), and hears the `_3p` copies: `railgun.fire_first_3p` /
  `fire_3p` / `fire_end_3p` / `projectile`, `charged.fire_3p` / `hit_enemy_3p` / `shot`, `slide.start_3p` / `loop_3p` /
  `scrape.loop_3p` / `end_3p` / `jump_3p` and her lines, `disruptor.cast_3p` / `flight.loop_3p` / `field.*_3p`, `ult.cast_3p`
  / `loop_3p` / `ending_3p` / `end_3p` and `ult.friendly`. Rounds and a Charged Shot through the Viewer standing on the line:
  20 -> 20, the zombie behind hurt; her field at the Viewer's feet: 20 -> 20, walking in it at 5.5 m/s, no burn sound.
- As enemies (`overcraft team join red` / `blue`, her perks Deceleration Field and Dual Thrusters): 0.5 s of rounds 20 -> 16.4
  (`hit_enemy_3p` heard); her field on the Viewer: walking through it at 4.13 m/s (5.5 x 0.75; the control walk 5.5), 20 ->
  8.94 in about 2 s, `disruptor.burn.start` / `burn.loop` / later `burn.end` heard, "Burning in a Disruptor field" on
  screen, the dome orange from inside (`v_enemy_field`); a Charged Shot in Overclock 20 -> 10.4 (120) and `ult.enemy` heard;
  enemy colours (`sheet_v_enemy_side`): the Charged Shot's beam orange-red, the field's dome orange.
- Seen, not chased: in the enemy field the Viewer's speed dips to 3.5 m/s one tick in four (with the field's damage steps).

`gradlew build` after the fixes: tests and `checkNoOverwatchAssets` pass.

Not tried: Improved Transparency through the Video Settings screen (F3+X toggles the same option); the Nether; a raid; her
emotes seen by the second player; the Disruptor against a barrier; dying mid-Overclock.

## Weapon parts, glows and aim (2026-11-04, #91, #95)
General: [weapon_parts](../animation/weapon_parts.md), [aim_sheets](../animation/aim_sheets.md), [glows](../rendering/rendering.md#glows-emission-from-the-data-2026-11-04-91). What a critic should check is there.

## False headshots (#110, 2026-11-04)
Up: [Techniques](../README.md). One rule for all ten checks now: `dev.overcraft.hero.Headshot`, with `HeadshotTest`.
- **The issue's steps didn't reproduce it.** Two NoAI zombies 12.5 m apart, the first's chest on the line: no false crit. Scanned on the old build: pitches through 8 ray heights at the second zombie with 5 m and 12.5 m to the first: every crit agreed with the 1.49 m band (`rl_t110c`). `kf_soj6`'s own setup isn't recorded;
  a likely cause there is the harness's "`look` and an ability in the same tick act on the old aim" (not shown).
- **What did reproduce:** a steep shot. From a 10 m pillar, 2.5 m out, 77 degrees down: **every** aim from the legs up was a headshot, the Charged Shot x1.5 and the kunai x2, because the hit point is where the ray enters the box inflated by its radius, through the top face (the critique's reading).
- **The fix:** does the shot's line pass through the head? The head: from 0.25 m under the eyes to the box's top + 0.11 m, and 5/12 of the width each side of the middle (a zombie's head is 0.5 m in a 0.6 m box; ours for every mob). A level shot past the side of the head at head height is now a body hit.

| 10 m up, 2.5 m out, aimed at height | Old Charged Shot (energy 50) | New |
|---|---|---|
| legs, 0.3 | 105 (crit) | 60 at energy 40 (body) |
| waist 0.9, chest 1.3, head 1.6, top 1.9 | 105 (crit) | crit: from there the line goes through the head |
| kunai: legs / waist / head | 120 / 120 / 120 | 60 / 120 / 120 |

- **What a critic should check:** whether the waist from 77 degrees should be a head: geometry says yes, a player may not. Only Charged Shot and kunai were played; the other eight sites share the one call by code. Mobs that aren't built like a zombie (a spider's head is at its front; a ravager's): the middle of the box is ours and wrong for them. Not reproduced is not the same as explained: if the owner sees a false crit on a level line, this isn't its cause.

### Round 2: each mob's own head (critique_rules 11, 2026-11-05)
`Headshot`: upright, broad (a golem), a row (the Wither), or front-headed (animals, spiders, a ravager, a ghast's face): placed by the mob's eye height and the way its head faces, since Minecraft's server has no head position. Thickness counts sideways and over the top, up to 0.11 m (a kunai's 0.195 m contact made a cow's flank a crit). **All ours.** Kunai, played (60 body, 120 crit), keys by `KeyMapping.set`:

| Aim | Old | New |
|---|---|---|
| Spider's head, from the front | 60 | 120 |
| Its head, from its side | 120 | 120 |
| Its abdomen: behind, side, top | 120, 120, 120 | 60, 60, 60 |
| Golem's head / shoulder / chest (it had to be turned) | not run | 120 / 60 / 60 |
| Ghast: middle / top / low, from the front | 60 / 120 / 60 | 120 / 120 / 60 |
| Cow's head from the front | 120 | 120 |
| Ravager's head from the front / its flank | 60 / 60 | 120 / 60 |
| Zombie, skeleton, creeper: head / chest | 120 / 60 | 120 / 60 |
| Wither: mid head / body / side head | 120 / 60 / miss | 120 / 60 / miss (the kunai passed: its box is 0.9 m wide; the side heads are outside it) |

- **The waist from 77 degrees still crits** (the line really goes through the head): for the owner to say. **No level-case false crit has shown up.**
- **What a critic should check:** a moving target; a muzzle offset (a thrown kunai's way isn't the eye's); a ghast's face from the side is a crit near its front edge; a hit from behind a front-headed mob with its head turned (`getYHeadRot`).

## The Charged Shot on the press (#126, 2026-11-06)
Up: [Techniques](../README.md) · the harness: [harness](../testing/harness.md) · raw files and scripts: [runs/railgun](../testing/runs/railgun/). The owner: "firing railgun is the highest priority. it cancels reloading, primary fire, all sorts of stuff, with 0 (or at least basically 0) cast time. There is a recovery time, but it fires immediately despite what is currently happening." "Railgun" here is her right button.

**What the sources say**
- **Cast time, H:** W's infobox (Sojourn, page last edited 2026-10-31): "0 second startup + 0.64 seconds recovery". D: nothing between SECONDARY #421 and the volley #450 but energy > 0 (#451), and no wait node.
- **Priority, H for the nodes, M for the stack's meaning:** in 1A23, #75's always-on plug leads to #615 (v2447 = 0, its recovery) -> #326 -> #421: beside the reload (#75's true plug, #233) and the rounds (its false plug, #415 -> #617 -> #1445 -> #527), not under them. #425 pushes v33 at priority 0x79, over the reload's #233 and the rounds' #571 at 0x2. (That 0x79 outranks 0x2 is read from #233 having an under plug at all: the 024 assets' order isn't read.)
- **Reload, H nodes, M meaning:** #233's under plug is #234 v51 = 0, and its top plug's wait #287 and anim #249 stop with it: the reload is over, not paused. #285 fills the magazine only after #287's 0.96 s. So a cut reload keeps the ammo it had (the owner's rule for a cut reload too: `HeroState.interruptReload`), and only an empty magazine reloads by itself after the recovery (#75's second clause needs v2447 = 0).
- **Recovery, M:** #438 v2447 = 0.65, chased at (1 + attack mods) a second (#636), W 0.64. SECONDARY's buffer 0.15 s (H). The rounds wait on it too (#617).
- **0.16 s, corrected:** v173 (#513, after a volley) gates #75's reload, #415's rounds and Disruptor's start. Not #421. The old "0.16 s before a Charged Shot" (M in kit_sojourn.md) was wrong, and it was what a held left button felt like. Those three readers aren't all in the code: see "found, not fixed" below.
- **Held, M:** #421 and #527 use the same plug m_80AF45FB, the one that resumes a held left button's rounds when #527 begins again. A right button still down as the recovery ends would fire again, if a button already down counts when #421 begins (not in the data: see round two below). **The game now keeps one press, one shot: a question for the owner.**
- **Not found:** nothing in Blizzard's patch notes or elsewhere on W about priority or cancelling (searched through the API). Overclock's 0.75 s cast keeps its block: W's text and D #364; how the data blocks it is not read (L), and #421's m_53C4B6A6 = v842 isn't understood. Melee: nothing in her graphs stops #421 during 0043's melee, which pushes v33 at 0x79 as well: an engine-side lock can't be ruled out, so "it cuts a melee's recovery" is L, ours by the owner's "all sorts of stuff".

**The fix**
- Server (`Railgun`): `chargedReady` is its own lock (`sojournChargedLock`) and Overclock's cast. Reload, rounds, melee recovery, slide, Disruptor's cast don't hold it. `chargedShot` cuts a reload (`interruptReload`).
- Client (`ClientSojourn.clicked`, from `KeyMapping.click`, the moment `MouseHandler.onButton` sees the button): with energy and no lock it sends the press at once with her aim, and plays her two sounds, the tracer and the clip. `Railgun.pressedShown` fires on the packet; her copies aren't sent back, others' are. A press it can't call goes at the next tick as before, and the server decides. (Round two below changed some of this.)
- `HeroAnimator.trigger`: "charged_shot" ends the reload's clips, layers and sound on every client.
- **Not called on the client:** asleep (the synced attachment), a screen, the perk menu, the comms key held. Hack doesn't stop it (a weapon; `HeroManager.onInput` silences abilities only).

**Measured** (`presstrace`, one clock; `key primary_fire down` is KeyMapping.set + click, what `MouseHandler.onButton` calls). 5 presses a row (6 for the first four old rows), median / worst ms from the press. "Look" = the tracer made; "frame" = the next frame's world pass reached, not light on a screen; the harness runs at a frame's start, so a real click adds up to a frame's wait on either build. Old: 430712b (polish-4 + the trace), raw `rg_old_*.txt`. New: 4b45d79, raw `rg_new_*.txt`, later commits as noted, `rg_final*.txt`. All at 30 fps except where marked (the window lost focus: 10 fps, which inflates those old numbers). `rg_old_b.txt`'s rows from "in the air" on are void (she had died falling): `rg_old_c.txt` reran them.

| Case | Old: fired, press to sound | Old: to server shot | New: fired, press to sound | New: to look / frame | New: to server shot |
|---|---|---|---|---|---|
| Idle | 6/6, 67 / 117 | 61 / 65 | 5/5, 0.1 / 4 | 0.5 / 4 (the session's first press: frame 289, old 674) | 8 / 47 |
| Mid-reload, 0.3 s in | **0/6, the press lost** | - | 5/5, 0.2 / 0.3 | 0.5 / 3.6 | 14 / 38 |
| Reload past 0.96 s | 6/6, 67 / 100 | 53 / 67 | 5/5, 0.2 / 0.2 | 0.3 / 2.9 | 27 / 40 |
| Reload at its end (asked 1.14 s) | 6/6, 67 / 101 | 48 / 69 | 5/5, 0.1 / 0.1 | 0.3 / 3.1 | 9 / 48 |
| Holding left, firing | 5/5, **168** / 168 | **163** / 165 | 5/5, 0.1 / 0.2 | 0.3 / 3.1 | 44 / 51 |
| Between left shots | 5/5, 67 / 68 | 47 / 60 | 5/5, 0.1 / 0.1 | 0.3 / 2.7 | 30 / 43 |
| Recovery, 0.25 s on | 0/5 | - | 0/5: as the data | - | - |
| Recovery, asked 0.55 s on (with overhead, likely past it) | 5/5, 67 / 100 | 45 / 69 | 5/5, 0.1 / 0.1 | 0.2 / 2.6 | 17 / 44 |
| Recovery, 0.72 s on | 5/5, 67 / 67 | 51 / 64 | 5/5, 0.1 / 0.1 | 0.2 / 1.4 | 37 / 45 |
| Just after G back | 5/5, 67 / 67 | 44 / 64 | 5/5, 0.1 / 0.1 | 0.2 / 2.7 | 29 / 42 |
| Jump, rising | 5/5, 34 / 67 | 33 / 67 | 5/5, 0.1 / 0.1 | 0.2 / 2.5 | 18 / 30 |
| Landing | 5/5, 67 / 67 | 50 / 62 | 5/5, 0.1 / 0.1 | 0.3 / 2.4 | 17 / 32 |
| Sliding | 5/5, 34 / 100 | 31 / 69 | 5/5, 0.1 / 0.1 | 0.2 / 2.6 | 14 / 37 |
| In the air | 5/5, 67 / 67 | 40 / 65 | 5/5, 0.1 / 0.1 | 0.3 / 2.6 | 41 / 48 |
| Disruptor's cast | 4/4, 67 / 67 | 55 / 62 | 4/4, 0.1 / 0.1 | 0.2 / 1.9 | 16 / 28 |
| Melee's recovery | **0/5, lost** | - | 5/5, 0.1 / 0.2 | 0.3 / 1.2 | 33 / 44 |
| Crouched | 5/5, 67 / 100 (10 fps) | 34 / 61 | 5/5, 0.1 / 0.3 | 0.2 / 2.5 | 25 / 48 |
| Low energy, 3 | 5/5, 100 / 100 (10 fps) | 19 / 23 | 5/5, 0.1 / 0.1 | 0.3 / 2.6 | 19 / 47 |
| Full energy | 5/5, 100 / 100 (10 fps) | 51 / 54 | 5/5, 0.1 / 0.1 | 0.3 / 2.5 | 26 / 50 |
| No energy | 0/5 | - | 0/5, the click heard (`kiriko.ability.denied`) | - | - |
| Empty, auto-reloading | **0/5, lost** | - | 5/5, 0.1 / 0.1 | 0.3 / 2.2 | 21 / 49 |
| In an emote | **void:** my `emote 9295` played nothing, so not measured | - | 5/5 with Triumph playing (`emoteClip`, its camera on; both gone after) on b0a9f51, 0.2 / 0.2 | 0.4 / 3.0 | 36 / 46 |
| Overclock's cast, 0.3 s in | 0/3 | - | 0/3: the hard stop kept | - | - |
| In Overclock | 3/3, 100 / 100 (10 fps) | 20 / 52 | 3/3, 0.1 / 0.2 (16d1ca9) | 0.3 / 1.8 | 21 / 23 |

The server shot is still up to about 50 ms after the press: the packet waits for the server (why isn't read: a queue until its next tick, likely). The hit's crack (`snd:charged.hit`) comes back about 33 ms after that, at a frame. **So what she sees and hears of the shot itself is at once; the hit's own sound and marker aren't.**

**Other checks, new build, each read from its raw**
- **Cut reload:** at 0.3 s in, 20 rounds: after the shot 20, reload off (rel -1), meter 0; still 20 at +1.2 s; R again reloads to 45. (The sound's timing that stood here was a bad read: round two.) Past 0.96 s: 45 kept. Empty: reload off, starts again by about 0.77 s after the shot, 45 at about 1.7 s.
- **Left held through a shot** (real rounds): last round 22 ms before the press, none till 627 ms after. Burst, shot, burst with energy earned by real rounds at the zombie: 1 shot, 15 rounds, the meter 0.8 before and 0.7 after.
- **No double shots:** every row's count is one a press; 6 quick taps (0.02 s): 6 shots, 6 clips, meter 0 each. **No stuck state:** `anim` idle and the ammo right after each sequence; a slide and a shot in it: 1 shot.
- **Damage (the server's):** 50 energy 8.27 (a head: 105, less a zombie's 2 armor), 100 energy 14.17 (180), as step 4's.
- **Held, in this first round's build:** a shot every 0.65 s in Overclock; outside it 1 shot with nothing landing, and a second weak one with rounds landing (coordinator's real keys). Since round two: 1 shot a press in all of them.
- **The buffer:** a press late in the recovery, not called by the client (`rg_new_x.txt`, its 0.402 line): the server fired it, the look 100 ms on at 10 fps. The edge: round two.
- **Flick:** `look -90` and the press one frame later, zombies ahead and to the side, 30 fps: with a rotation packet first, 4 of 8 went along the old aim (`rg_final.txt`); with the aim in the press, 8 of 8 on the new target (`rg_final2.txt`). **Old build not measured.**
- **Hard stops:** asleep (a bot Ana's dart): no shot, no ghost, energy kept. Dead, Minecraft mode, the comms key held 0.2 s and 0.5 s (found: at 0.2 s it fired until b0a9f51): nothing. Hacked (a bot Sombra's EMP): it fires, as a weapon; **the request listed hack as a stop, the code and BZ say abilities only: for the owner to say.**
- **Other heroes:** Kiriko's right button after the change: ofuda, no shot (`clicked` returns for anyone but Sojourn). Others not played.
- **The look:** `run/screenshots/rg_new_f1` (not committed; the frame after a press in a reload: the gun kicked, the flash, 20 rounds). The reload's pose goes at once, with no blend back on the view: read in stills only. HUD: the energy reads 100 in that frame and 0 when the sync arrives, a tick and a frame at most; the reload bar the same.

**Found, not fixed:** v173's 0.16 s isn't in the code for the rounds or the reload (a left click right after a burst, R right after firing), and Disruptor's start. R in the recovery is refused (`startReload`), where D's #80 sets v51 and the reload starts as it ends. Hit marker and crack wait for the server, as for every hero. A wrong client call shows a shot that wasn't: round two found three ways, and fixed them.

**What wasn't tried:** a real mouse or a person: every press is the harness's KeyMapping.set + click, so **this isn't the merge gate's hand play, and feel isn't judged.** A second client seeing the shot (its packets are in the code, its reload cut too; one client on this machine). A dedicated server with real latency: a press the server then refuses shows a shot that wasn't (her energy gone in that moment, a stun on the way): not provoked. Attack-speed buffs on the client's lock (0.65 s flat: a press it thinks too early goes the old way; if slowed, it could show one early and the server buffers it). The perk menu open. 60+ fps. An Overwatch side by side.

### Round two (a code critic and the coordinator's real keys, 2026-11-06)
Their pass: [real_keys_664f830](../testing/runs/railgun/real_keys_664f830.md). Each item reproduced first on c8abe69 (`rg2_before*.txt`), replayed on 946b16b (`rg2_final*.txt`, `rg2_after*.txt` from 5b5b6a8 where said). **Harness keys except the flick (OS mouse events, `rk_mouse.ps1`). "100 ms" is `presstrace lag`, a dev delay on her hero payloads both ways, off by default: moves and positional sounds don't wait and the server is in this process, so it isn't real latency.** 30 fps unless said. Screenshots not committed (`run/screenshots/rg2_before_*`, `rg2_after_*`).

| Item | Before (c8abe69) | After (946b16b unless said) |
|---|---|---|
| 1. First Charged Shot, fresh client, no screenshots | frames of **362 + 568 ms**; 252 of 290 thread samples in `EffectArt.load` (`rg2_before_first.txt`) | longest frame **43 ms** (normal 33), no `art_miss` |
| First rounds / reload / Overclock | 121 + 124 / 77 / 62 ms | none over 36 ms |
| First Disruptor | 54, 44, 196 ms | 50, 46 |
| First slide | 49 ms (no art in its samples) | 42: not this cause, not chased |
| 2. Frames after the press: reload / melee / Disruptor's cast | `anim` idle in the first frame, all three (critic right; `sheet_before_melee`: the gun back in one frame) | reload: `idle+reload` 4 frames (the 0.15 s fade), then idle (5b5b6a8, 30 fps). Melee and cast play on to their ends, the shot's layer over them |
| 3. Real flick: burst, click in that frame | 3 of 6 (coordinator's 4 of 10) | **12 of 12** |
| Flick, then 15 ms, click / 30 ms, click | 6 of 6 / not rerun | 6 of 6 / 6 of 6 |
| 4. R, then right inside the ping, 100 ms | at +0.5 s `idle+reload`, no reload on the server, sound running: 5 of 5 | `idle`, 5 of 5; channel stopped as `reload_cut` lands, 0 running at +0.65 s (control uncut: 1.38 s) |
| Left to empty, right as it does, 100 ms | the same, 4 of 4 | `idle`, 0 running, 4 of 4; empty magazine's real reload follows the recovery |
| 5a. Press in the last of the recovery, a click after it, 100 ms | **10 of 12** a shot shown and not fired | 0 of 12 |
| 5b. Shown press in the server's last tick, button down, 100 ms | **1 of 30** her copies doubled | 0 of 48 (plus 16 at wider gaps): 0 |
| 5c. Q, right inside a round trip, 100 ms | **3 of 3** a shot shown and not fired | 0 of 3: nothing shown |
| 7. Press before the recovery's end: last lost / first fired, ms after the shot | 414 / 429; next shot at +603 | 442 / 460; next shot at +652 |
| In Overclock | 387 / 456 | 398 / 467 |
| 8. Two Sojourns reloading, one fires | both channels gone | the other's runs on to its end (control 1.28 s, it 1.28-1.38 s); bots, this client hearing |

**What each was, and the fix**
1. `EffectArt` coloured each mask on the thread that first asked: file read, then pixel by pixel. 32 frames of smoke and 14 of flash in that one shot. Now `EffectArt.prepare` at the first tick she's the hero (select screen or a rejoin): a low-priority worker, then textures at 1.5 ms a frame. **Her art is ready 2.1-2.2 s after the pick.** Pressed 0.8 s after it (`rg2_after_race.txt`): 118 + 241 ms, what wasn't ready yet. **The pick's own frames: 147 + 292 ms; the thread's samples there are mostly the HUD and the model's first draw (`HudDraw.image`, `HudShapes`, `isClosed`, `ClothSim`), `HeroTexture` 22 of 512 in all. So not ours by the samples, but the old build's pick wasn't measured.** Table `SojournFx.ART` is written out from the source: a new effect has to be added (`art_miss` in a trace shows it). An enemy Sojourn's colours, and an ally's seen from another hero, aren't made ahead. **Every other hero has the same lazy load** (not touched).
2. Critic right, as read. The fix is theirs: a layer in `ActionTrack`. **The data's cut:** #233 leaving the stack ends #249; no blend field I can read on it, and add_reload_end is `onFinished` only: the 0.15 s fade is ours (the kit's `FADE`). The gun's own reload clip and `add_reload` still stop at once (`actionAt` removed) while the arms fade: read in stills only. Melee: the shot's tracer now leaves a gun her swing has out of view (`sheet_after_melee`): for a person.
3. `Minecraft.run` polls events, then `runTick` does tasks, ticks and then `mouseHandler.handleAccumulatedMovement()` (decompiled): the click runs before the frame's movement. `clicked` calls it first; it's public, on the client's thread, and later events stay for the frame's own call. With a smooth camera the split changes its dt: not played. **Tick-path presses and held fire don't carry an aim and shouldn't need to:** they fire when the server gets to them, later than the press, with the latest move packet: at most a tick old. The press is a moment; those aren't.
4. Critic right. The server sends her `reload_cut` when a shown shot cuts a reload: behind the reload in the same stream.
5. Critic right on all three, with these fixes: **a press left to the server late in the recovery holds off shown shots for 0.5 s or until its shot arrives (ours: a ping above about 400 ms can still ghost);** Q locks at her press if the ult reads ready; a shown press can't be taken over. **Still possible: a shown shot the server refuses because energy ran out as she pressed, a stun on the way, or attack-speed changes on the flat 0.65 s.**
7. Count fixed (4 looks, 0.15 s). **Against the critic's reading:** the old edge wasn't 0.51-0.55 here: the old build fired at 456 and 509 in Overclock, because the press also takes up to two ticks to reach the check and its recovery ran out at 0.60 s. **Found by this:** a packet's shot counted its first tick at once: next shot +603 against D's 0.65. Now +652 from a press, with the rounds. The window is 0.15 s from when the server sees the press: from the finger it's 0.19-0.21 s.
8. `reloadCut` stops the nearest other reload, heard through a sound listener; two Sojourns side by side can swap.

**6, the held button: a question for the owner.** What firmed up: the plug m_80AF45FB is a while-down state, H: it's what makes rounds stop on release at #527 in this same graph, and Mercy's held right button (006D #608) uses it for a chase that runs while held. #421 begins again as v2447 reaches 0, H (#615). **What isn't in the data:** whether a button already down when #421 begins counts. Left-button rounds resuming after a reload says yes only if that's how the game plays, and no source here states it; m_53C4B6A6 = v842 on both buttons is unread. So it stays M.
- **Kept (least surprising): one press, one shot.** Holding does nothing more, in or out of Overclock. No click at 0.65 s.
- **The data's likely reading:** held, she fires again each time the recovery ends: every 0.65 s in Overclock (a real convenience); outside it, a weak shot from energy that landed while she held (rounds in flight: `srv_shot` then `charged.fire_low` at +605, and 1.25 s with no rounds), or the "not available" click 0.65 s after any shot held that long.
- **Asked: in real Overwatch, with energy, hold the right button: does it fire again 0.65 s on?** A one-line change either way (`rightPressed` in `Railgun.tick`).
- Corrected: earlier lines here said "1 shot" outside Overclock and "out of it, the click": that was only with nothing landing. Now: 1 shot in all of them (`HOLD`, `rg2_final.txt`). **`snd:railgun.reload` at 2287 in the coordinator's line:** one round at 2230 then the reload, with the left button held to 2510: an empty magazine, by the same shape as `HOLD both, 9 rounds` before the fix (`rg2_before.txt`), though that line's ammo isn't logged.

**Harness rows the real pass hadn't:** melee's recovery, Overclock's cast and inside it, an emote (Triumph playing), empty magazine, full table in `rg2_final.txt` (sound 0.1 / look 0.3 / frame about 2.5 ms median, server shot 14-38 median, worst 50); sleep (a bot Ana's dart) and a stun (a bot Sigma's Accretion): nothing fired, energy kept; death: nothing; hack: fires. Comms key, Minecraft mode: nothing. **All harness keys.** Rally's stun didn't land in my try (`stunned (Rally) False`).

**Corrections to the above:** the "cut gone by 0.3-0.5 s" was `bot playing`, which lists a stopped sound for 20 ticks from its start (`SoundEngine.soundDeleteTime`): `presstrace sounding` reads the channel. The "in an emote" row of `rg_new_*`'s `rg_extra` "more" still uses a clip id and plays nothing; the table's row uses `triumph`.

**Not tried:** real ping, a remote server, a second client (others' copies of the shot and cut, by code and bots only). A person's hands, 60+ fps with focus for most rows, Kiriko and the other heroes this round (`clicked` returns for all but Sojourn; `ActionTrack`'s case is hers). The old build at the pick. The perk menu open. Smooth camera. A ping above the 0.5 s hold-off. Overcharged, Dual Thrusters with any of this. An Overwatch side by side.
