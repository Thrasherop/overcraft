# Tracer data map, from Overwatch's game data (build 154088)

Read 2026-11-11 offline and read-only through TankLib/DataTool on the archived CASC copy `E:\Overwatch Archive\build-154088`
(2026-10-06), with Overwatch and Battle.net checked closed before every run, as [pharah-map.md](pharah-map.md). Confidence: **H** read
in the data, **M** one link inferred, **L** guess. Public numbers: [tracer.md](tracer.md). The node-by-node readings this map
condenses: `E:\Overwatch Archive\research\tracer\work_kit\kit_tracer_move.md` (Blink, Recall, mover, Flanker, the movement perks) and
`kit_tracer_weapon.md` (Pulse Pistols, Pulse Bomb, Kinetic Reload, Quantum Entanglement); their graphs `work_kit\c_<g>.txt` /
`decomp_<g>.txt` / `edges_<g>.txt`, values `work\gamevalues_out.txt`, `work\varvalues_32FC.json`. Scripts: Pharah's retargeted
(`scripts\*_tracer.py`, `export_models.sh`). Measured against the owner's Workshop recording: [WS 2026-11-11](../workshop/analysis/tracer_ws.py),
its output `techniques/testing/runs/tracer/ws_2026-11-11.txt`.

## Ids
- Hero 0003.075, entity 0001.003; graphs **0016** Pulse Pistols (weapon manager 0015), **0024** Blink, **001A** Recall, **0034** Pulse
  Bomb with its entity **0045** (graph 0C1B), **51EE** perks, **32FC** values, 01C7 shared (sub-role, regen). Tag **1187** = Flanker.
- Buttons: PRIMARY xD128396E (pistols), ABILITY 1 xCBF0F815 **and** SECONDARY xB42D90B7 (Blink, one press path), ABILITY 2 x2ADDA7B1
  (Recall, 0.15 s buffer), ULTIMATE x2DFEDC52 (Pulse Bomb). Live perks (005E.177): 2AC7 Temporal Regen, 26B6 Kinetic Reload (minor),
  259B Blink Packs, 259D Quantum Entanglement (major). Chronal Dash (28B8) is in the graphs but not the list.
- Models: 3P D748, 1P D868, pistol DA61 (entity 0017 twice: hardpoints 000A = Weapon_R slot 1B9B, 000B = Weapon_L slot 1B9C), bomb
  160BB. Clip maps `tools/clipmaps/tracer*.txt`; sounds `tools/soundmaps/tracer*.txt`.

## Against the public numbers and the Workshop
| | Data | Public | WS 2026-11-11 | Used |
|---|---|---|---|---|
| Health | 175 (0001) H | 175 | max 175 | 175 |
| Run | mover 6.0 fwd / strafe, 5.4 back H | 6.0 (W) | 5.999 / 5.999 / 5.400, diag back 5.600 | 6.0 / 5.4 |
| Acceleration | default mover | | 3.36, 5.28, 6.00 over three 0.016 s ticks | default mover at 6.0 |
| Jump, gravity, air cap | 5.72 / 17.5 / 5.5 (default) H | 5.72 | 5.72; 0.28 m/s a tick; 6.0 -> 5.92 in the air | default |
| Pistols | 5.75 x 2 bullets, 20 / s, 40 rounds, 10-20 m to x0.3, x2 head, 0.02 m H | same (W) | (not recorded) | data |
| Spread | 0 -> 3.5 deg over 3 shots; held 0.2 s, then 4 / s M | 3.5, 0.224 s | | data |
| Re-fire lock | 0.2 s after letting go (#12) H | | | data |
| Reload | 1.0 s, ammo at 0.7 s H/M | 1 s, 0.686 | | data |
| Blink charges | 3 (2195) H | 3 | 3 | 3 |
| Blink recharge | 3 s, one at a time, not reset by blinking (#54 / #143) H | 3 s, order unknown | back at +3.00 s each in sequence (10.10, 13.10, 16.11, 19.12, 22.13) | data = WS |
| Blink distance | 7.5 (1AED) H | 7.5 | 7.49-7.51 m, all in one tick | 7.5, instant |
| Blink lockout | 0.1 s (#153) H | 0.096 | "in use" 0.10 s | 0.1 |
| Blink direction | move input, forward with none (#165) M | moving direction | input direction; a held key the same tick | data |
| Blink vertical | not in the graph | horizontal (W) | level in the air (vy zeroed, then gravity); on the ground +1.00 up a step, +1.39 up a ledge, -1.40 down a slope | WS: level; step 1.5 (m_497165F8, L) |
| Blink speed after | no impulse (H by search) | kept | horizontal kept, vertical 0 | WS |
| Recall history | 3 s (#590) H | 3 s | endpoint within 0.42 m of 3.0 s before | 3 s |
| Recall duration | 0.9 s (1B7C) H | 0.9 s phased | 0.896 s, phased 56 / 56 | 0.9 |
| Recall path | engine (STU_41B3FC2B) | | still 0.43 s, then the old place at once (0.44 s) with its health, held there to 0.9 s while gravity adds to its old speed (-1.96 -> -9.48) | WS |
| Recall health | measured before / after only | best of the 3 s | 165 -> 175 at 0.44 s | best, at 0.44 s |
| Recall ammo | full at 0.72 s (0.8 x 0.9) H nodes | full | | 0.72 s |
| Recall cooldown | 12 s (02E1) from its end H | 12 s | | 12 s from end |
| Recall ult charge | 10 % of health given back M | | | 10 % |
| Pulse Bomb | 1375 (0B00) M; cast 0.15 + 0.2 H; 15 m/s, 30 m/s² M, 0.22 m; stick 5; fuse 1 s from the stick; 350 to 2 m -> 70 at 4 m H; self x0.5; no push | 1375, 0.144 + 0.192, 350 / 70, 1 s | press -> throw 0.15 s, ult flag 0.19 s | data |
| Flanker | +50 on every pack (23D3) H | +50 | | +50 on a potion of Healing (ours) |
| Damage role passive | none (global healing cut) H | removed 2026-02-10 | | none |
| Temporal Regen | regen delay 6 -> 3 s H | BZ 50 %, W 100 % | | 3 s (data = W) |
| Kinetic Reload | 12 rounds a quick-melee hit H | 12 | | 12 |
| Blink Packs | a pack -> +1 charge H | +1 | | on a potion of Healing (ours) |
| Quantum Entanglement | 50 overhealth, 6 s, then 25 (M); magazine to 60 H | 50, 20 ammo, 6 s | | data |

## Open
- The blink node's own fields (0.1, 0.7, 1.5, four 1.0): its ledge and step rule is engine-side; the 1.5 step is a reading (L).
- Whether Blink follows the view's pitch (WS: never seen to; W: horizontal). Standing still with no key: forward (D), not isolated in WS.
- Recall's visible rewind (its camera path) is the client's effect; the Workshop sees only the server's place (held, then the jump).
- Recall's cleanse (W "lesser cleanse"; no node: ours, Combat.cleanse).
- Quantum Entanglement's decay curves; what the bomb sticks to besides enemy heroes (ours: any enemy creature, then blocks).
