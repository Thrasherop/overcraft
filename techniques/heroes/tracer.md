# Tracer (hub)

Everything learned about Tracer, by subject. Issue #71: **Damage, Flanker** (BZ 2026-02-10; her data's tag 1187), the third Damage
hero after [Sojourn](sojourn.md) and [Pharah](pharah.md), and the first checked against a Workshop recording of her own. Up:
[Techniques](../README.md) · the method: [reading_game_data](../game-data/reading_game_data.md),
[workshop_recordings](../game-data/workshop_recordings.md) · built the way [Pharah](pharah.md) was.

## Her notes and sources
- **Public numbers:** [tracer.md](../../tools/reference/tracer.md): BZ 2024-01 to 2026-11 (her last number change BZ 2026-08-11,
  Recall 13 -> 12 s, Temporal Regen in) and the wiki's revision 410824.
- **The data map:** [tracer-map.md](../../tools/reference/tracer-map.md): graphs 0016 / 0024 / 001A / 0034 (+0C1B) / 51EE / 01C7, and the
  table of data against the public numbers and the Workshop. Node-by-node readings in `E:\Overwatch Archive\research\tracer\work_kit\`
  (`kit_tracer_move.md`, `kit_tracer_weapon.md`).
- **Workshop recording (WS 2026-11-11):** `local-assets/raw/workshop/Log-2026-11-11-23-49-15.txt` (gitignored), read by
  [tracer_ws.py](../../tools/workshop/analysis/tracer_ws.py); output [ws_2026-11-11.txt](../testing/runs/tracer/ws_2026-11-11.txt).
- **Clip maps** `tools/clipmaps/tracer*.txt` (3P D748 143 clips, 1P D868 58, pistol DA61 13, 18 emotes, the Heroic intro); **sounds**
  `tools/soundmaps/tracer.txt` (voice, set 0003.05F) and `tracer_sfx.txt` (107 events built).
- **Code:** `dev.overcraft.tracer` (`Tracer` the kit, `TracerStats` every number with its source, `Blink` the destination rule,
  `TracerSounds`), `entity.PulseBombEntity`, `client.ClientTracer` (Blink moved on her own client; Recall's hold),
  `OwMovement.heroRun` (6.0 m/s) / `OwMovement.held`, `mixin.HealOrHarmMixin` (her health-pack stand-in), `HeroWeapon.leftPistol`.

## What her data and the recording taught (2026-11-11)
- **One Workshop session settles what neither source does.** Blink's recharge order (data #54 / #143: one at a time, not reset by
  blinking) shows in the log to the hundredth: charges back at +3.00 s each in sequence. Blink is instant (7.49-7.51 m inside one
  0.016 s tick, the "in use" flag then 0.10 s), level in the air (vertical speed zeroed, then gravity), and on the ground it follows
  the floor (+1.00 up a step, +1.39 up a ledge, -1.40 down a slope).
- **Recall isn't a path in the server's eyes.** The Workshop sees her stand still 0.43 s, then appear at once where she was 3.0 s
  before (within 0.42 m), with the health of then (165 -> 175), and stand there to 0.9 s while gravity keeps adding to the speed she
  had there; she leaves with it (-1.96 -> -9.48 m/s down). The rewind trail Overwatch draws is the client's effect. Built that way.
- **The mover holds her speed, not a passive:** forward / strafe 6.0, back 5.4 (D 0003.075; WS 5.999 / 5.400 / 5.600 diagonally
  back); her air cap stays the default 5.5 (WS: a 6.0 jump slows to 5.92 in the air).
- **Where data and the public text disagree, the data won:** Temporal Regen is 6 -> 3 s (D, = the wiki's 100 %), not BZ's 50 %.
- **Workshop gotcha (this log):** an AI bot (Ashe) shared slot S0; ticks split by the hero on each T line. The log's times are
  rounded to 0.01 s, so a 0.016 s tick reads as 0.02: gravity computed from the times reads 14, from the ticks 17.5.

## Built (commit 960e660, then the play fixes)
Each number in `TracerStats` with its source (D / WS / BZ / W) and H / M / L.
- **Pulse Pistols** (left button): hitscan from her eyes, 2 bullets a shot (one a pistol), 20 shots a second, 5.75 each, 10-20 m
  falloff to x0.3, x2 on the head, 0.02 m; 40 rounds, 2 a shot; spread 0 -> 3.5 degrees over 3 shots, held 0.2 s then back at 4 a
  second; 0.2 s before a new burst after letting go; reload 1 s, the rounds back at 0.7 s; empty, it reloads by itself after the lock.
  A shot's two bullets on one creature are one hit (as D.Va's pellets).
- **Blink** (Shift or the right button: her data's ABILITY 1 and SECONDARY): 3 charges, one back every 3 s, 7.5 m along the move keys
  (forward with none), 0.1 s lockout. Her own client moves her the tick she presses (`Blink.destination`: her box swept 0.1 m at a time,
  up over what's in the way by at most 1.5 m above her start, down the ground as far while on it, level off a deeper ledge, stopped
  at a wall, never inside a block); the server takes the charge and plays the take (blink_1/2/3 by the count in a row, her line on
  the third). No reload or fire cut (D: no action-stack slot).
- **Recall** (E): her last 3 s kept on the server (place, facing, speed from her own steps, health); phased and unseen 0.9 s, held
  still by her client (`OwMovement.held`); 0.44 s in, back to the oldest point facing as then, with her best health of the 3 s (never
  lower than now; a tenth of what it gives back as ult charge, D M); magazine full at 0.72 s; leaves with that moment's speed plus
  0.46 s of gravity; 12 s from its end; not in the first 3 s after spawning (D #254); cleanses as Combat.cleanse (W "lesser cleanse",
  no node: ours).
- **Pulse Bomb** (Q, 1375): 0.15 s cast then 0.2 s before she fires; 15 m/s falling at 30 m/s² (D M), 0.22 m; sticks to the first
  enemy (5 to it; it rides it) or block or enemy barrier; 1 s later 350 to 2 m down to 70 at 4 m, linear; half on herself; no push; no
  ult charge from it; goes off if she's dead.
- **Flanker:** +50 from a health pack. **Ours:** Minecraft has no packs, so a potion of Healing on her (drunk, splashed, lingering, an
  arrow) is one (`HealOrHarmMixin`): +50 and Blink Packs' charge. **Damage role passive:** none since 2026-02-10 (global healing cut).
- **Perks:** Temporal Regen (regeneration from 3 s), Kinetic Reload (12 rounds an enemy hit by quick melee), Blink Packs (+1 charge a
  pack), Quantum Entanglement (Recall: 50 overhealth held 6 s then 25 a second; the magazine to 60, the extra kept 6 s then 20 a
  second, ours).
- **HUD:** Blink's box with its charges in the corner and the next one's recharge as a band (its seconds only with none left: D #172),
  Recall's box, the pistols' 40, Pulse Bomb's meter. **Select screen:** DAMAGE FLANKER, 175 HEALTH, LMB LSHIFT RMB E Q.
  **Models:** 3P / 1P with both pistols (the left one an extra on Weapon_L, posed as the right), the bomb (160BB); 18 emotes, their
  props and stows (each pistol its own slot); the Heroic intro wired through `HeroModels`' `<hero>_intro_clips` (not played).
- **Ours (no data):** the shot and Blink look (spark particles: her effects aren't built), the muzzle offsets, the bomb's spark burst.

## Played (2026-11-11/12, one dev client, `-Xmx3G`, flat survival world `tr1`, harness keys)
Raw stamped output: [runs/tracer/runs.txt](../testing/runs/tracer/runs.txt), the scripts that drove it in
[runs/tracer/scripts](../testing/runs/tracer/scripts/) (`drive.py`, `fight.py`, `t*.txt`). Screenshots in `run/screenshots/` (not
committed; each opened): `tr_select1.png`, `tr_hud1.png`, `t9_rejoin.png`, `t12_back3p.png`, `sheet_t15.png` (front, Bomb Spin, Mic
Drop, Spin, firing 3P and 1P), `t13_end.png`. Expected -> seen:
- **Select** (clicked): Tracer under DAMAGE, DAMAGE FLANKER, 175 HEALTH, five icons; READY: 14 / 14 Minecraft (175), both pistols in
  first person, HUD as above. **Rejoin** (Save and Quit, client restarted, world reopened, three times): the select screen with Tracer
  chosen, READY in.
- **Pistols** on a 100-health zombie (armor 0) 5 m off, held 1.2 s: 100 -> 80.6 (242 OW; 230 expected + a couple of head bullets);
  15 m: 134 OW (149.5 expected at a perfect aim: the spread's misses); 25 m: 51 (69). Ammo 40 -> 0 in 1 s, reload 0.2 s later, rounds
  back at 0.7 s, firing again at 1.0 s (held 3.5 s: two full cycles). R at 30: 40 at 0.7 s.
- **Blink:** standing, Shift: z 0.5 -> 8.0 (7.5). W + the right button: 7.5 on top of the walk. A + Shift: 7.5 to her left. A fourth
  press with none: no move, the empty click. Charges back one at a time about 3 s apart (1 / 2 / 3, each chime). Third in a row: her
  Blink line. **Walls:** a 3-high and a 2-high wall 3.5 m ahead: stopped at z 3.7 (her box against it), not inside. **A 1-block step:**
  up onto it, y -59, z 8.0. **A 1-block drop:** down it, y -61. **A 3-block drop:** level to z 8.0, then fell to -63 unhurt (3 m).
  **In the air** (at a jump's top): level, then fell. **Water** (3 deep): 7.5 m along the bottom. **Ladder**, facing the wall: stayed (the
  wall). **Boat:** seated, Shift does nothing. A 10 m fall after a Blink off a pillar: a plain fall, 14 -> 8.
- **Run:** 0.30 blocks a tick forward (6.0 m/s), 0.27 back (5.4), 0.15 crouching (3.0); 9.1 m in 1.5 s from a stand.
- **Recall:** after 1.5 s of walking and a 4 MC hit: still for 0.44 s, then back at z 0.5 (where she was 3 s before), 10 -> 14, a
  /damage in it did nothing, rounds 26 -> 40 at 0.72 s, cooldown 12.0 at its end. **Into a running jump** (after fixes 2 and 3, T7e):
  back mid-jump at y -59.4, held, then away at 0.24 blocks a tick forward and down. **G 0.2 s in:** Minecraft mode where she stood,
  cooldown started. **After a respawn:** E does nothing for 3 s, then works.
- **Pulse Bomb:** stuck to a zombie 3 m off: 350 to it, 273 to a second 2.55 m from it (0.78), 147 to her 2.4 m off (0.84 x 0.5). Thrown
  level at a zombie 6-8 m off it lands on the ground about 4 m out (the 30 m/s² fall) and sticks there; once, 1.5 m from her, it killed
  her at full health (175 = 350 x 0.5: Overwatch's own rule). A tamed wolf beside a blast: untouched.
- **Perks** (level 3 by the dev command, picked in the LAlt menu with the clicks): Kinetic Reload 20 -> 32 on one melee hit; Quantum
  Entanglement: 60 rounds and 4 absorption (50) after Recall, the absorption 3.2 at 6.3 s, 0 at 8.3; the rounds 45 at 6.3 s, 40 at 8.
  After `/overcraft reset`: Temporal Regen: her regeneration began between 2.8 and 3.3 s after a hit (1 MC a second before it is
  peaceful's own); Blink Packs + Flanker: 1 charge -> 2 and 5 -> 14 health on an Instant Health effect (4 its own, 4 Flanker's, 1
  peaceful's).
- **Hero switch** (H, near spawn: farther it says too far, every hero's rule): Pharah (225 health, 6 rockets), back to Tracer (175, 40,
  her perks kept). **Death and respawn** (/damage, real mobs): back as Tracer, full.
- **Night fights** (normal, midnight, monsters spawning, a wave of 6 zombies, 3 skeletons, a spider and a creeper, her tamed wolf; the
  harness's `fight.py` aims at the nearest each 0.25 s, holds fire, Blinks sideways when one is within 3 m, Recalls below half, throws
  the bomb in 6 m): 70 s, 5 deaths (skeletons, zombies, a spider), the wave cleared; the wolf killed by a zombie (her bullets pass
  allies). Second, the fixed build, ult full: 60 s, 3 deaths, 17 Blinks, 6 Recalls, 3 bombs.

**Found playing (fixed):**
1. **Her self-hit from the bomb was scaled by the difficulty** (easy: 51.8 came off as 38): Minecraft scales an explosion's damage on
   a player; her own hit now goes as her hitscan type (no scaling). Seen fixed: 147 on easy at 2.4 m.
2. **Recall into the air left her standing** (T7, T7b): the speed went out in the same tick as the end, before the sync that lets her
   client go, and the hold zeroed it; and the server's copy of a player's speed is ~0. Now the speed comes from her own recorded steps
   and goes the tick after, before `recall()`.
3. **Still "on the ground" where Recall held her** (T7d): no move runs while held, so the flag was stale and ground friction ate the
   speed it gave her. The hold clears it. Seen fixed: T7e.

**Not shown / not tried:**
- **The real mouse and keys** (owed before the merge): every press here went through the harness's key mappings.
- A second player (her `_3p` sounds, the bomb seen by another, Recall's invisibility from outside), the Nether, lava, a slope fight
  (only a 1-block step and drop), a stair run, a pulse bomb on a moving enemy that it reaches (the throw fell short of a walking zombie),
  the bomb on an enemy barrier, Blink through a doorway or under a 1-block ceiling, Blink while hacked / asleep (gated in code, not
  played), the intro in a real Play of the Game, her emote sounds (none built), the select screen's framing (Pharah's 0.72 kept), the
  1P Blink clips (`add_blink_s0..3`, not wired: no blink animation plays), a critic.
- **Open for an Overwatch side by side:** the bomb's 30 m/s² fall (M: thrown level it lands about 4 m out); Recall's camera (Overwatch
  draws the rewind; here she stands still unseen, then is there); whether Blink follows the view's pitch (W / WS: level).
