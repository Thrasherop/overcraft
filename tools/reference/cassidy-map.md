# Cassidy data map, from Overwatch's game data (build 154088)

Read 2026-11-12 offline and read-only through TankLib/DataTool on the archived CASC copy `E:\Overwatch Archive\build-154088`
(2026-10-06), with Overwatch and Battle.net checked closed before every run, as [tracer-map.md](tracer-map.md). Confidence: **H** read
in the data, **M** one link inferred, **L** guess. Public numbers: [cassidy.md](cassidy.md). The node-by-node reading this map condenses:
`E:\Overwatch Archive\research\cassidy\work_kit\kit_cassidy.md`; its graphs `work_kit\c_<g>.txt` / `decomp_<g>.txt` / `edges_<g>.txt`,
values `work\gamevalues_out.txt`, `work\varvalues.json`, `work\lostats_out.txt`. Scripts: Tracer's, retargeted (`scripts\*_cassidy.py`,
`scripts\kit\`). The build is dated 2026-10-06 and already has that day's hitscan sizes; the last live Cassidy number change (BZ
2026-08-11) is older, so no later patch overrides the data. No Workshop recording yet.

## Ids
- Hero 0042.075, entity 02A3.003; graphs **01D2** Peacekeeper + Fan the Hammer + Silver Bullet (weapon manager 0015), **01F0** Combat
  Roll, **01E9** Flashbang, **022F** Deadeye with per-target **01EE** (status 0056.053), **51E5** perks, **32C4** values, 01C7 shared
  (Sharpshooter). Hinder status 0079.053 (graph 0228). Silver Bullet bleed 5FDA. Tag **1188** = Sharpshooter.
- Buttons: PRIMARY xD128396E (Peacekeeper, buffer 0.1), SECONDARY xB42D90B7 (Fan the Hammer; Silver Bullet arm), ABILITY 1 xCBF0F815
  (Combat Roll), ABILITY 2 x2ADDA7B1 (Flashbang), ULTIMATE x2DFEDC52 (Deadeye), RELOAD x5D2A8B7F; abilities buffer 0.15.
- The grenade is **Flashbang** (loadout 0060, graph 01E9). Magnetic Grenade's loadout 0335 is still in the data, named, but not in the
  hero's loadouts: only the hinder status's damage label and Stadium's Hot Potato (2836.134) use it.
- Live perks (006C.177): 2527 Bang Bang, 2ADA Giddy Up (minor); 286A Rollin' Round-Up, 2723 Silver Bullet (major). Even the Odds (25E1)
  is still named by the hero and its Deadeye node remains, but its file is gone. Gun Slingin' and Past Noon nodes are orphans.
- Models: 3P 093F.00C, 1P 0FD9.00C, revolver 0915.00C (entity 02A6, hp 000A), Flashbang 11D8.00C (entity 037D).

## Against the public numbers
| | Data | Public | Used |
|---|---|---|---|
| Health | 250 (02B7) H | 250 | 250 |
| Run, jump | default mover: 5.5 fwd / strafe, 4.95 back H | 5.5, back 90 % | 5.5 / 4.95, default jump |
| Quick melee | 40 (02D0) H | 40 | 40 |
| Peacekeeper | 70 (02BF), 25-35 m to x0.3, crit x2, 0.04 m, no spread H; 65 is Stadium | same (W) | data |
| Peacekeeper rate | 0.5 s from the shot (v173 chase) H | 0.5 s | 0.5 s |
| Ammo, reload | 6; anim 1.5 s, ammo at 0.8 s (1.5 x 0.5333) H; fire blocked to 1.5 s M | 6, 1.5 s / 0.8 | data |
| Fan the Hammer | 50, 20-30 m to x0.3, no crit (override 1) M, 0.04 m H | 50, 15 at 30 m, no crit | data |
| FtH rhythm | 0.12 s between shots until empty; spread 0 then 6 deg (1 shot to max) H/M; 0.3 s primary lock after M | 0.12 s, 6 deg, first exact | data |
| Recoil | Peacekeeper 4, FtH 2.5 (m_C5143E2E) M | 4 / 2.5 units | data |
| Combat Roll move | 15 m/s for 0.4 s = 6.0 m; **x1.3 = 19.5 m/s in the air** (7.8 m); input direction, else facing; keeps vertical speed; exits at 5.5 H/M | 6.22 m in 0.416 s, 15 m/s | data |
| Combat Roll DR | -50 % for the whole 0.4 s H | -50 % | data |
| Combat Roll reload | ammo full at 0.2 s (message 007D) H/M | reloads | 0.2 s |
| Combat Roll cooldown | 5 s from the roll's end H | 5 s | 5 s from end |
| Flashbang throw | cast 0.1 s, then 0.35 s; 30 m/s, gravity 20 (M), 0.2 m, life 5 s; range ~7 m (6.5 + 0.5 x an unread term) H/L | 0.128 + 0.608, 30 m/s, 7 m | data, 7 m |
| Flashbang blast | 75, 3 m, no falloff (one ring at 1.0) M, no knockback H | 75, 3 m | data |
| Hinder | 1.2 s x (1 - CC resist) H; slow 0.5 M; movement abilities off (v5995) M; crouch off M | 1.2 s, -50 %, no crouch | data |
| Flashbang cooldown | 12 s from the cast's end H | 12 s | 12 s from end |
| Deadeye cast | 0.2 s, then 7 s; reload + Silver Bullet reset at cast; charge zeroed at cast H | 7 s | data |
| Deadeye lock | per target 150 /s for its own first 2 s, then 300 /s; paused out of sight; cone 105, 200 m, hp 0001 H | 150 / 300, 105 deg, 200 m | data |
| Deadeye self | slow 70 % -> 35 %, DR 40 % -> 0, in 10 steps of 0.15 s after 2 s H/M | 1.4 s fade | data |
| Deadeye fire | 0.1 s, then one target every 0.1 s (+0.066), 1 ammo each (floor 0), 0.3 s end H; cancel 0.1 s (+0.5 anim) H; timeout: no shot H | 0.112 s, 0.288 + 0.4 | data |
| Deadeye cost | 1800 (0AEF) M | 1800 | 1800 |
| Sharpshooter | hero crit -> Combat Roll cooldown - damage dealt x 0.0075 s H | 0.75 % of damage | data |
| Bang Bang | second Flashbang with the first: 25 m/s, range +3.25 (~10 m) H; damage x0.6 M | 10 m, 0.1 s later, -40 % | data |
| Giddy Up | +60 % from the roll's end, falling linearly in 10 steps over 1.5 s H/M | 60 %, 1.5 s | data |
| Rollin' Round-Up | 15 x bullets reloaded by the roll H | 15 each | 15 |
| Silver Bullet | 70, 25-35 m, crit x2, bleed 50 over 1.5 s, 8 s, reset by Combat Roll and Deadeye H | same | data |
| Perk XP | 1560 / 1560 + 2340 = 3900 M | 1560 / 3900 | 1560 / 3900 |

## Open
- Flashbang's range term `STU_FDBB0B67` (x 0.5 on top of 6.5 m) and which `m_D2D11CE9` flag explodes it at the range end.
- Whether Fan the Hammer stops when the button is let go (data: started on press, runs the cylinder out, M).
- Deadeye: which of 01D5 / 0487 / 183B.078 is "It's high noon" (0.5 s after the cast); ult charge earned while channelling.
- Bang Bang's x0.6 filter scope (only the Flashbangs?); the second blast's timing in play (launched together, slower).
- The Peacekeeper `m_FEC435C6` 400 / FtH 100 (max ranges, L); 0541 75 'Rifle Secondary Damage' on the tooltips has no reader.
