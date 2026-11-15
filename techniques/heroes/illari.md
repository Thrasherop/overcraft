# Illari (hub)

Everything learned about Illari, by subject. Issue #135: **Support, Survivor** (her data's tag 1173), built after
[Junkrat](junkrat.md), the same way. Up: [Techniques](../README.md) · the method: [reading_game_data](../game-data/reading_game_data.md) ·
no Workshop recording of her yet: her movement is the data's default mover, her flight Valkyrie's mover at her speed (ours).

## Her notes and sources
- **Public numbers:** [illari.md](../../tools/reference/illari.md): BZ 2023-08 to 2026-11 (her last change BZ 2026-11-03: charge
  0.65 -> 0.575 s, pylon 40 -> 50), W rev 408912. **Her kit text matches the data's:** the secondary is the healing beam (it shipped with
  her; the "charged healing shot" was a pre-release prototype). **Sunstruck is not a passive:** it is Captive Sun's mark. The Support role
  passive was removed (BZ 2026-02-10); her passive slot is Survivor's.
- **The data map:** [illari-map.md](../../tools/reference/illari-map.md): graphs 3A65 (rifle and beam) / 3B38 (Outburst) / 3DF4 + 59EA /
  3DF5 / 3DF6 (pylon) / 3CAB + 3CB1 + 3DFF (Captive Sun) / 51F3 (perks) / 4659 (values), Survivor 01C7 #2564; the table of data against
  the public numbers. Node-by-node reading: `E:\Overwatch Archive\research\illari\work_kit\kit_illari.md`.
- **Clip maps** `tools/clipmaps/illari*.txt` (3P 188, 1P 69, pylon 8, Heroic intro); **sounds** `tools/soundmaps/illari.txt` (voice) and
  `illari_sfx.txt` (71 events). One model (117.118) for both views; the rifle is part of the body mesh (no weapon file).
- **Code:** `dev.overcraft.illari` (`Illari` the kit, `IllariStats` every number with its source, `IllariState`, `IllariSounds`),
  `entity.HealingPylonEntity`, `entity.CaptiveSunEntity`, `OwMovement.illariFlight` (her client flies her), `LifeweaverPropRenderer.pylon`,
  `IllariMathTest`.

## Built (feature/illari)
Each number in `IllariStats` with its source (D / BZ / W) and H / M / L.
- **Solar Rifle** (left): charges by itself in 0.575 s from 0.25 s after a shot; a press fires from 40 % charge, held it fires at full
  (ours, M); 25 at 40 % to 75 at full on the data's curve (0.7 -> 50); hitscan 0.04 m, falloff 30-50 m to x0.3, x1.5 head; 14 shots,
  reload 1.5 s with the ammo back at 1.08 s; 0.15 s between shots, 0.2 s after the beam.
- **Healing beam** (right): the first ally along her aim in 20 m (0.25 m round), 115 / s; energy 100 draining over 3 s, back at 35 / s
  from 0.6 s after; emptied, back from 1.0 s later and locked until full (D). Drawn as gold dust (ours).
