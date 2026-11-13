# Junkrat data map, from Overwatch's game data (build 154088)

Read 2026-11-13 offline and read-only through TankLib/DataTool on the archived CASC copy `E:\Overwatch Archive\build-154088`
(2026-10-06), with Overwatch and Battle.net checked closed before every run, as [cassidy-map.md](cassidy-map.md). Confidence: **H** read
in the data, **M** one link inferred, **L** guess. Public numbers: [junkrat.md](junkrat.md). The node-by-node reading this map condenses:
`E:\Overwatch Archive\research\junkrat\work_kit\kit_junkrat.md`; its graphs `work_kit\c_<g>.txt` / `decomp_<g>.txt` / `edges_<g>.txt`,
values `work\gamevalues_log.txt`, `work\varvalues.json`, `work\lostats_out.txt`. Scripts: Cassidy's, retargeted (`scripts\*_junkrat.py`,
`scripts\kit\`, `scripts\sounds\`). The build is dated 2026-10-06; the last live Junkrat number change (BZ 2026-09-08: mine 7 s, Nitro
100 %) is older and is in the data, so no later patch overrides it. No Workshop recording yet.

## Ids
- Hero 0065.075 (037C.075, also named "Junkrat", is a bot / PvE copy: not used), entity 0380.003, 1P 0381.003; graphs **022B** Frag
  Launcher (weapon manager 0015), **022A** Total Mayhem, **096A** Concussion Mine with the mine's **096B**, **09BA** Steel Trap with the
  trap's **09BB** (root status 01F5.053 -> 09BC), **0232** RIP-Tire with the tire's **0234** / **0233**, **51E8** perks, **336B** values,
  01C7 shared (Specialist). Tag **116F** = Specialist.
- Buttons: PRIMARY xD128396E (Frag Launcher; on the tire: detonate), SECONDARY xB42D90B7 (detonate the mine, buffer 0.3; on the tire:
  detonate), ABILITY 1 xCBF0F815 (Concussion Mine; Nitro Boost on the tire), ABILITY 2 x2ADDA7B1 (Steel Trap), ULTIMATE x2DFEDC52
  (RIP-Tire; on the tire: detonate), RELOAD x5D2A8B7F; abilities buffer 0.15.
- Live perks (0055.177): 252C Nitro Boost, 26F3 Bomb Voyage (minor); 25C0 Frag Cannon, 272B Mine Recycling (major). The data has them
  in the tiers of BZ 2026-04-14.
- Models (the classic skin's, from its .owentity files; base in brackets): 3P **1170A** (1019), 1P **11709** (1196), Frag Launcher
  **1022D** (1283; entity 0452, hp 000A, var 193F), the mine in his left hand **10095** (1445; entity 04E8, hp 000B, var 1DB2), frag
  **118EA** (2764; entity 0385, the death bombs 0515 too), thrown mine **10093** (2959; entity 0F9B), trap in flight **100DC** (28CD;
  entity 11A9), deployed trap **29B2** (entity 103A; jaws 103B = 100DC), tire **118D8** (101B; pet entity 039C).
- Clip maps: [junkrat.txt](../clipmaps/junkrat.txt) (3P, 139 clips), [junkrat_1p.txt](../clipmaps/junkrat_1p.txt) (67),
  [junkrat_weapon.txt](../clipmaps/junkrat_weapon.txt) (launcher, 19 + the Heroic emote's clip), [junkrat_mine.txt](../clipmaps/junkrat_mine.txt)
  (9), [junkrat_tire.txt](../clipmaps/junkrat_tire.txt) (13), [junkrat_trap.txt](../clipmaps/junkrat_trap.txt) (1),
  [junkrat_intro_clips.txt](../clipmaps/junkrat_intro_clips.txt) (Heroic). Sound maps: [junkrat.txt](../soundmaps/junkrat.txt) (voice),
  [junkrat_sfx.txt](../soundmaps/junkrat_sfx.txt) (61 events). Emotes: `emotes/junkrat.json` (17).

## Against the public numbers
| | Data | Public | Used |
|---|---|---|---|
| Health | 250 (0178) H | 250 | 250 |
| Run, jump | default mover: 5.5 fwd / strafe, 4.95 back, crouch 3 H | 5.5 | 5.5 / 4.95, default jump |
| Quick melee | 40 (02D0) H | 40 | 40 |
| Frag damage | direct 45 (0343) + splash 80 (0344), no crit (override 1) H/M | 45 + 80 = 125 | data |
| Frag splash | 80 inside 0.5 m (2 x 1D3F 0.25), falling to 10 (1D2F 0.125) at 2 m (0347); linear M | 80 -> 10, 0.5-2 m | data |
| Frag projectile | 30 m/s (0345), gravity 17 (M), lifetime 7 s, hero 0.1 m (28EE), world 0.2 + 0.12 = 0.32 m H | 30 m/s, 0.32 m, 0.16 m after 2nd bounce | data |
| Frag bounces | size x [1, 0.5] per bounce M, restitution 0.5 M; the count / 2nd-bounce rule not found L | 2 ground bounces, enemy contact, 4.5 s | W's rule, data's numbers |
| Frag knockback | (5, 8) x a per-target scalar (1D2C / 1D2D), rings 0.5 m 1 / 2 m 0.75 H/L | 9.4 m/s; self 2.5 / 4 | data, self x0.5 (L) |
| Self damage | "Deals no damage to self with explosives" (loadout text) H | none, knockback yes | none |
| Ammo, rate | 5 (0346); 0.667 s between shots (022B #102) H | 5, 1.5 /s | 5, 0.667 s |
| Reload | anim 1.5 s (0348), ammo at 1.5 x 0.578 = 0.867 s H | 1.5 s / 0.848 | 1.5 / 0.867 |
| Frag Cannon | speed x1.25 (37.5 m/s) H | +25 % | data |
| Mine throw | 0.144 s cast, 25 m/s, gravity 20, 0.2 m, sticks (M), anim 0.5 s H | 25 m/s, 0.2 m, 0.128 s | data |
| Mine charges | 2, 7 s each (034C) H | 2, 7 s | 2, 7 s |
| Mine detonate | SECONDARY, from 0.1 s after the throw; one mine out (0511 removes the old) H | 2nd press, 0.112 s | data |
| Mine blast | 120 (034F) inside 1 m, linear to 55 (034A) at 3 m (0350), enemies; knockback 15 / 15 (1D51 / 1D50) H | 120 -> 55, 1-3 m, 21.2 m/s | data |
| Mine self launch | within 3.5 m: 15 horizontal (187A) away + 15 up (1D47), no damage H | 15 / 15, no damage | data |
| Mine health | 25 (19A8), breaks without exploding H/M | 25 | 25 |
| Mine Recycling | a mine kill -> +1 charge H | same | data |
| Bomb Voyage | after the mine launch, attack speed +0.35 for 3 s H | +35 %, 3 s | data |
| Trap throw | 15 m/s (0356), gravity 20, 0.3 m, life 5 s; anim 0.35 s H | 17 m/s, 0.3 m | 15 m/s |
| Trap | arm 0.75 s, trigger 1 m (1D61), 100 (0352), root 2.5 s x (1 - CC resist) (0353), health 100 (0355) H | 0.75 s, 1 m, 100, 2.5 s, 100 | data |
| Trap cooldown | 10 s (0354), from 0.5 s after the throw; one trap (a new one destroys the old) H/M | 10 s | data |
| RIP-Tire cost | 2100 (0AEA) M | 2100 | 2100 |
| RIP-Tire cast | anim 2.0 s (v670 1.5 + 0.5), spawned 0.05 m ahead; armed 1.4 s + 0.2 s, then detonate H | 1.58 + 0.08 s | data |
| RIP-Tire tire | 10 s (035B), health 100 (03D9), forward 12 / back 6 / strafe 3 m/s, 0.8 m tall (M fields) H/M | 12 m/s, 100, 10 s | data |
| RIP-Tire blast | 600 inside 2 m (0349 / 035A), linear to 50 (0358) at 10 m (0359); knockback 6 / 6 (1D6E / 1D6F); killed: no blast H | 600 -> 50, 2-10 m | data |
| RIP-Tire Junkrat | five movement inputs zeroed while the tire lives (rooted) H/M | rooted | rooted |
| Nitro Boost | ABILITY 1 once, 1.6 s after the spawn; +100 % speed falling to +25 % over 2 s, 2.5 s; tire damage x0.5 H/M | +100 %, -50 % damage | data |
| Total Mayhem | 6 bombs of 50 (187C) in 2 m (1996), 1.5 m/s, gravity 9.8, spread 90 deg, burst at 0.8-1.3 s H | 6 x 50, 2 m, 0.7 s | data |
| Specialist | elimination -> reload speed +0.5 for 3 s (23D5 / 23D6), used up by one reload (M) H/M | +50 %, 3 s | data |
| Perk XP | 1675 / 1675 + 2510 = 4185 (129E / 129F) H | 1675 / 4185 | 1675 / 4185 |

## Sounds (junkrat_sfx.txt, the event names the code plays)
Frag: `junkrat.frag.fire` / `_3p` (L: no node names a launch sound; 2D0E / 1BFE picked), `frag.flight` / `_3p` (loop), `frag.bounce`,
`frag.explode` / `_3p`, `frag.hit_enemy` / `_3p`, `frag.explode_world`, `frag.fizz`, `junkrat.reload` / `_3p`, `frag.cannon`. Mine:
`junkrat.mine.throw` / `_3p`, `mine.bounce`, `mine.stick`, `mine.armed` / `_3p` (loop), `mine.detonate` / `_3p` (the button),
`mine.explode` / `_3p`, `mine.jump`. Trap: `junkrat.trap.throw` / `_3p`, `trap.land`, `trap.arm`, `trap.trigger`, `trap.caught` / `_3p`,
`trap.release`. Tire: `junkrat.tire.cast` / `_3p`, `tire.loop` / `_3p`, `tire.jump`, `tire.land`, `tire.explode` / `_3p`, `tire.destroyed`
(no tire climb sound in the data). `junkrat.mayhem.drop`, `junkrat.mayhem.bomb`, `junkrat.nitro.boost`, `junkrat.melee` / `_3p`.
Voice (junkrat.txt): `junkrat.voice.ultimate` (own, "Fire in the hole!"), `junkrat.ult.friendly` (allies hear "Ladies and gentlemen,
start your engines!"), `junkrat.ult.enemy` (enemies hear "Fire in the hole!"), plus `voice.mine_throw`, `mine_jump`, `mine_kill`, `trap`,
`tire_kill` and the shared moments.

## Open
- The frag's bounce count and its second-bounce / 4.5 s detonation rule: the bounce struct's fields (STU_3FE1A93B, STU_20C861FB) are
  read but not named; W's rule is kept.
- The per-target knockback scalars (frag `v10638`, mine `v10614`): which makes his own frag knockback half (W's 2.5 / 4).
- Whether Specialist also moves the reload's 0.867 s ammo point (the wait has no speed scalar).
- The tire's wall-climb and jump fields by name (mover fields, M); the frag's fire sound (L).
