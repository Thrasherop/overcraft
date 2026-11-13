# Cassidy (hub)

Everything learned about Cassidy, by subject. Issue #72: **Damage, Sharpshooter** (BZ 2026-02-10; his data's tag 1188), the fourth
Damage hero after [Sojourn](sojourn.md), [Pharah](pharah.md) and [Tracer](tracer.md), built the way Tracer was. Up:
[Techniques](../README.md) · the method: [reading_game_data](../game-data/reading_game_data.md) · no Workshop recording of him yet
(the owner will be asked for one): his movement is the data's default mover.

## His notes and sources
- **Public numbers:** [cassidy.md](../../tools/reference/cassidy.md): BZ 2024-01 to 2026-11 (his last number change BZ 2026-08-11,
  Giddy Up), the wiki's revision 409375.
- **The data map:** [cassidy-map.md](../../tools/reference/cassidy-map.md): graphs 01D2 (Peacekeeper, Fan the Hammer, Silver Bullet) /
  01F0 (Combat Roll) / 01E9 (Flashbang) / 022F + 01EE (Deadeye) / 51E5 (perks) / 32C4 (values), and the table of data against the
  public numbers. Node-by-node reading: `E:\Overwatch Archive\research\cassidy\work_kit\kit_cassidy.md`.
- **The grenade is Flashbang**, not Magnetic Grenade: BZ 2024-06-20 turned Magnetic Grenade into Flashbang (a 1.2 s hinder, not the
  old stun), every live note since names Flashbang, and his data's loadout is 0060 (graph 01E9); Magnetic Grenade's 0335 is in no
  loadout of his (only the hinder's damage label and Stadium's Hot Potato).
- **Clip maps** `tools/clipmaps/cassidy*.txt` (3P 161, 1P 74, Peacekeeper 5 + Quick Draw, 16 emotes, the Heroic intro); **sounds**
  `tools/soundmaps/cassidy.txt` (voice, 33 events) and `cassidy_sfx.txt` (25 rules).
