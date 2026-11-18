# Torbjörn (hub)

Everything learned about Torbjörn, by subject. Issue #136: **Damage, Specialist** (his data's tag 116F), the sixth Damage hero, built the
way [Junkrat](junkrat.md) and [Illari](illari.md) were. Up: [Techniques](../README.md) · the method:
[reading_game_data](../game-data/reading_game_data.md) · no Workshop recording of him: his movement is the data's default mover.

## His notes and sources
- **Public numbers:** [torbjorn.md](../../tools/reference/torbjorn.md): BZ 2025-09 to 2026-11 read for his lines (earlier through W's dated
  log), W rev 409440. His last number change is BZ 2026-09-08 (alt spread 4.5 -> 4, Anchor Bolts +25 %). **The Damage role passive is
  gone** (BZ 2026-02-10: its healing cut is global, `Combat.HEAL_CUT`); his sub-role is **Specialist** (as Junkrat's). **His passive
  slot is empty**: the Forge Hammer is a weapon (weapon 2), not a passive; the old Scrap / Armor Packs are long gone.
- **The data map:** [torbjorn-map.md](../../tools/reference/torbjorn-map.md): graphs 1683 (Rivet Gun) / 168C (Forge Hammer) / 16C5 (Deploy
  Turret) with the turret's 1688 / 1689 / 38EA / 1721 (Overload) / 16DA + 16DD + 16DE (Molten Core) / 51F6 (perks) / 38D0 (values), the
  table of data against the public numbers. Node-by-node: `E:\Overwatch Archive\research\torbjorn\work_kit\kit_torbjorn.md`, `kit_turret.md`,
  `kit_overload_ult.md`.
- **Clip maps** `tools/clipmaps/torbjorn*.txt` (3P 177, 1P 101, Rivet Gun 18, hammer 8, turret 49, emotes, the Heroic intro): the weapon
  branches are name prefixes (`hammer_`, `molten_`, `overload_`); **sounds** `tools/soundmaps/torbjorn.txt` (voice) and `torbjorn_sfx.txt`.
- **Code:** `dev.overcraft.torbjorn` (`Torbjorn` the kit, `TorbjornStats` every number with its source, `TorbjornState`, `TorbjornSounds`),
  `entity.RivetEntity` (rivets and pellets), `entity.SlagEntity`, `entity.TurretEntity` (a `Mob`), `LifeweaverPropRenderer.turret`,
  `HeroAnimator.clipPrefix` (his weapon branch's clips), `HeroManager.TORBJORN_LOOK`, `TorbjornMathTest`.

## Where the numbers disagreed (TorbjornStats says which won)
- **Turret fire rate:** W 4 / s (0.256 s); the data reads 0.5 s at level 2 and 0.25 s at level 3 (1689 #173 / #181). W kept (L): W's
  DPS figures are the live game's, and BZ 2026-02-18 fixed "the rate of his ... Turret fire". Worth a Workshop measurement.
- **Molten Core's burn:** BZ / W 190 / s + 100 on armor; the data's statuses read 100 to all and 190 armor-filtered (M on the filter).
  BZ and W kept.
- **Turret cooldown:** W "12 s, 5 s redeploy"; the data 6 s from the cast, 12 s when it goes in combat (fired in 1 s / hit in 3 s), else 6.
  The data's kept (H).
- **Overloaded Turret's length:** the perk text's 5 s is Overload's old length; the data ties it to Overload (6 s). Data kept.
- **Turret targeting:** the data has none (a native AI component reads the target); W's rule (closest enemy in sight, kept until dead or
  out of sight, his rivet's target taken over) is what's built, with `Allies.lockOn` (enemies only: never a bystander or an ally).

## How the turret lives in Minecraft (ours)
- A `Mob` with no AI and `OwnableEntity` owned by him: `Allies` makes it his side's (his allies' heals pass it by: `Combat.heal`; his own
  side's blows don't hurt it) and his enemies' (their area abilities take it). Minecraft's mobs pick targets by kind, and none picks a
  turret: a mob it shoots is turned on it (`setLastHurtByMob` + `setTarget`), so a zombie walks up and claws it, a skeleton shoots it.
- 225 health = 18 Minecraft health; level 3 (Overloaded Turret) adds 150 (30). Not pushed, no fall damage, fire-immune, never despawns,
  saved with its chunk; one that isn't the turret he has out removes itself (`Torbjorn.stillHis`).
- G and death keep it; a hero switch, leaving the world or another dimension removes it (`HeroKit.leavePlay` forGood, `clearWorld`).

## Played (2026-11-14, one dev client, harness keys, fresh flat survival world `tb1`)
Raw stamped output: [runs/torbjorn/runs.txt](../testing/runs/torbjorn/runs.txt), scripts in [runs/torbjorn/scripts](../testing/runs/torbjorn/scripts/)
(`drive.py`, `t1..t8`, `v1..v3`). Screenshots in `run/screenshots/tb_*.png` (not committed; each opened). Targets: NoAI husks, armor 0.
- **Select** (clicked): Torbjörn 6th under DAMAGE, his model, LMB 2 LSHIFT E Q with his icons, "225 HEALTH, 75 ARMOR" (`tb_select1.png`);
  READY: 300 (armor yellow), Shift / E boxes, the Rivet Gun's 18 (`tb_hud0.png`).
- **Rivet Gun** (T1): chest at 8 m 5.6 MC (70) ✓; alt at 3 m 10 MC (125) ✓, ammo -2 ✓; alt at 25 m 0.3 MC (one pellet, x0.3) ✓; held 3 s: 6
  shots ✓; R: ammo back between 0.75 and 1.2 s, done by 2.2 s ✓. **Head at 8 m: no crit** (5.6): bug 1 below.
- **Turret** (T2, T5, V1-V3): thrown and built; a villager and a cow beside a husk untouched, the husk ~43 OW / s ✓; kept its target, took
  the second when the first died ✓; a walking zombie it shot came and clawed it (18 -> 6 MC, `tb_z3.png`) ✓; a skeleton's arrows hurt it ✓;
  /damage "by a villager" refused (his side) ✓; replaced in combat: cooldown 12 ✓; broken by an enemy: cooldown 12 ✓.
- **Hammer** (T3): key 2 out (mode 4), 70 a swing ✓; held 3 s: 4 swings (5 expected: bug 2); key 1 back ✓.
- **Overload** (T3c): 100 overhealth (8 MC absorption) ✓, 6 swings in ~2.3 s (+43 %) ✓, cooldown 10 s from its end ✓.
- **Molten Core** (T4): 10 slag, pools burning 15.2 MC / s (190) on the husk in one ✓, a villager in reach untouched ✓, him in his own pool
  unhurt ✓ (`tb_molten.png`).
- **Perks** (T6, LAlt menu clicks): Pre-Heated: Q started Overload, no cooldown after ✓; Overloaded Turret: turret 30 MC, ~108 OW / s with
  missiles, back to 18 and ~48 / s when Overload ended ✓.
- **Leaving play** (T7, T8): G: Minecraft mode, turret kept; **the overhealth stayed** (bug 3); death: back as Torbjörn, turret kept ✓; a
  switch to Junkrat: turret gone ✓; Save and Leave, Open world: the select screen, READY, the turret gone ✓.

**Found playing (fixed, each reproduced first):**
1. **No headshot from a rivet** (T1b: 5.6 MC at the head): it judged the head by the hit's point, which a projectile's entity hit gives as
   the target's feet. Now `Headshot.crossed` (the shared rule, as the kunai).
2. **A held hammer swung every 0.65 s, not 0.6** (T3b: 4 swings in 3 s): the next swing waited a tick after the last ended. It starts on the
   same tick now.
3. **Overload's overhealth outlived it** (T7a: G, absorption still 8 MC): ending it took off the speed and attack bonus only. Now
   `Combat.endOverhealth` at its end.

**Open (not fixed):**
- The turret model shows its level 3 missile pods at level 2 (its geosets aren't read); no aim layer (the whole model turns to its target);
  no world widget for his turret's health, no reticle widget.
- Rivets, pellets and slag are drawn as Minecraft items (no models in his data: effects only); the turret's shots as spark particles.
- The Interact key's turret recall isn't built (W: the hero option's default is Ability 1 again throwing a new one, which is).
- The turret's missiles (Overloaded Turret) are hitscan hits of 30, not projectiles (ours).
- Lifeweaver's weapon keys look dead (ClientMercy.input returns 0 for a hero not Mercy; Torbjörn's now pass): not checked in play, not touched.

## Fix round 1 (2026-11-14, the critic of 385d464; merged next 0.3.51 first)
Reproduced on the merged build first (R1 in [runs/torbjorn/scripts](../testing/runs/torbjorn/scripts/), harness keys), then fixed and played
again (R2, R3). Before -> after:
- **Molten Core charged his ult** (R1a: 109 -> 773 over ~6.5 s on one husk). Its slag and pools give no charge now (`ultDealt`;
  W Ultimate ability), and `ultActive` covers only the 0.75 s cast so his turret and rivets charge him during it. R2a: 81 -> 99
  (the passive 5/s only); R3: pools still burn 15.2 MC/s (190).
- **The hammer didn't reload the Rivet Gun** (R1c: 16 rivets after 3 s with it out). W: "Automatically reloaded ... Forge Hammer equipped
  for 2 seconds". R2c: 13 at 1.5 s, 18 at 2.3 s.
- **Holding the left button with the hammer out blocked the swap** (R1d: key 1, still the hammer). A swap now cuts the swing. R2d: the
  gun out at once, a rivet fired.
- **A redeploy while the turret fought was allowed** (R1f: thrown, cooldown 11.5). D 16C5 #495's condition (read here): with
  "in combat" (v10467, #509: fired in 1 s or hit in 3 s) set, a throw needs fewer than the one turret out. W agrees ("cannot be redeployed
  while it is in combat"). R2f: refused, cooldown 0, the same turret out. W's 5 s redeploy vs D's 6: D kept.
- **Overloaded Turret's level 3 through his death:** `leavePlay` ends it (his kit doesn't tick while he's dead). A dead player's turret is
  unloaded with his chunk anyway (the servercmd found none), and back at level 2 after the respawn.
- **Hammer cadence:** D 168C #374 sets the swing's StateWait #325 to 0.6 s, with the hit from #345's 0.32 s for 0.2 s (#350): kept 0.6 s,
  the hit moved 0.2 -> 0.32 s. W's 1.43 / s (0.7 s) holds an extra 0.1 s the graph doesn't; its Overload numbers match D's 0.32 / 1.43.
  R2: 6 swings in ~3.6 s held.
- **Pools and damage boosts:** the burn is divided by the boost the hurt would add (W: the damage over time can't be amplified).
- **HUD:** the turret ring is over the cooldown actually running (6 or 12 s, carried in ability3Cooldown).
- **Select screen:** drawn at 0.6, not 0.72 (`r_select_torb2.png`: his whole head in frame).
- Played again, the round-1 fixes: a head rivet 11.2 MC (140) ✓; Overload's overhealth gone on G (8 -> 0 MC) ✓; the cadence above ✓.

Kept, with the source: Anchor Bolts scales the throw's **speed** x1.25 (D 38D0 #69: 10FA x (1 + v60624)), as the data writes it; the
ult's 0.75 s cast (D #370; W 0.592) and 6.5 slag a second (D #382; W 0.144 s held). Not done: the Interact key's recall (no press of
it reaches the server's kit). Seen, not his: after Save and Leave and Open world, the select screen came up with Kiriko chosen and
Torbjörn only underlined; READY played Kiriko.

**Perks played, 2026-11-17** (perks picked in the Left Alt menu with the real mouse; speeds from the harness's velocity, 0.15 s
samples while holding the real W / S):
- **Hammer Time:** the gun 5.5 m/s forward; the hammer out (key 2) 6.6 m/s forward (+20 %) and 5.94 back (x0.9, our back-pedal);
  with Overload (E) on top 7.42 back = 5.5 x 1.5 x 0.9, the two adding as Overwatch's buffs do. ✓
- **Anchor Bolts:** thrown (Shift) at a stone wall 8 m off, it held there 1 m up, but **stood upright against the wall** and,
  with the wall under it removed, **hung in the air** (both reproduced). Fixed: it's a SurfaceStuck now (the face synced), drawn
  with its base on the face and its front swivelling toward its aim in the face's plane; its gun is reckoned out from the face;
  the block behind it gone, it lets go and falls as a grounded one does.

**Not shown / not tried:** a critic; an Overwatch side by side; slopes, water, ladders, the
Nether; a second player; the dragon; emotes and the Heroic intro.
- **Slopes, 2026-11-17:** he walks up a stone staircase at full speed (3 steps, onto a platform). With Anchor Bolts, a turret thrown
  at the stairs bolted sideways onto a stair's half-block riser (stuck, face 2, 0.14 m above the step). Fixed (`onlyAStep`: a face
  no higher than Minecraft's 0.6 step is ground, not a wall); the same throw now lands on the step (y 65.0, on ground), and a stone
  wall still takes it (stuck, face 3).
