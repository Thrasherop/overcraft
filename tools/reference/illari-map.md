# Illari data map, from Overwatch's game data (build 154088)

Read 2026-11-14 offline and read-only through TankLib/DataTool on the archived CASC copy `E:\Overwatch Archive\build-154088`
(2026-10-06), with Overwatch and Battle.net checked closed before every run, as [junkrat-map.md](junkrat-map.md). Confidence: **H** read
in the data, **M** one link inferred, **L** guess. Public numbers: [illari.md](illari.md). The node-by-node reading this map condenses:
`E:\Overwatch Archive\research\illari\work_kit\kit_illari.md`; its graphs `work_kit\c_<g>.txt` / `decomp_<g>.txt` / `edges_<g>.txt`,
values `work\varvalues_4659_out.txt`, `work\gamevalues_log.txt`, `work\lostats_out.txt`. Scripts: Junkrat's, retargeted
(`scripts\*_illari.py`, `scripts\kit\`, `scripts\sounds\`). **The build predates her last live change (BZ 2026-11-03: max charge 0.65 ->
0.575 s, pylon heal 40 -> 50)**: the data holds 0.65 and 40, so for those two the patch notes win. No Workshop recording yet.

## Ids
- Hero **031C.075**, entity **481D.003** (Gameplay3P), 1P **48CC.003**; graphs **3A65** Solar Rifle and the healing beam (weapons manager
  0015), **3B38** Outburst, **3DF4** Healing Pylon with the pylon's **59EA** (values) / **3DF5** / **3DF6** (heal shots) and **3D81** (a
  heal shot's impact on the ally), **3CAB** Captive Sun with the ball's **3CB1** and the mark's **3DFF** (run on each enemy hit), **51F3**
  perks, **4659** values, 3A9A hero tips (UI only), 01C7 shared (Survivor). Tag **1173** = Survivor. Not hers: 3B5E, 3C81, 449F, 44D0,
  475C (shared stat / bookkeeping graphs).
- Buttons: PRIMARY xD128396E (rifle; with Captive Sun up: the ball; while beaming: Solar Flare), SECONDARY xB42D90B7 (beam, buffer
  0.15), RELOAD x5D2A8B7F, ABILITY 1 xCBF0F815 (Outburst; held = higher), JUMP x4F81E1BD (also holds Outburst higher), ABILITY 2
  x2ADDA7B1 (pylon; again removes it), INTERACT x19C8495B (removes the pylon), ULTIMATE x2DFEDC52 (Captive Sun).
- Live perks (0064.177): **25B1** Rapid Construction, **25B2** Summer Solstice (minor); **25B4** Sunburn, **26F8** Solar Flare (major).
  25B3 (the removed Solar Power) still gates nodes in 3A65 #3219 / #3220.
- Models: **one model, 117.118**, for both views: 3P look 80A.119 (entity 481D), 1P look 80B.119 (entity 48CC). **The Solar Rifle has no
  model of its own** (child entity 53DA, var 1BA5, on 1P hardpoint 0078: an effect only); its meshes are the body's submeshes 7 / 8 / 15
  / 16 (drawn in both views). Pylon **134D8.00C** (look 17EA2; thrown entity 4B4A, deployed 4BCA, set 10FA), Captive Sun ball
  **10BAE.00C** (look 13AC1; entity 4AC0), pylon heal shot entity 4C2A and rifle hitscan entity 4828 (no models: effects).
- Clip maps: [illari.txt](../clipmaps/illari.txt) (3P, 188 clips, set 0F5E + the hero file's sets), [illari_1p.txt](../clipmaps/illari_1p.txt)
  (69, set 0F5D), [illari_pylon.txt](../clipmaps/illari_pylon.txt) (8, set 10FA), [illari_clips.txt](../clipmaps/illari_clips.txt) (emotes only),
  [illari_intro_clips.txt](../clipmaps/illari_intro_clips.txt) (Heroic). Sound maps: [illari.txt](../soundmaps/illari.txt) (voice, 37 events),
  [illari_sfx.txt](../soundmaps/illari_sfx.txt) (71 events). Emotes: `emotes/illari.json` (12).

## Against the public numbers
| | Data | Public | Used |
|---|---|---|---|
| Health, melee, speed | 250 (0B30), 40 (02D0), 5.5 (202B); mover 2.0 m, r 0.5, crouch 1.25 H | 250, 40, 5.5 | same |
| Rifle charge | **0.65 s** to full (3A65 #1751), after 0.25 s recovery (#2922, v30010) H | **0.575 s** (BZ 2026-11-03), 0.25 | BZ 0.575 (patch after the build) |
| Rifle damage | 25 + 50 x curve(charge): 0 -> 0, **0.4 -> 0**, 0.7 -> 0.5, 1 -> 1 (#3042, 11C5 / 0B31): **25 at 40 %, 75 at full** H/M | 25 -> 75, linear assumed | data's curve |
| Min shot | PRIMARY only from 40 % charge (#1827, v50965 = 0.4 x charge): 0.26 s, 0.23 s at 0.575 H | 0.25 s (W) | data (0.4 x) |
| Shot timing | firing needs 0.15 s since the last shot and 0.2 s since the beam (#544) H | none | data |
| Falloff, size, crit | 30-50 m to x0.3 (11C9 / 11CA, volley #555), 0.04 m (28A2), headshot x1.5 (1CEB) H/M | same | same |
| Ammo, reload | 14 (11C8), 1.5 s (11C7), ammo back at 0.72 x 1.5 = **1.08 s** (#2862) H | 14, 1.5 | data |
| Beam | **115 /s** (0B32), 20 m (11F5), radius 0.25 (293A) (#1763 / #1795) H | 115, 20, 0.25 | same |
| Beam energy | pool 100, cost 33.3 /s (11F6: 3 s), regen **35 /s** (1D21) after **0.6 s** (1DCD) (#2557-#2559) H | 3 s, 35 %/s, 0.6 s | same |
| Beam emptied | regen ~1.0 s later, beam locked until full (#2565 / #2573) H/M | +0.4 s (W) | data |
| Pylon throw | 25 m/s (293C), gravity 15 (M), size 0.15 (293B), thrown 0.1 s into a 0.6 s cast (3DF4 #89 / #115) H | 25 m/s, 0.08 + 0.816 s | data |
| Pylon | 75 + 50 shields (15A3 / 15A4), build **1.25 s** (1387) H | 75 + 50, 1.3 s | data |
| Pylon heal | **40** (1204) / self 20 (1206), one shot every 0.8 s, range 15 m (1D37), lowest health % first H/M | **50** / 25 (BZ 2026-11-03) | BZ 50 / 25 |
| Pylon shot | homing, **55 m/s** (3DF6 #216), life 8 s H | 25 m/s (W) | data (W's 25 is the throw) |
| Pylon cooldown | **7 s** (11FB) from the throw; **14 s** (11FC) if hit in the last 3 s (1D36), hacked or still building (3DF4 #345) H/M | 14 / 7 / 3 s (W) | data |
| Outburst | 25 (11F8), radius 8 (11FA), cooldown 8 (11F9) H | 25, 8, 8 | same |
| Outburst push | knockback pair **m_E33EFEE1 18 / m_583C208B 5** (1D35 / 1D34), x1 to 5.6 m, x0.3 at 8 m (3B38) H | H 18 / V 5 (W) | data |
| Outburst launch | tap standing **6 up** (0EC2), moving or held **13.5** (0EC3), direction (x, 0.6, z) tap / (x, 2, z) held (key or JUMP) H | 6 / 13.5 tooltip; W 18.4 / 16.7 | data |
| Outburst cast | burst 0.2 s in, 0.8 s in all (anim 0677, #635) H | 0.208 + 0.88 | data |
| Survivor | 01C7 #2564 on tag 1173: **Outburst only** (message 26FD) starts the regen at once, kept at least 0.25 s (23DA) H/M | same; Captive Sun unknown | data: not Captive Sun |
| Ult cost | 2975 (0AE6; alt 2400) H/M | 2975 (W) | 2975 |
| Perk XP | 2355 / 2355 + 3535 = 5890 (12D7 / 12D8) H | 2355 / 5890 (W) | same |
| Captive Sun launch | 1.2 s (anim 01B8, #822), 12 m/s up, the beam energy and ammo refilled 0.6 s in (#1076 -> 007D) H | 1.2 s, 12 m/s | data |
| Flight | speed **x1.45** (1D3D), knockback taken -50 % (1DF9); the ball fires on PRIMARY or by itself 4 s after the launch (1388) H/M | +45 %, 50 %, 5.2 s | data (the tooltip's 1A7C 0.6 is stale) |
| Ball | 30 m/s (293E), 0.45 m (293D), 10 s, direct **5** (1D38) + splash **50** (1D39), no falloff (3CB1) H | same | same |
| Ball splash radius | **8 m** (1216) H | **10 m** (BZ 2024-12-10) | disagree (L): BZ 10 unless a mode scalar is found |
| Mark | **7 s** (1218), slow 40 % for 1 s then back to 0 over 6 s (3DFF) H/M | **6 s** (BZ 2023-08-24), 40 % -> 0 | disagree (L): BZ 6 |
| Sunstruck | threshold **90** (1A34), explodes **0.8 s** later, **160 inside 1 m -> 120 at 5 m** (1212 / 1213, 1D3A / 1D3B; 3DFF #451 / #515 / #460) H | 90, 0.8 s, 160-120, 1-5 m | same |
| Rapid Construction | build x0.25, cooldowns 12 / **6** s H | 300 % faster, 12 / 5 s (W) | data 6 |
| Summer Solstice | flight +3 s, attack speed +0.2 in flight (and v27756 +0.2, reader unknown) H/L | +3 s, +20 % / +20 % | same |
| Sunburn | 50 over ~3 s (2.984) H | 50 over 3 s | same |
| Solar Flare | 100 heal, 20 m, 40 deg cone, 10 s, 0.4 s cast, only while beaming, no energy (51F3 #453 / #482) H/M | 100, 20 m, 40 deg, 10 s | same |

## Clips (the Java side's states)
| | 3P (illari.txt) | 1P (illari_1p.txt) |
|---|---|---|
| idle | `idle` D01F (alias 85 df18 default[4]) | `idle` CF5E (alias 9 df18 cat9; + `add_idle` DB14) |
| run | `run` CFC2 + 8-way (`run_back` D4D6, `strafe_left` D4D8, ...) | `run` D04E |
| jump / fall | `jump` D246, `fall` D264 (still); `jump_forward` DAB0, `fall_forward` DAB1 | `jump` D4E8, `fall` D4E9 |
| crouch | `crouch` DC25, `crouch_walk` DC29 | none of its own |
| fire | `add_fire` D26B (alias 4A8) | `add_fire` D006 |
| charge | `add_charge` DEF7, `add_charge_2` DEF9 (alias 16E) | `add_charge_hold` D478 |
| beam | `add_beam_hold` DA04 (alias 16F), `add_beam_end` DEFB (40A) | `add_beam_hold` D53B, `add_beam_end` DDD2 |
| reload | `reload` D23F (1.67 s) | `reload` DD5F (1.47 s) |
| pylon throw | `pylon_throw` D300 (+ `_down` / `_up`) | `pylon_throw` D1B3 |
| outburst | `outburst` D0E4 (0.8 s) | `outburst` D095 |
| ult cast | `ult_launch` D48B (1.0 s) | `ult_launch` D592 (1.17 s) |
| flight | `flight` D482 + 2D directions (`flight_f` ... ; ball ready), `flight_after` DE97 + directions (after the ball) | `flight` D595 (3.6 s) |
| ball fire | `ball_fire` DEEB (+ `_down` / `_up`) | `ball_fire` D597 |
| melee | `melee` D089 | `melee` D306 |
| Solar Flare | `add_solar_flare` 174B4 | `solar_flare` 174AB |
| pylon | `idle` D5B2, `deploy` D55B, `build` D5AF (1.3 s), `add_pylon_fire` D5B4, `add_hit_down` DD9C, `destroyed` D5B3, `hacked` DDA3 | |

## In the local pack (`local-assets/pack`, never git)
- Models (`assets/overcraft/hero_models/`): `illari.glb` (3P, 15 submeshes with the rifle, 188 clips), `illari_1p.glb` (6 submeshes, 69
  clips), `illari_pylon.glb` (8 clips), `projectile_illari_sun.glb` (the ball, static), `illari_clips.glb` (12 emotes, 43 clips),
  `illari_intro_clips.glb` (`potg_intro` ... `potg_intro_4b`), `prop_134d8.glb` (Have a Seat's pylon, clips DD36 / DD37 / DF1D);
  glows `illari_glow.json`, `illari_1p_glow.json`, `illari_pylon_glow.json`.
- UI (`textures/gui/heroes/illari/`): `portrait.png`, `portrait_close.png`, `head.png`, `bust.png`, `silhouette.png`;
  `abilities/solar_rifle.png`, `secondary_fire.png`, `healing_pylon.png`, `outburst.png`, `captive_sun.png`; `perks/rapid_construction.png`,
  `summer_solstice.png`, `sunburn.png`, `solar_flare.png` (no card / fullbody in her icon set).
- Sounds (event names the code plays): rifle `illari.rifle.fire` / `_3p` (full charge), `rifle.fire_uncharged` / `_3p` (one Wwise
  event in the data, split by a game parameter: M), `rifle.charge`, `rifle.charged`, `rifle.hit_enemy` / `_3p`, `rifle.hit_world`,
  `rifle.whiz` (L), `illari.reload` / `_3p`; beam `illari.beam.start` / `_3p`, `beam.loop` / `_3p`, `beam.stop` / `_3p`, `beam.refill`,
  `beam.full`, `beam.hit` / `_3p`, `beam.hit_world` (L), `beam.heal` / `_3p`, `beam.healing` (heard by the healed ally); pylon
  `illari.pylon.throw` / `_3p` (L), `pylon.flight`, `pylon.land`, `pylon.build`, `pylon.build_fast` (Rapid Construction), `pylon.loop`,
  `pylon.heal`, `pylon.heal_hit` (L), `pylon.targeted`, `pylon.destroyed` / `_3p`, `pylon.removed` / `_3p`; `illari.outburst` / `_3p`,
  `outburst.hit_enemy` / `_3p`, `outburst.sunburn`, `outburst.burn` / `_3p`; Captive Sun `illari.sun.cast` / `_3p`, `sun.flight` / `_3p`,
  `sun.armed` / `_3p`, `sun.fire` / `_3p`, `sun.ball`, `sun.impact`, `sun.mark` / `_3p`, `sun.marked`, `sun.marked_own`, `sun.trigger` /
  `_3p`, `sun.trigger_owner`, `sun.explode` / `_3p`; `illari.flare` / `_3p`, `flare.hit`; `illari.melee` / `_3p`. No sound at the
  beam's overheat (only the voice line). Voice: `illari.voice.ultimate` (her own, 1848), `illari.ult.friendly` (1356), `illari.ult.enemy`
  (1355), `voice.pylon`, `pylon_destroyed`, `pylon_moved`, `outburst`, `outburst_hit`, `beam_hurt_ally`, `beam_empty`, `ping_pylon` and
  the shared moments; no line for a Sunstruck kill, a reload or Solar Flare in her graphs.

## Open
- Ball splash 8 m and mark 7 s against BZ's 10 m and 6 s: a mode scalar (4659 #297 / #300 config vars) or a later value not found.
- The rifle's geoset bit for the emotes' masks (her masks read 0 / 1 / 3 / 7 as Juno's; the model's first-LOD table lists two submeshes
  only): `emote_data.py` has no `BODY_WEAPON_GEOSETS` entry for her yet, so `pieceGeosets` is empty.
- The beam's heal mode field, the press-to-beam timer (0.3 s), the flight mover's fields, Summer Solstice's v27756 reader; which pylon
  voice stimulus (13E1 / 13E2 / 1465) is which; 3A94 / 3BFA / 261E not read.
- Fixed-length sounds (`sun.flight`, `sun.armed`, `rifle.charge`, `beam.refill`) are long one-shots the code must stop; the per-listener
  copies (marked enemy, healed ally) need a second client to check.
- No effect texture pass for her abilities (Junkrat had none either).