- **Healing Pylon** (E; with one out, E places a new one: fix round 1): 0.6 s cast, thrown at 0.1 s at 25 m/s; sets on a floor or wall; built 1.25 s; every
  0.8 s a heal shot (55 m/s: a delayed heal, drawn as a dust line, ours) of 50 (25 on her) to the ally in 15 m and sight with the lowest
  health share; 75 + 50 shields (shields back 3 s after a hit at 30 / s: ours, L); cooldown 7 s once gone, 14 s if hit in its last 3 s
  or taken back unbuilt. Enemies break it: their hits, their projectiles that strike it, and **ours:** a hostile mob within 1 m of it
  claws it once a second for its attack damage (Minecraft's mobs can only target creatures).
- **Outburst** (Shift): 0.8 s cast, the burst at 0.2 s: 25 to enemies in 8 m and sight, push 18 away / 5 up (x1 to 5.6 m, x0.3 at 8 m);
  her launch 6 m/s up on a standing tap, 13.5 m/s moving or with Jump held, along (x, 0.6, z) / (x, 2, z); lands soft; 8 s.
- **Survivor:** Outburst starts the regeneration at once (Brigitte's way, `lastDamagedTick`).
- **Captive Sun** (Q, 2975): 12 m/s up; ammo and energy full 0.6 s in; flight from 0.6 s to 5.2 s at 5.5 x 1.45 m/s, knockback taken
  halved; the ball from 1.2 s on the left button or by itself at 4 s: 30 m/s, 5 direct + 50 to enemies in 10 m (BZ; the data's 8 m kept
  out, L) and sight, each marked 6 s (BZ; data 7, L), slowed 40 % for 1 s easing to none; 90 damage on a marked one (from anyone) sets
  Sunstruck off 0.8 s later: 160 inside 1 m to 120 at 5 m; a marked one dying sets it off too (W). Marks chain.
- **Perks** (coded, **not played**): Rapid Construction (build x0.25, cooldowns 12 / 6), Summer Solstice (+3 s flight, +20 % flight and
  attack speed in it), Sunburn (50 over 3 s), Solar Flare (left during the beam: 100 to allies in a 40 degree cone, 20 m, 10 s).
- **HUD:** Outburst and Pylon boxes (Pylon lit while out), the beam's panel with its energy bar, the rifle's 14. **Select screen:** SUPPORT
  SURVIVOR, 250 HEALTH, LMB RMB LSHIFT E Q with her icons and portrait.

## Played (2026-11-14, one dev client, harness keys, fresh flat survival worlds `il1` / `il2`)
Raw stamped output: [runs/illari/runs.txt](../testing/runs/illari/runs.txt), scripts in [runs/illari/scripts](../testing/runs/illari/scripts/)
(`drive.py`, `fight.py`, `i1..i5`, `s2`, `v1`). Screenshots in `run/screenshots/illari_*.png` (not committed; each opened). Targets: NoAI
husks 100 MC = 1250 OW, armor 0; a villager as the ally. Expected -> seen:
- **Select** (clicked, new world, twice): Illari 9th under SUPPORT, her model and icons (`illari_select_kit.png`); READY: 250, the HUD
  (`illari_fire.png`). **After a death** and **after rejoining** (a crash): the select screen with Illari chosen, READY back into her.
- **Rifle** (I1): full charge at 10 m 6 MC (75) ✓; head 9 MC (112.5) ✓; held 4 s: 5 shots, 30 MC, ammo 10 -> 5 ✓ (0.825 s apart); R:
  ammo back between 0.53 and 0.9 of the reload, done at 1.5 s ✓. The 40 % press (an uncharged shot) wasn't isolated: the script's taps
  landed after full charge.
- **Beam** (I2): villager 5 -> 14.2 MC in 1 s (115) ✓, energy 0.667 ✓; held 4 s: empty at ~3 s, locked (mode 32), full again ~4 s after
  ✓; on a husk: nothing ✓.
- **Pylon** (I2, I3): villager 2 -> 6 -> 14 -> 18 MC, 4 MC (50) a shot ✓; a NoAI husk beside it broke it in < 5 s, cooldown 12.45 shown
  1.5 s later (14) ✓; E with it out took it back ✓. **Not shown:** a walking husk (AI) went past it to hit her and never clawed it; a
  skeleton's arrows (12 s) arced over it; it isn't solid, as in Overwatch.
- **Outburst** (I4): husks at 3 and 7 m 2 MC (25) each, the one at 10 m none ✓; standing tap ~1 m up, moving 9 m/s ahead, Jump held
  ~4.5 m up; no fall damage ✓. **Push not seen:** NoAI husks didn't move (Minecraft keeps NoAI mobs still); a moving mob's push untried.
- **Survivor** (S2): /damage to 10 MC, Outburst: 14.5 MC 1.7 s later ✓ (no 6 s wait). The control (damage with Outburst on cooldown)
  regenerated too: /damage has no attacker; inconclusive.
- **Captive Sun** (I5, V1): launch up ~1.4 m in 0.5 s, flight mode then ball-ready at 1.2 s ✓; the ball: 4 MC (50) on three husks up to
  6 m apart, 4.4 (55) with the direct hit ✓; a rifle shot on a marked husk: Sunstruck chained through all three (318 / 441 / 283 OW) ✓;
  flight with W held 0.399 b/t = 7.98 m/s ✓; down from 9 m at its end, unhurt ✓.
- **G** (S2): Minecraft mode and back ✓. **Switch** to Junkrat and back (the select payload): the pylon gone ✓.
- **Night fight** (N1, normal, midnight: 3 zombies, 2 skeletons, a spider, a creeper, a villager): 90 s, 4 deaths at the shared spawn,
  3 Outbursts, 2 pylons at her feet; respawned as Illari each time. The harness bot is crude (stops firing within 2 m).

**Found playing (fixed, each reproduced first):**
1. **Registering her voice crashed the game at start:** her wheel lines (`call_*`) and the shared moments are every hero's, registered by
   `OcSounds.init`. Dropped from `ILLARI_VOICE`.
2. **Captive Sun's ball crashed the game when drawn** (`ProjectileModelRenderer.prepare`: index 1 for a 1-joint model): the ball's mesh
   names a joint past its skin's. `SkinnedModel` now holds each vertex's joints to the skin's last (`illari_ult_ball.png` after).
3. **The press that fired the ball also fired the rifle** (ammo 14 -> 13 the same tick). The ball's press now counts as a shot. V1:
   ammo 14 after the ball ✓.

**Not fixed (open):**
- **Her hair's white swept lock renders as plain white** (`illari_select_kit.png`, `illari_pylon.png`): likely a material with no base
  texture or an emissive one; not read.
- **In flight she plays her fall pose**, not the data's `flight` clips (`illari_flight.png`); her 3P beam / charge additive layers aren't
  wired either (the animator plays `fire`, `reload`, `outburst`, `pylon_throw`, `ult_launch`, `ball_fire` by name).
- On the select screen she stands side-on with the rifle across her (`select` clip as exported); not compared with Overwatch.
- No reticle widget (the rifle's charge is synced in `secondaryReload` for one, unused). Effects are vanilla particles, not her data's.
- Sound events registered and played by name; not checked event by event, nor the long one-shots the map warns of beyond `sun.flight`.

## Fix round 1 (2026-11-14, the critic on 47bc22f): reproduced on 47bc22f, then replayed on the fix
Same scripts on both builds, fresh worlds `fx_old` / `fx2_old` (47bc22f) and `fx_new` / `fx2_new` (the fix); raw lines under `=== FX-OLD`,
`FX1b-OLD`, `FX2-OLD`, `FX-NEW`, `FX2-NEW`, `FXF` in [runs.txt](../testing/runs/illari/runs.txt); scripts `fx.txt`, `fx1.txt`, `fx2.py`.
1. **Her own pylon ate her shots** (FX1c: husk 1.5 m behind it, the line 0.76 m up through its box): old ammo 14 -> 13, husk 100 ->
   100; fixed 100 -> 94. A pylon stops a shot only when it's an enemy's (`Illari.enemyPylon`); allied heroes' projectiles pass it too
   (`OwProjectile.canHitEntity`).
2. **The ball's own damage counted toward Sunstruck** (FX2b: ball 55, then 40 OW of /damage): old went off (95.6 -> 79.6); fixed 92.4,
   no blast; then a rifle shot (+112.5) set it off (-> 70.6). Marks come after the ball's damage, and none of the ball's damage counts
   (`ballDamaging`: the splash after a pass-through landed on an already-marked target and counted, found replaying).
3. **The ball burst on the first enemy** (FX3b: a husk held in the air at her height, 10 m off): old 55 there; fixed 5 (passed through,
   W "passes through enemies and explodes on a shield, terrain or after 10 s").
4. **Pylon cooldown frozen while it's out** (FX4): old 0.0 with it out; fixed 5.95 at 1 s (7 from the throw, W "starts at activation").
   E with one out: old took it back (no pylon); fixed a new one replaced it once the 7 s ran (FX4b, cooldown 7 again), not within 3 s of a
   hit. 14 s from its destruction stays. Rapid Construction 12 / **5** (BZ 2026-06-16 -2 s; the data's 6 kept out).
5. **Outburst moving was 13.5 m/s set** (FX5: vz 0.549 / vy 0.304 b/t): D #812 is an impulse; added to her run (5.5) it gives W's 18.4
   tapped and 16.7 held exactly. Fixed: vz 0.747 / vy 0.26 b/t at 0.15 s (its decay above her air speed already applied).
6. **Outburst barely moved a mob** (FX6, an AI husk 2.5 m off): old 3.5 m; fixed 5.1 m (the push lifts it off the ground first, so the
   first tick's ground friction doesn't eat it).
7. **The pylon's hum outlived it** (FX7b: killed after building): old `illari.pylon.loop` still playing 1 s later; fixed gone. While it
   stands the hum restarts every 24 s.
8. A second Captive Sun on a marked target now refreshes the mark's time and keeps its count (code, not played). The HUD's pylon box
   fills against the cooldown it started with. **Select screen:** her `select` / `select_2` clips as the showcase's intro and hold: she
   faces the camera (`illari_fx_select.png`). **Flight pose:** the `flight` clip (animator state "flight", `illari_fx_flight_front.png`;
   `HeroManager.ILLARI_FLIGHT`).

**Still open:** her hair renders white (`illari_fx_select.png`): its material 8210 is shader 0x36 with no colour image in its .owmat
(only a soft gradient mask, 21C98), so the piece is drawn untextured; the colour must come from the shader, not a texture mapping:
left for a material pass. Vanilla particles for her effects, no reticle widget. Not replayed: the mark refresh, Rapid Construction.

**Not shown / not tried:**
- **The real mouse and keys** (owed before the merge): every press went through the harness's key mappings.
- The four perks in play; the 40 % minimum shot; the push on a moving mob; slopes, water, lava, ladders, the Nether (dimension change:
  code-read only), a boat, a second player (an enemy player shooting the pylon, the ult lines' split), the dragon, emotes and her intro,
  Summer Solstice's speed, Sunburn's ticks, Solar Flare's cone. A critic and an Overwatch side by side.