- **Code:** `dev.overcraft.cassidy` (`Cassidy` the kit, `CassidyStats` every number with its source, `CassidyState`, `Hinder` the
  Flashbang's status, `CassidySounds`), `entity.FlashbangEntity`, `net.DeadeyePayload` + `client.hud.DeadeyeHud` (the locks and
  skulls), `client.ClientCassidy` (the roll moved on his own client, as Brigitte's bash), `ActionTrack`'s held Deadeye stance.

## Built (commit 153101c, then the play fixes)
Each number in `CassidyStats` with its source (D / BZ / W) and H / M / L.
- **Peacekeeper** (left button): hitscan 0.04 m, 70, x2 head, 25-35 m to x0.3, 0.5 s a shot (held: automatic), 6 rounds, reload 1.5 s
  with the rounds at 0.8 s, by itself when empty.
- **Fan the Hammer** (right button): the rest of the cylinder from the press (or held), 0.12 s apart, 50 each, 20-30 m to x0.3, the
  first exact then up to 6 degrees, no crit; 0.3 s before Peacekeeper after (D M).
- **Combat Roll** (Shift): moved by his own client the tick he presses, 15 m/s for 0.4 s (6 m), x1.3 off the ground, gravity on, out
  at 5.5 m/s at most, along the move keys (facing with none); 50 % less damage the whole roll; the cylinder full 0.2 s in; 5 s from its
  end. Its clip roll_s0..s3 by the leading key (ours: which slot is which direction is M). **Ours:** it steps up only Minecraft's
  0.6 m (a full block stops it, stairs don't).
- **Flashbang** (E): 0.1 s cast, 30 m/s falling at 20 m/s², 0.2 m; off at the first enemy or block or after 7 m; 75 to every enemy
  within 3 m in sight, no falloff; 1.2 s hinder: half speed (an attribute slow, so mobs too); 12 s from the cast's end. **Ours:**
  of the hinder's "no movement abilities" only his own roll asks it; the crouch isn't blocked.
- **Deadeye** (Q, 1800): 0.2 s cast, then 7 s; each enemy in a 105-degree cone, 200 m, in sight gains 150 a second for its own first
  2 s of lock, then 300 (paused out of sight); he is 70 % slower and takes 40 % less, fading to 35 % / 0 in 10 steps of 0.15 s after 2
  s; the left button fires: the most charged first (ours: the data's order is the tracker's), one every 0.1 s, each 0.066 s after its
  turn, its charge as the damage, 1 round each; the right button cancels; out of time, no shot. His cylinder full and Silver Bullet
  back at the cast. Markers: a red ring closing in on each target, a skull when the shot kills (ours, the HUD's own shapes).
- **Sharpshooter:** crit damage x 0.0075 s off the roll's cooldown. **Damage role passive:** none since 2026-02-10.
- **Perks:** Bang Bang (a second Flashbang at 25 m/s that goes 10.25 m, both 45), Giddy Up (+60 % from the roll's end, 10 steps over
  1.5 s), Rollin' Round-Up (15 for each round the roll reloads), Silver Bullet (in Fan the Hammer's place: hold the right button, the
  left fires a 70 piercing shot with crit, then 50 bleed over 1.5 s; 8 s; reset by the roll and Deadeye; its box where Lucio's third
  is, lit while aimed).
- **HUD:** Combat Roll and Flashbang in Kiriko's two places, Peacekeeper's 6. **Select screen:** DAMAGE SHARPSHOOTER, 250 HEALTH,
  LMB RMB LSHIFT E Q. **Models:** 3P EFA3, 1P F082, Peacekeeper EECF on Weapon_R, Flashbang 118EB; 16 emotes and their props.

## Played (2026-11-12, one dev client, `-Xmx3G`, flat survival world `cas1`, harness keys)
Raw stamped output: [runs/cassidy/runs.txt](../testing/runs/cassidy/runs.txt), the scripts in
[runs/cassidy/scripts](../testing/runs/cassidy/scripts/) (`drive.py`, `fight.py`, `t1..t13.txt`). Screenshots in `run/screenshots/`
(not committed; each opened): `cas_select2.png`, `cas_hud1.png`, `cas_t3_flash.png`, `cas_t4_lock2.png`, `cas_rejoin.png`,
`cas_pause.png`, `cas_t7_deadeye_front.png`, `cas_t7_deadeye_1p.png`, `cas_t12_deadeye_1p.png`. Targets are NoAI husks (zombies
burn at noon: the first T1 read was off by the burning). Expected -> seen:
- **Select** (clicked, new world): Cassidy 4th under DAMAGE, DAMAGE SHARPSHOOTER, 250 HEALTH, five icons; READY: 20 / 20 (250), the
  revolver in first person, the HUD as above. **Rejoin** (Save and Leave, Quit Game, client restarted, world reopened, three times):
  the select screen with Cassidy chosen, his perks kept.
- **Peacekeeper** (T1, husk 100 MC, armor 0): 3 shots in 1.2 s held, 16.8 off (210); head 11.2 (140); 30 m 3.77 (46.6: the eye 29.7 m
  off); 40 m 1.68 (21). **Fan:** 6 rounds at 10 m, 4 hit (16 = 200), the spread's misses; empty -> reload by itself. **Reload:** rounds
  at 0.8 s, done 1.5 s.
- **Combat Roll** (T2): standing 6.16 m in 0.4 s; with A held to his left; in the air 8.0 m; 3 -> 6 rounds mid-roll; `/damage 5`
  mid-roll took 2.5, after it 5; cooldown 5 s from its end. **Terrain** (T8): a 3-high wall 3.5 m ahead: stopped at z 3.7; a 1-block
  step: stopped (ours); oak stairs: up to y -58; off a 3-block drop: down, no damage; into 3-deep water: along it.
- **Flashbang** (T3): two husks within 3 m 6.0 (75) each, one 4.5 m off untouched; a husk's speed 0.23 -> 0.115, back at 1.3 s; aimed
  at a husk 12 m off: off at 7 m, the husk untouched; under water at a husk: 6.0.
- **Deadeye** (T4): a 250 husk and a 500 husk 10 m ahead, a third behind a wall: the skull on the 250 one; `/damage 5` at 1 s took 3
  (40 %); fired ~2.4 s in: the 250 one dead, the 500 one 36 off (450), the walled one untouched; his speed 0.1 -> 0.03. Cancel (right
  button): no shot, charge spent. Timed out at 7.2 s: no shot. Its lines (T13): he hears "It's high noon" (his own), Deadeye's start,
  loop, shot and end sounds.
- **Perks** (level 3 by the dev command, picked in the LAlt menu with the clicks): Giddy Up 8.8 -> 7.8 -> 6.8 -> 5.5 m/s over 1.5 s;
  Silver Bullet through two husks in a line 9.6 off each (70 + 50 bleed); Bang Bang 7.2 on the near husk (both blasts), 3.6 on one
  9.5 m off; Rollin' Round-Up 10 -> 14.8 (4 rounds x 15).
- **G** in Deadeye: Minecraft mode, speed back to 0.1, markers gone, charge spent; G mid-roll: the roll cut, cooldown started.
  **Hero switch** (H at spawn): Tracer (175, 40 rounds), back to Cassidy (250, 6). **Death** (/kill, respawn): Cassidy, full.
- **Night fights** (normal, midnight, monsters spawning; 6 zombies, 3 skeletons, a spider, a creeper and a tamed wolf; `fight.py`
  aims at the nearest each 0.25 s, holds fire, fans within 4 m, rolls within 2.5 m, Flashbangs within 6 m, Deadeye with two or more
  within 25 m): 70 s, no deaths, 1 Deadeye on 11; **then the cylinder stuck at 0** (fix 3). Second, fixed: 60 s, 2 deaths (a creeper,
  zombies), 5 rolls, 14 fans, 7 Flashbangs, 2 Deadeyes, the cylinder cycling.
- **Sounds** (T12, `heard`): Peacekeeper's fire, reload, the roll and his grunt, the fan, Flashbang's throw, "Hold up, now" and its bang.

**Found playing (fixed, each reproduced first):**
1. **Sharpshooter took far too much off** (T2f: a 140 headshot zeroed a 2.35 s cooldown): Combat.damage answers in Overwatch points and
   the kit converted them again. Fixed (T7a): 4.25 -> 2.8 over 0.36 s (1.05 off).
2. **Silver Bullet's bleed was 53, not 50** (T5b: 9.87 off): 8 steps at the 0.2 s rate. Now 50 shared by its steps (T7b: 9.6).
3. **The cylinder stuck at 0** (T9, reproduced in T10: fire held 4 s, no reload): an action's cut sets HeroState.reloadHeld, which every
   other kit counts down and his didn't. Fixed (T12a): the reload again after a Flashbang mid-reload; T14's fight cycled.
4. **He heard two Deadeye lines** (T12c: "Step right up" and "It's high noon"): HeroSounds.ultLine gives the hero the allies' line;
   his data has his own copy (key F1AC, 183B). Fixed (T13): only his own.
5. **First-person Deadeye filled the view with his left arm and his hat's brim** (`cas_t7_deadeye_1p.png`, the 1P clip 1373 held):
   the first-person Deadeye clips are left out for now (`cas_t12_deadeye_1p.png`: the revolver, the markers); the third-person
   stance is held through the channel (ActionTrack `holdEnd`).

**Not shown / not tried:**
- **The real mouse and keys** (owed before the merge): every press here went through the harness's key mappings.
- A critic; an Overwatch side by side (roll distance and speed curve, Deadeye's stance and its first-person view, Fan the Hammer's
  spread and rhythm, Flashbang's arc).
- The front view of Deadeye's held stance looked close to his idle (`cas_t7_deadeye_front.png`): which frame the stance holds is open.
- A second player (his `_3p` sounds, the enemy's "It's high noon", the hinder on a hero: its slow, and that it blocks only his roll),
  the Nether, lava, ladders, a boat, Deadeye on a moving target, the roll's direction clips checked by eye (roll_s1..s3), the intro in a
  real Play of the Game, his emotes played, the select screen's framing (0.72 kept), Silver Bullet's aim zoom (none built).
- Sound events: every other hero's count in the pack could not be compared against a clean baseline (the pack's older `sounds.json`
  copies are partial); the data agent's check: only `cassidy.*` events added.

## Critic b79a4d1 and its fixes (2026-11-13)
The critic's report: [runs/cassidy/critic_b79a4d1.md](../testing/runs/cassidy/critic_b79a4d1.md) (raw: `critic_runs_b79a4d1.txt`). Each
reproduced on the old build first (the fixes stashed), then played on the fix: one dev client, fresh flat worlds `fix_old` / `fix_new`,
harness keys (not the real mouse and keys). Raw stamped log: [runs/cassidy/fixer_b79a4d1_runs.txt](../testing/runs/cassidy/fixer_b79a4d1_runs.txt)
(labels `old_*`, `new_*`), scripts `f1..f5.txt`, `f2b.txt`. Screenshots (not committed, each opened): `FX_old_f8_skull.png`,
`FX_old_f9_skull.png`, `FX_f8_skull.png`, `FX_f9_skull.png`. The synced `mode` now carries two counts above its flags (bits 8-15 seen,
16-23 cut): read its low byte for the flags. Expected -> old -> fixed:
1. **Combat Roll: client and server disagreed** (high). Now one authority, Tracer's Blink's way: his client rolls only when the last sync
   says the server will take it **and** the server has seen every E, Q and roll it sent (`Cassidy.pending`, `clientMay`); the server rolls
   only for a roll his client sent (its direction in the same packet: no stale clip), and one it can't take (a hinder, hack or sleep
   landing in the round trip) it counts as cut, and his client stops. F1, lag 150, a shot, E, Shift 0.17 s later -> no move, nothing
   spent. Old: z 0.5 -> 6.66, the server's roll cooldown 0, ammo 5 (no reload). Fixed: z 0.5, the throw went (cooldown 11.7), ammo 5.
   F2, lag 150, taps every ~0.17 s as the cooldown ends -> every server roll with its move. Old (critic C7): a server roll from a tap the
   client refused. Fixed: refused taps sent nothing (seen count unchanged); the one roll moved 6.16 m and the server's count and cooldown
   followed. F13 control: E, Shift 0.6 s later: no lag, a roll (cooldown 4.3 s after); **under lag 150, refused** (his client still sees
   the cast: it refuses for one round trip longer than the server would; a lost press, not a disagreement: ours, open).
2. **Deadeye fired at nothing** (F4, sealed box, left button 1 s in) -> the press does nothing. Old: mode 1 -> 0, the ult spent. Fixed:
   still channelling (mode low byte 1), times out unfired at 7 s. Q again fires it (W): F6b, old nothing (timed out, the husk alive);
   fixed: fired, the husk 100 -> 65.2.
3. **The skull ignored armor** (F8, the critic's C4c: a 60 MC zombie in full iron, fire ~3.85 s in) -> no skull unless it dies. The
   share is now `Combat.projected` (the same path as Combat.damage: the hurt's scaling, armor and toughness, Resistance, protection,
   without their side effects) over its health. Old: skulls on it (and on far-off slimes), left at 1.94. Fixed: the marker, no skull,
   left at 1.94 (`FX_f8_skull.png`); F9, longer: the skull, then dead (`FX_f9_skull.png`).
4. **Jump, crouch, quick melee in Deadeye** (F5b) -> none. Old: y -60 -> -59.12, the husk 100 -> 96.8. Fixed: y -60, husk 100;
   crouch held: not crouching in Deadeye, crouching after it (control).
5. **Dragon parts quartered** -> `Combat.damagePart` in his shots, Deadeye through the head. F11 (fixed only; old code-read): a NoAI
   dragon 200 -> 144.8 from one Deadeye shot (~690 OW: the whole charge; through the body ~15).
6. Lows:
   - **Hack ends Deadeye** (F7, `cut hack 2` 1 s in): old mode 9 (going on, hacked); fixed: ended unfired.
   - **Hinder cuts a roll** (F3, `cut hinder` 0.1 s in; reference cassidy.md: "interrupts and blocks movement abilities"): old the full
     6.2 m; fixed stopped at 3.2 m, cooldown from the cut (ours), his client stopped by the cut count.
   - **"Locks on passive animals"**: not so. In the critic's flat world they were **slimes** (`ents Slime`: dozens 50+ m off; flat
     worlds spawn them, `Enemy`), which `Allies.areaHits` rightly takes; cows aren't locked. What was wrong: six rounds could go to far
     slimes locked as long as the zombie in front. Now equal charges go nearest first (ours). Our runs: `/gamerule spawn_monsters false`.
   - **Flashbang's range** (F10: a husk whose near face is 11.15 m out): old it burst at 7.5 m (z 8.5) and hit it (100 -> 94); fixed: the
     leg that crosses 7 m is cut to end there (`Cassidy.rangeCrossing`), the husk untouched.
   - **Deadeye's fade** now steps as Giddy Up does (full at 2 s, the first step 0.15 s on, none from 3.5 s); W says 1.4 s.
   - **Two Cassidys** (ours, code only: one client): Deadeye's slow multiplies (ADD_MULTIPLIED_TOTAL: x0.3 x0.5 with a hinder, not
     clamped to a stop); each Cassidy's bleed runs on its own (keyed by target and owner), his own second restarts his.
   - **Flashbang's damage** is `Combat.blast` (not scaled by the difficulty against players). **HINDER_TIME's citation**: BZ 2025-11-11.
7. **Tests** (CassidyMathTest): `damageTaken` (the roll's 50 %: the critic's first mutation), `deadeyeTargets` (`lockable` needs sight,
   `canFire`, `skullShare`), `flashbang` (`flashTakes` needs sight and no barrier; `rangeCrossing`), `rollAuthority` (`clientMay`,
   `pending`, `counts`), and the fade against Giddy Up's steps. They test the rules the code calls; a mutation that drops the
   `Combat.inSight` call at its call site would still pass. No offline test for `Combat.projected` (it needs a live entity).

**Not tried here:** the real mouse and keys; a second player (two Cassidys' slows and bleeds, a hinder or hack from a real enemy landing
mid-round-trip, the enemy's view of the cut roll); a hack landing mid-roll (left going: no source read on it); Peacekeeper headshots on
the dragon's head by hand (Deadeye only); a Shift press dropped at a hero switch (his client retakes the count after 2 s stuck); an
Overwatch side by side.
