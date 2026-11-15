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

**Not shown / not tried:** a critic; an Overwatch side by side; Hammer Time and Anchor Bolts in play (coded); slopes, water, ladders, the
Nether; a second player; the dragon; emotes and the Heroic intro.
