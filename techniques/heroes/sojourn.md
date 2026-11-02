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
