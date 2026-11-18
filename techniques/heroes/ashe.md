# Ashe (hub)

Everything learned about Ashe, by subject. Issue #137: **Damage, Sharpshooter** (D tag 1188), the sixth Damage hero, built after
[Illari](illari.md) the way [Cassidy](cassidy.md) and [Junkrat](junkrat.md) were. Up: [Techniques](../README.md) · the method:
[reading_game_data](../game-data/reading_game_data.md) · no Workshop recording of her: her movement is the data's default mover.

## Her notes and sources
- **Public numbers:** [ashe.md](../../tools/reference/ashe.md): BZ 2026-10 / 11 (nothing new for her after the data build), W rev 408621.
- **The data map:** [ashe-map.md](../../tools/reference/ashe-map.md): graphs 14FC (Viper, Take Aim) / 1513 (Coach Gun) / 14FD + 1502 + 150F
  (Dynamite, its stick, the burn) / 1536 + 1537 / 153B / 153A / 17F6 (B.O.B.) / 51E3 (perks) / 35A6, 35B3 (values). Node by node:
  `E:\Overwatch Archive\research\ashe\work_kit\kit_ashe_hero.md`, `kit_ashe_bob_dynamite.md`.
- **Clip maps** `tools/clipmaps/ashe*.txt` (3P, 1P, Viper, Coach Gun, B.O.B., emotes, Heroic); **sounds** `tools/soundmaps/ashe.txt`
  (voice) and `ashe_sfx.txt` (58 events, B.O.B.'s included); emotes `emotes/ashe.json` (20). Models in the pack: `ashe`, `ashe_1p`,
  `ashe_viper` (her held weapon: `HeroModels.weapon`), `ashe_coach_gun`, `ashe_bob`, `projectile_ashe_dynamite`.
- **Code:** `dev.overcraft.ashe` (`Ashe` the kit, `AsheStats` every number with its source, `AsheState`, `AsheSounds`),
  `entity.DynamiteEntity`, `entity.BobEntity` (a `PathfinderMob` and her pet: `OwnableEntity`), `LifeweaverPropRenderer.bob`, Ashe's
  zoom in `ClientAna` (the synced scope in the payload's meter), `AsheMathTest`.

## Built (feature/ashe)
- **The Viper** (left: hip 35 every 0.25 s, spread 0-2°; right held: Take Aim, in 0.2 s / out 0.17 s, then 75 every 0.65 s, no spread,
  zoom to 40°, x0.75 speed): hitscan 0.04 m, x2 head, falloff to x0.3 (20-40 / 35-55 m); 12 rounds; R or empty: a shell at 0.5 s, then
  one each 0.25 s, a press of either button cuts it into its 0.3 s end (the shells in stay).
- **Coach Gun** (Shift): 0.55 s cast, the blast at 0.165 s: 15 hitscan pellets of 6 in the data's rings within 10° (the data's 20° as the
  cone's width, M), no crit; enemies hit pushed 20 m/s along her aim and 5 up (all of it to 8 m, none from 15 m); she's launched 18 m/s
  back (+0.1 up), lands soft; 10 s. **Double-Barreled**'s window (coded).
- **Dynamite** (E): thrown 0.15 s in at 25 m/s, gravity 8; lands and lights (2 s), bounces off walls (ours), drops off creatures; her
  Viper shot sets it off; 50 -> 20 over 1.5-5 m to enemies and bystanders in sight, half on her; a burn of 100 over 5 s (hers 50) in
  0.2 s steps, taken off by a phase; 12 s from 0.55 s in. **Remote Detonator** (E again, 0.5 s) and **Airburst** (x1.4 unlanded, +6
  rounds) coded.
- **B.O.B.** (Q, 2450): at her feet facing her way at the cast; 0.5 s drop, then 4 s charge at 15 m/s; a wall (rays 0.65 m ahead at 0.1 /
  0.75 m) or being stuck stops him; an enemy within 2 m and 90° ahead: the uppercut on all within 4 m / 90° that areas hit (enemies and
  bystanders): 120, held 1.5 s (0.5 s airborne) then thrown up 15 m/s (12 airborne, L which is which); 0.8 s on he stands and fires 10 s,
  17 a shot 8 a second, 1.45° spread, 40 m, at an enemy only (Allies.lockOn), preferring the one she shot in the last 3 s (W; ours the
  3 s); then 0.5 s and gone. 1150 health (1000 + the Tank role's 150, W); her side's damage doesn't reach him. **Mobs** fight him once he
  hits them (ours: `BobEntity.provoke` makes him their last attacker and target). **Cleanup:** he stays through her G and death (W: he
  fights on), goes on a hero switch, leaving, or when she isn't in his world. Not saved.
- **Sharpshooter:** crit damage x 0.0075 s off Coach Gun's cooldown (and the held one during Double-Barreled).
- **Perks** (coded, **not played**): Remote Detonator, Double-Barreled, Viper's Sting (25 over 1 s on the second scoped hit in a row on
  one target within 3 s; a scoped miss breaks it), Airburst.
- **HUD:** Coach Gun and Dynamite boxes (lit for Double-Barreled's window / a stick out with Remote Detonator), the Viper's 12.
  **Select screen:** DAMAGE SHARPSHOOTER, 250 HEALTH, LMB RMB LSHIFT E Q with her icons.

## Played (2026-11-14, one dev client, harness keys, flat world `ashe1`, a stone platform at y 150)
Raw stamped output: [runs/ashe/runs.txt](../testing/runs/ashe/runs.txt), scripts in [runs/ashe/scripts](../testing/runs/ashe/scripts/)
(`drive.py`, `fight.py`, `a1..a10`, `n1`, `w1`, `v1`, `v3`). Screenshots `run/screenshots/ashe_*.png` (not committed; each opened).
Targets NoAI husks of 100 MC (1250 OW). Expected -> seen:
- **Select** (new world: arrow keys + Enter; rejoin): Ashe 6th under DAMAGE, her model, SHARPSHOOTER, 250, her icons
  (`ashe_select_kit.png`); rejoin opened the select screen with her chosen (`ashe_rejoin.png`). HUD (`ashe_hud0.png`, `ashe_scoped.png`).
- **Viper** (A1): hip 2.8 MC (35) ✓; held 2 s: 8 shots, 22.4 MC ✓; scoped 6 MC (75) ✓, two 0.65 s apart ✓; scoped head 12 MC (150) ✓;
  R from 5: a shell at 0.5 s then every 0.25 s, 12 at ~2.5 s ✓; from empty, a left press at 1.2 s stopped it with 6 in ✓. Scoped, the
  husk 10 m off drew ~160 px wide against ~90 unscoped by the 70° view: the 40° zoom ✓.
- **Coach Gun** (A2, A3, A8, A10): a walking zombie 3 m off pushed 3.1 m and took 48.5 OW, she flew 8 m back ✓; looking down, up 4.6 m in
  1.2 s, landed 20 -> 20 ✓. **Bug, fixed:** at 4 m a husk took 4 pellets (24) with the 20° as a half-angle; as the width 9 (54) (A8a).
- **Dynamite** (A3, A10): landed 4.3 m ahead, off 2 s later: husks 1.75 m and 3.2 m off took 150 and 138.5 in all (blast + 100 burn) ✓; she,
  4 m off, took 63 (half blast + 50 burn, D #178 / #239) ✓; shot 0.7 s after landing it went off at once ✓; cooldown 11.8 at 0.6 s ✓.
- **B.O.B.** (A4-A8, W1, V3): charged 24 m in ~1.6 s (15 m/s) ✓; stopped at a wall (8.3 of 9) and never fired through it ✓; fired at the
  husk 1.36 MC (17) a shot ✓; a villager and a cow off his line untouched ✓; walking zombies crowded and hit him (92 -> 65 MC) and he
  killed one, the kill feed with B.O.B.'s icon ✓ (`ashe_bob_mobs.png`); gone 15 s after the cast ✓. **Bugs, fixed:** (1) no knock-up on
  a walking zombie (A5: y 150 throughout): the root zeroed the launch; root first, then launch: up 2.97 m (A8b) ✓. (2) His line stopped
  at her allies (read after N1): V3, a villager and her wolf between him and a husk: husk 200 -> 153.6, both untouched ✓.
- **G** with him out (A6): he stayed and kept firing (husk 200 -> 168.7) ✓; G back ✓. **Switch** to Junkrat in the spawn area with him
  and a stick out (A7): both gone, Junkrat 5 frags; back to Ashe 12 ✓. **Death** (/kill, fight deaths): back as Ashe ✓.
- **Water** (W1): hip shot from inside a pool 2.73 MC ✓; Coach Gun there moved her little (the pool's walls).
- **Night fight** (N1: normal, midnight, 3 zombies, 2 skeletons, a spider, a creeper, a villager, her wolf; `fight.py` 75 s): B.O.B. at
  once among 5, 3 sticks, 2 Coach Guns, 1 death, respawned as Ashe. Her wolf ended at 1 HP: not traced (her areas skip allies by rule;
  it fought the zombies).

**Not fixed (open):**
- No ADS pose: the view model stays at the hip when scoped (`ashe_scoped2.png`); her 1P `ads_*` clips and the 3P scoped stance aren't wired
  (the animator plays `fire`, `fire_ads`, `ads_in`, `ads_out`, `reload_*`, `coach_gun`, `dynamite_throw`, `ult_cast` by name).
- The Viper isn't in her hands on the select screen (`ashe_select_kit.png`); the Coach Gun model isn't drawn in the cast (not mounted).
- B.O.B.'s charge clip is one of eight unset run clips (L), his drop clip a guess (L); no ammo-free reload widget at the reticle.
- Mobs don't seek B.O.B. out unless he hits them (Minecraft's target goals look for players); vanilla particles for her effects.

**Not shown / not tried:** the real mouse and keys (owed before the merge); the four perks in play; slopes, lava, ladders, a boat, the
Nether, a second player (an enemy Ashe's B.O.B., friendly fire), the dragon; her emotes and intro; Double-Barreled's window; Airburst's
radius; a critic and an Overwatch side by side.
